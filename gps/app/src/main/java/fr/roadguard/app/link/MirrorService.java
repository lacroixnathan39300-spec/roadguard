package fr.roadguard.app.link;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Surface;

import java.nio.ByteBuffer;

import fr.roadguard.link.LinkProtocol;

/**
 * Mode miroir : l'écran du téléphone (Waze ou toute autre appli) est filmé avec l'accord de
 * l'utilisateur, compressé en H.264 par la puce du téléphone et envoyé à l'autoradio via ROADGUARD Link.
 * Se réadapte quand le téléphone passe en paysage.
 */
public final class MirrorService extends Service {
    public static final String EXTRA_RESULT_CODE = "result_code";
    public static final String EXTRA_DATA = "data";
    public static final String ACTION_STOP = "fr.roadguard.app.link.MIRROR_STOP";
    public static final String EXTRA_VIDEO = "video";
    public static final String EXTRA_AUDIO = "audio";
    private boolean wantVideo = true;
    private boolean wantAudio;
    private volatile boolean audioRunning;
    private Thread audioThread;
    private static final String TAG = "RoadguardMirror";
    private static final String CHANNEL = "roadguard_mirror";
    /** Côté long de la vidéo : celui de l'écran du poste (1024 × 600), moins à coder et à décoder. */
    private static final int LONG_SIDE = 1024;

    public static volatile MirrorService instance;

    private MediaProjection projection;
    private VirtualDisplay display;
    private MediaCodec encoder;
    private Surface inputSurface;
    private HandlerThread thread;
    private Handler handler;
    private int width, height;
    private byte[] lastConfig;
    private DisplayManager.DisplayListener rotationListener;

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || ACTION_STOP.equals(intent.getAction())) { stopSelf(); return START_NOT_STICKY; }
        startForegroundNow();
        if (projection != null) return START_NOT_STICKY;
        int code = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED);
        Intent data = intent.getParcelableExtra(EXTRA_DATA);
        if (code != Activity.RESULT_OK || data == null) { stopSelf(); return START_NOT_STICKY; }
        MediaProjectionManager mpm = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        projection = mpm.getMediaProjection(code, data);
        thread = new HandlerThread("roadguard-mirror");
        thread.start();
        handler = new Handler(thread.getLooper());
        projection.registerCallback(new MediaProjection.Callback() {
            @Override public void onStop() { stopSelf(); }
        }, handler);
        instance = this;
        wantVideo = intent.getBooleanExtra(EXTRA_VIDEO, true);
        wantAudio = intent.getBooleanExtra(EXTRA_AUDIO, false);
        if (wantVideo) { handler.post(this::startEncoding); watchRotation(); }
        if (wantAudio) startAudio();
        return START_NOT_STICKY;
    }

    private void startForegroundNow() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.createNotificationChannel(new NotificationChannel(CHANNEL, "ROADGUARD miroir", NotificationManager.IMPORTANCE_LOW));
        PendingIntent stop = PendingIntent.getService(this, 2, new Intent(this, MirrorService.class).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE);
        Notification n = new Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_slideshow)
            .setContentTitle("Écran affiché sur l'autoradio")
            .setContentText("Touchez « Arrêter » pour ne plus partager l'écran.")
            .addAction(new Notification.Action.Builder(null, "Arrêter", stop).build())
            .setOngoing(true).build();
        if (Build.VERSION.SDK_INT >= 29) startForeground(4740, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        else startForeground(4740, n);
    }

    /** Taille vidéo : côté long 1280, proportions de l'écran, multiples de 16. */
    private void computeSize() {
        DisplayMetrics m = new DisplayMetrics();
        Display d = ((DisplayManager) getSystemService(DISPLAY_SERVICE)).getDisplay(Display.DEFAULT_DISPLAY);
        d.getRealMetrics(m);
        float scale = (float) LONG_SIDE / Math.max(m.widthPixels, m.heightPixels);
        width = Math.max(16, Math.round(m.widthPixels * scale / 16f) * 16);
        height = Math.max(16, Math.round(m.heightPixels * scale / 16f) * 16);
    }

    private void startEncoding() {
        try {
            computeSize();
            MediaFormat f = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
            f.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            f.setInteger(MediaFormat.KEY_BIT_RATE, 2_500_000);
            f.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
            f.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
            // Faible latence : profil de base (pas d'images B à réordonner), priorité temps réel, débit constant.
            f.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline);
            f.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AVCLevel31);
            f.setInteger(MediaFormat.KEY_PRIORITY, 0);
            f.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR);
            if (Build.VERSION.SDK_INT >= 26) f.setInteger(MediaFormat.KEY_LATENCY, 1);
            f.setInteger("max-bframes", 0);
            // Une image est renvoyée même si l'écran ne change pas : l'autoradio reste à jour.
            f.setLong(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, 500_000);
            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            encoder.setCallback(new MediaCodec.Callback() {
                @Override public void onInputBufferAvailable(MediaCodec c, int i) {}
                @Override public void onOutputBufferAvailable(MediaCodec c, int i, MediaCodec.BufferInfo info) {
                    try {
                        ByteBuffer buf = c.getOutputBuffer(i);
                        if (buf != null && info.size > 0) {
                            byte[] bytes = new byte[info.size];
                            buf.position(info.offset);
                            buf.get(bytes);
                            int type;
                            if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) { lastConfig = bytes; type = LinkProtocol.BIN_VIDEO_CONFIG; }
                            else if ((info.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0) type = LinkProtocol.BIN_VIDEO_KEYFRAME;
                            else type = LinkProtocol.BIN_VIDEO_FRAME;
                            LinkService s = LinkService.instance;
                            if (s != null) s.sendVideo(type, bytes, width, height);
                        }
                        c.releaseOutputBuffer(i, false);
                    } catch (IllegalStateException ignored) {}
                }
                @Override public void onError(MediaCodec c, MediaCodec.CodecException e) { Log.w(TAG, "Encodeur", e); }
                @Override public void onOutputFormatChanged(MediaCodec c, MediaFormat format) {}
            }, handler);
            try {
                encoder.configure(f, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            } catch (Exception lowLatencyRefused) {
                // Encodeur qui refuse les réglages faible latence : réglages de base.
                for (String k : new String[]{MediaFormat.KEY_PROFILE, MediaFormat.KEY_LEVEL, MediaFormat.KEY_PRIORITY,
                        MediaFormat.KEY_BITRATE_MODE, MediaFormat.KEY_LATENCY, "max-bframes"}) f.removeKey(k);
                encoder.configure(f, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            }
            inputSurface = encoder.createInputSurface();
            encoder.start();
            int dpi = getResources().getDisplayMetrics().densityDpi;
            if (display == null) {
                display = projection.createVirtualDisplay("roadguard-mirror", width, height, dpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, inputSurface, null, handler);
            } else {
                display.resize(width, height, dpi);
                display.setSurface(inputSurface);
            }
        } catch (Exception e) {
            Log.w(TAG, "Miroir impossible", e);
            stopSelf();
        }
    }

    private void stopEncoding() {
        if (display != null) display.setSurface(null);
        if (encoder != null) { try { encoder.stop(); } catch (Exception ignored) {} encoder.release(); encoder = null; }
        if (inputSurface != null) { inputSurface.release(); inputSurface = null; }
    }

    /** Rotation du téléphone : nouvelle taille d'image. */
    private void watchRotation() {
        DisplayManager dm = (DisplayManager) getSystemService(DISPLAY_SERVICE);
        rotationListener = new DisplayManager.DisplayListener() {
            @Override public void onDisplayAdded(int id) {}
            @Override public void onDisplayRemoved(int id) {}
            @Override public void onDisplayChanged(int id) {
                if (id != Display.DEFAULT_DISPLAY || handler == null) return;
                handler.post(() -> {
                    int oldW = width, oldH = height;
                    computeSize();
                    if (oldW == width && oldH == height) return;
                    stopEncoding();
                    startEncoding();
                });
            }
        };
        dm.registerDisplayListener(rotationListener, handler);
    }

    /** L'autoradio vient de se (re)connecter : lui renvoyer la configuration et une image complète. */
    public void requestKeyFrame() {
        Handler h = handler;
        if (h == null) return;
        h.post(() -> {
            LinkService s = LinkService.instance;
            if (s != null && lastConfig != null) s.sendVideo(LinkProtocol.BIN_VIDEO_CONFIG, lastConfig, width, height);
            if (encoder != null) {
                Bundle b = new Bundle();
                b.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0);
                try { encoder.setParameters(b); } catch (IllegalStateException ignored) {}
            }
        });
    }

    public static void start(Context c, int resultCode, Intent data, boolean video, boolean audio) {
        c.startForegroundService(new Intent(c, MirrorService.class).putExtra(EXTRA_RESULT_CODE, resultCode).putExtra(EXTRA_DATA, data)
            .putExtra(EXTRA_VIDEO, video).putExtra(EXTRA_AUDIO, audio));
    }

    /**
     * Son des applis (musique, GPS…) capté avec l'accord donné pour le partage, envoyé au poste en PCM.
     * Les applis qui interdisent la capture restent sur le téléphone.
     */
    @android.annotation.SuppressLint("MissingPermission")
    private void startAudio() {
        if (Build.VERSION.SDK_INT < 29) return;
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) return;
        android.media.AudioPlaybackCaptureConfiguration config = new android.media.AudioPlaybackCaptureConfiguration.Builder(projection)
            .addMatchingUsage(android.media.AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(android.media.AudioAttributes.USAGE_GAME)
            .addMatchingUsage(android.media.AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
            .addMatchingUsage(android.media.AudioAttributes.USAGE_UNKNOWN)
            .build();
        android.media.AudioFormat format = new android.media.AudioFormat.Builder()
            .setEncoding(android.media.AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(LinkProtocol.AUDIO_RATE)
            .setChannelMask(android.media.AudioFormat.CHANNEL_IN_STEREO).build();
        int chunk = LinkProtocol.AUDIO_RATE / 50 * 4; // 20 ms stéréo 16 bits
        android.media.AudioRecord record = new android.media.AudioRecord.Builder()
            .setAudioFormat(format)
            .setBufferSizeInBytes(Math.max(chunk * 8, android.media.AudioRecord.getMinBufferSize(LinkProtocol.AUDIO_RATE,
                android.media.AudioFormat.CHANNEL_IN_STEREO, android.media.AudioFormat.ENCODING_PCM_16BIT)))
            .setAudioPlaybackCaptureConfig(config).build();
        audioRunning = true;
        audioThread = new Thread(() -> {
            byte[] buf = new byte[chunk];
            try {
                record.startRecording();
                while (audioRunning) {
                    int n = record.read(buf, 0, buf.length);
                    LinkService s = LinkService.instance;
                    if (n > 0 && s != null) s.sendAudio(buf, n);
                }
            } finally {
                try { record.stop(); } catch (IllegalStateException ignored) {}
                record.release();
            }
        }, "roadguard-audio");
        audioThread.start();
    }

    @Override public void onDestroy() {
        instance = null;
        audioRunning = false;
        if (rotationListener != null) ((DisplayManager) getSystemService(DISPLAY_SERVICE)).unregisterDisplayListener(rotationListener);
        Handler h = handler;
        if (h != null) h.post(() -> {
            stopEncoding();
            if (display != null) { display.release(); display = null; }
            if (projection != null) { projection.stop(); projection = null; }
            LinkService s = LinkService.instance;
            if (s != null) s.sendMirrorStopped();
            thread.quitSafely();
        });
        super.onDestroy();
    }
}
