package fr.roadguard.app.link;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import fr.roadguard.app.SpotifySessionListener;
import fr.roadguard.link.LinkProtocol;

/**
 * ROADGUARD Link côté téléphone : attend l'autoradio sur le Wi-Fi (partage de connexion),
 * lui envoie la position GPS du téléphone et la musique en cours, et exécute ses commandes
 * (lecture/pause, suivant…). Une seule voiture connectée à la fois.
 */
public final class LinkService extends Service {
    public static final String ACTION_STOP = "fr.roadguard.app.link.STOP";
    private static final String TAG = "RoadguardLink";
    private static final String CHANNEL = "roadguard_link";
    private static final int NOTIFICATION_ID = 4739;

    /** État lisible par l'écran ROADGUARD Link. */
    public static volatile String status = "Arrêtée";
    public static volatile boolean running;
    /** Service en cours, pour le relais des notifications. */
    public static volatile LinkService instance;
    /** Actions des notifications transmises (clé → actions), pour les exécuter depuis l'autoradio. */
    private final java.util.Map<String, Notification.Action[]> actions = new java.util.concurrent.ConcurrentHashMap<>();

    private final Handler main = new Handler(Looper.getMainLooper());
    private final AtomicReference<LinkProtocol.Session> current = new AtomicReference<>();
    /** Un seul fil d'envoi : l'ordre des messages est conservé et le fil principal n'attend jamais le réseau. */
    private final java.util.concurrent.ExecutorService sender = java.util.concurrent.Executors.newSingleThreadExecutor();
    private ServerSocket server;
    private Thread acceptThread;
    private NsdManager.RegistrationListener nsdListener;
    private LocationListener locationListener;
    private MediaSessionManager.OnActiveSessionsChangedListener sessionsListener;
    private MediaController watched;
    private final MediaController.Callback mediaCallback = new MediaController.Callback() {
        @Override public void onMetadataChanged(MediaMetadata metadata) { sendMedia(); }
        @Override public void onPlaybackStateChanged(PlaybackState state) { sendMedia(); }
    };

    public static SharedPreferences prefs(Context c) { return c.getSharedPreferences("roadguard_link", MODE_PRIVATE); }

    /** Identifiant stable de ce téléphone pour l'autoradio. */
    public static String phoneId(Context c) {
        SharedPreferences p = prefs(c);
        String id = p.getString("phone_id", null);
        if (id == null) { id = LinkProtocol.b64(LinkProtocol.random(9)); p.edit().putString("phone_id", id).apply(); }
        return id;
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) { stopSelf(); return START_NOT_STICKY; }
        startForegroundNow();
        if (!running) start();
        // Câble USB : le poste a basculé le téléphone en mode accessoire.
        android.hardware.usb.UsbAccessory accessory = intent == null ? null
            : intent.getParcelableExtra(android.hardware.usb.UsbManager.EXTRA_ACCESSORY);
        if (accessory != null) new Thread(() -> serveAccessory(accessory), "roadguard-link-usb").start();
        return START_STICKY;
    }

    private void startForegroundNow() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(new NotificationChannel(CHANNEL, "ROADGUARD Link", NotificationManager.IMPORTANCE_LOW));
        }
        PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, LinkActivity.class), PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop = PendingIntent.getService(this, 1, new Intent(this, LinkService.class).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE);
        Notification n = new Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentTitle("ROADGUARD Link")
            .setContentText(status)
            .setContentIntent(open)
            .addAction(new Notification.Action.Builder(null, "Arrêter", stop).build())
            .setOngoing(true)
            .build();
        if (Build.VERSION.SDK_INT >= 29) startForeground(NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        else startForeground(NOTIFICATION_ID, n);
    }

    private void setStatus(String s) {
        status = s;
        main.post(this::startForegroundNow);
    }

    private void start() {
        running = true;
        instance = this;
        setStatus("En attente de l'autoradio…");
        acceptThread = new Thread(this::acceptLoop, "roadguard-link-accept");
        acceptThread.start();
        registerNsd();
        startLocation();
        startMedia();
    }

    private void acceptLoop() {
        try {
            server = new ServerSocket(LinkProtocol.PORT);
            while (running) {
                Socket socket = server.accept();
                new Thread(() -> serve(socket), "roadguard-link-session").start();
            }
        } catch (IOException e) {
            if (running) { Log.w(TAG, "Serveur arrêté", e); setStatus("Erreur réseau : " + e.getMessage()); }
        }
    }

    private void serve(Socket socket) {
        try {
            socket.setSoTimeout(20_000);
            socket.setTcpNoDelay(true);
            serve(socket.getInputStream(), socket.getOutputStream(), socket, "Wi-Fi");
        } catch (IOException e) {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private void serveAccessory(android.hardware.usb.UsbAccessory accessory) {
        android.hardware.usb.UsbManager usb = (android.hardware.usb.UsbManager) getSystemService(USB_SERVICE);
        android.os.ParcelFileDescriptor pfd;
        try {
            pfd = usb.openAccessory(accessory);
        } catch (SecurityException e) {
            setStatus("Câble USB : autorisation refusée");
            return;
        }
        if (pfd == null) { setStatus("Câble USB : liaison impossible"); return; }
        java.io.FileDescriptor fd = pfd.getFileDescriptor();
        // Lectures par blocs de 16 Ko : exigé par le mode accessoire pour ne pas perdre de données.
        java.io.InputStream in = new java.io.BufferedInputStream(new java.io.FileInputStream(fd), 16_384);
        java.io.OutputStream out = new java.io.FileOutputStream(fd);
        serve(in, out, pfd, "câble USB");
    }

    private void serve(java.io.InputStream input, java.io.OutputStream output, java.io.Closeable link, String via) {
        LinkProtocol.Session session = null;
        try {
            session = LinkProtocol.serverHandshake(input, output, phoneId(this), Build.MODEL, peers());
            LinkProtocol.Session old = current.getAndSet(session);
            if (old != null) closeQuietly(old);
            setStatus("Connecté à " + (session.peerName.isEmpty() ? "l'autoradio" : session.peerName) + " (" + via + ")");
            sendMedia();
            while (running) {
                JSONObject m = session.receive();
                handle(m);
            }
        } catch (IOException e) {
            Log.i(TAG, "Session terminée : " + e.getMessage());
            if (e instanceof LinkProtocol.AuthException) setStatus(e.getMessage());
        } finally {
            if (session != null && current.compareAndSet(session, null) && running) setStatus("En attente de l'autoradio…");
            try { link.close(); } catch (IOException ignored) {}
        }
    }

    private void handle(JSONObject m) {
        String type = m.optString("t");
        if ("media_cmd".equals(type)) {
            MediaController c = watched;
            if (c == null) return;
            switch (m.optString("cmd")) {
                case "play": c.getTransportControls().play(); break;
                case "pause": c.getTransportControls().pause(); break;
                case "next": c.getTransportControls().skipToNext(); break;
                case "previous": c.getTransportControls().skipToPrevious(); break;
                default: break;
            }
        }
        if ("http".equals(type)) { relayHttp(m); return; }
        if ("touch".equals(type)) {
            MirrorTouchService t = MirrorTouchService.instance;
            if (t != null) t.touch(m.optString("a"), (float) m.optDouble("x"), (float) m.optDouble("y"), m.optLong("time"));
            return;
        }
        if ("mirror_keyframe".equals(type)) {
            MirrorService mirror = MirrorService.instance;
            if (mirror != null) mirror.requestKeyFrame();
            return;
        }
        if ("mirror_stop".equals(type)) {
            if (MirrorService.instance != null) startService(new Intent(this, MirrorService.class).setAction(MirrorService.ACTION_STOP));
            return;
        }
        if ("notif_action".equals(type)) {
            Notification.Action[] list = actions.get(m.optString("key"));
            int i = m.optInt("index", -1);
            if (list != null && i >= 0 && i < list.length && list[i].actionIntent != null) {
                try { list[i].actionIntent.send(); } catch (PendingIntent.CanceledException ignored) {}
            }
        }
        // "ping" : rien à faire, la réception suffit à garder la connexion vivante.
    }

    private LinkProtocol.Peers peers() {
        Context ctx = this;
        return new LinkProtocol.Peers() {
            @Override public byte[] secretFor(String id) {
                String s = prefs(ctx).getString("peer_" + id, null);
                return s == null ? null : LinkProtocol.unb64(s);
            }
            @Override public String pendingCode() {
                SharedPreferences p = prefs(ctx);
                return System.currentTimeMillis() < p.getLong("pending_until", 0) ? p.getString("pending_code", null) : null;
            }
            @Override public void paired(String id, String name, byte[] secret) {
                prefs(ctx).edit().putString("peer_" + id, LinkProtocol.b64(secret))
                    .putString("peer_name_" + id, name).remove("pending_code").remove("pending_until").apply();
            }
        };
    }

    private void send(JSONObject m) {
        LinkProtocol.Session s = current.get();
        if (s == null) return;
        try { s.send(m); } catch (IOException e) { closeQuietly(s); current.compareAndSet(s, null); }
    }

    // ---- Notifications : messages et appels ----------------------------------------------

    public static boolean notificationsEnabled(Context c) { return prefs(c).getBoolean("forward_notifications", true); }

    /** Appelé par le service d'accès aux notifications. */
    public void onNotificationPosted(android.service.notification.StatusBarNotification sbn) {
        if (current.get() == null || !notificationsEnabled(this)) return;
        Notification n = sbn.getNotification();
        if (sbn.getPackageName().equals(getPackageName())) return;
        if ((n.flags & Notification.FLAG_GROUP_SUMMARY) != 0) return;
        String category = n.category == null ? "" : n.category;
        boolean call = Notification.CATEGORY_CALL.equals(category);
        boolean message = Notification.CATEGORY_MESSAGE.equals(category) || Notification.CATEGORY_EMAIL.equals(category)
            || n.extras.containsKey(Notification.EXTRA_MESSAGES);
        if (!call && !message) return;
        CharSequence title = n.extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence text = n.extras.getCharSequence(Notification.EXTRA_TEXT);
        if (title == null && text == null) return;
        try {
            JSONObject m = new JSONObject().put("t", "notif").put("key", sbn.getKey())
                .put("kind", call ? "call" : "message")
                .put("app", appLabel(sbn.getPackageName()))
                .put("title", title == null ? "" : title.toString())
                .put("text", text == null ? "" : text.toString());
            org.json.JSONArray labels = new org.json.JSONArray();
            Notification.Action[] list = n.actions;
            if (list != null) {
                for (Notification.Action a : list) {
                    // Les actions qui demandent une saisie (répondre par écrit) ne sont pas proposées en voiture.
                    boolean needsText = a.getRemoteInputs() != null && a.getRemoteInputs().length > 0;
                    labels.put(needsText || a.title == null ? "" : a.title.toString());
                }
                actions.put(sbn.getKey(), list);
            }
            m.put("actions", labels);
            sender.execute(() -> send(m));
        } catch (org.json.JSONException ignored) {}
    }

    public void onNotificationRemoved(android.service.notification.StatusBarNotification sbn) {
        if (actions.remove(sbn.getKey()) == null || current.get() == null) return;
        try {
            JSONObject m = new JSONObject().put("t", "notif_removed").put("key", sbn.getKey());
            sender.execute(() -> send(m));
        } catch (org.json.JSONException ignored) {}
    }

    // ---- Internet de l'autoradio par le téléphone ------------------------------------------

    private final java.util.concurrent.ExecutorService http = java.util.concurrent.Executors.newFixedThreadPool(4);
    private static final int MAX_HTTP_BODY = 3 * 1024 * 1024;

    /** Requête HTTPS faite pour le poste (carte, recherche, itinéraires). Jamais vers le réseau local. */
    private void relayHttp(JSONObject m) {
        final int id = m.optInt("id");
        http.execute(() -> {
            JSONObject reply = new JSONObject();
            try {
                reply.put("t", "http_resp").put("id", id);
                java.net.URL url = new java.net.URL(m.getString("url"));
                if (!"https".equals(url.getProtocol())) throw new IOException("Seul HTTPS est relayé");
                java.net.InetAddress addr = java.net.InetAddress.getByName(url.getHost());
                if (addr.isLoopbackAddress() || addr.isSiteLocalAddress() || addr.isLinkLocalAddress() || addr.isAnyLocalAddress())
                    throw new IOException("Adresse locale refusée");
                java.net.HttpURLConnection c = (java.net.HttpURLConnection) url.openConnection();
                c.setConnectTimeout(10_000);
                c.setReadTimeout(20_000);
                c.setInstanceFollowRedirects(m.optBoolean("follow", true));
                c.setRequestMethod(m.optString("method", "GET"));
                c.setRequestProperty("User-Agent", "ROADGUARD-DRIVE/2.0 (via téléphone)");
                if (m.has("ctype")) c.setRequestProperty("Content-Type", m.getString("ctype"));
                try {
                    if (m.has("body")) {
                        c.setDoOutput(true);
                        try (java.io.OutputStream o = c.getOutputStream()) { o.write(LinkProtocol.unb64(m.getString("body"))); }
                    }
                    int code = c.getResponseCode();
                    reply.put("code", code);
                    if (code >= 300 && code < 400 && !m.optBoolean("follow", true)) {
                        String loc = c.getHeaderField("Location");
                        reply.put("body", LinkProtocol.b64((loc == null ? "" : loc).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                    } else {
                        java.io.InputStream in = code >= 200 && code < 300 ? c.getInputStream() : c.getErrorStream();
                        if (in != null) {
                            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                            byte[] buf = new byte[16_384];
                            int n;
                            try (java.io.InputStream input = in) {
                                while ((n = input.read(buf)) > 0) {
                                    if (out.size() + n > MAX_HTTP_BODY) throw new IOException("Réponse trop grande");
                                    out.write(buf, 0, n);
                                }
                            }
                            reply.put("body", LinkProtocol.b64(out.toByteArray()));
                        }
                        if (c.getContentType() != null) reply.put("ctype", c.getContentType());
                    }
                } finally {
                    c.disconnect();
                }
            } catch (Exception e) {
                try { reply.put("error", e.getMessage() == null ? "Erreur réseau" : e.getMessage()); } catch (org.json.JSONException ignored) {}
            }
            sender.execute(() -> send(reply));
        });
    }

    // ---- Mode miroir -----------------------------------------------------------------------

    private final java.util.concurrent.atomic.AtomicInteger pendingVideo = new java.util.concurrent.atomic.AtomicInteger();
    private volatile boolean waitingKeyFrame;
    private volatile int sentW, sentH;

    /** Image vidéo à transmettre ; les images ordinaires sont sautées si le Wi-Fi n'arrive pas à suivre. */
    public void sendVideo(int type, byte[] data, int w, int h) {
        LinkProtocol.Session s = current.get();
        if (s == null) return;
        if (type == LinkProtocol.BIN_VIDEO_FRAME && (waitingKeyFrame || pendingVideo.get() > 3)) {
            if (!waitingKeyFrame) {
                waitingKeyFrame = true;
                MirrorService mirror = MirrorService.instance;
                if (mirror != null) mirror.requestKeyFrame();
            }
            return;
        }
        if (type == LinkProtocol.BIN_VIDEO_KEYFRAME) waitingKeyFrame = false;
        pendingVideo.incrementAndGet();
        sender.execute(() -> {
            try {
                if (type == LinkProtocol.BIN_VIDEO_CONFIG || w != sentW || h != sentH) {
                    sentW = w; sentH = h;
                    s.send(new JSONObject().put("t", "mirror_start").put("w", w).put("h", h));
                }
                s.sendBinary(type, data, 0, data.length);
            } catch (Exception e) {
                current.compareAndSet(s, null);
                closeQuietly(s);
            } finally {
                pendingVideo.decrementAndGet();
            }
        });
    }

    /** Son du téléphone : sauté si la liaison prend du retard (mieux vaut une coupure qu'un décalage). */
    public void sendAudio(byte[] pcm, int length) {
        LinkProtocol.Session s = current.get();
        if (s == null || pendingVideo.get() > 6) return;
        byte[] copy = java.util.Arrays.copyOf(pcm, length);
        pendingVideo.incrementAndGet();
        sender.execute(() -> {
            try { s.sendBinary(LinkProtocol.BIN_AUDIO_PCM, copy, 0, copy.length); }
            catch (Exception e) { current.compareAndSet(s, null); closeQuietly(s); }
            finally { pendingVideo.decrementAndGet(); }
        });
    }

    public void sendMirrorStopped() {
        sentW = 0; sentH = 0;
        try {
            JSONObject m = new JSONObject().put("t", "mirror_stop");
            sender.execute(() -> send(m));
        } catch (org.json.JSONException ignored) {}
    }

    /** Destination partagée depuis une autre appli (Google Maps, Waze, contact…). */
    public static boolean sendDestination(String text) {
        LinkService s = instance;
        if (s == null || s.current.get() == null || text == null || text.trim().isEmpty()) return false;
        try {
            JSONObject m = new JSONObject().put("t", "dest").put("text", text.trim());
            s.sender.execute(() -> s.send(m));
            return true;
        } catch (org.json.JSONException e) {
            return false;
        }
    }

    // ---- Position GPS du téléphone -------------------------------------------------------

    @SuppressLint("MissingPermission")
    private void startLocation() {
        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override public void onLocationChanged(Location l) { sendLocation(l); }
            @Override public void onStatusChanged(String p, int s, Bundle b) {}
            @Override public void onProviderEnabled(String p) {}
            @Override public void onProviderDisabled(String p) {}
        };
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, locationListener, Looper.getMainLooper());
    }

    private void sendLocation(Location l) {
        if (current.get() == null) return;
        try {
            JSONObject m = new JSONObject().put("t", "loc").put("lat", l.getLatitude()).put("lon", l.getLongitude())
                .put("time", l.getTime());
            if (l.hasSpeed()) m.put("speed", l.getSpeed());
            if (l.hasBearing()) m.put("bearing", l.getBearing());
            if (l.hasAccuracy()) m.put("acc", l.getAccuracy());
            sender.execute(() -> send(m));
        } catch (org.json.JSONException ignored) {}
    }

    // ---- Musique du téléphone -------------------------------------------------------------

    private boolean hasMediaAccess() {
        String enabled = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        return enabled != null && enabled.contains(getPackageName());
    }

    private void startMedia() {
        if (!hasMediaAccess()) return;
        try {
            MediaSessionManager msm = (MediaSessionManager) getSystemService(MEDIA_SESSION_SERVICE);
            ComponentName listener = new ComponentName(this, SpotifySessionListener.class);
            sessionsListener = this::watch;
            msm.addOnActiveSessionsChangedListener(sessionsListener, listener, main);
            watch(msm.getActiveSessions(listener));
        } catch (SecurityException e) {
            Log.w(TAG, "Accès à la musique refusé", e);
        }
    }

    private void watch(List<MediaController> controllers) {
        if (watched != null) watched.unregisterCallback(mediaCallback);
        watched = null;
        if (controllers != null) {
            for (MediaController c : controllers) {
                PlaybackState st = c.getPlaybackState();
                if (st != null && st.getState() == PlaybackState.STATE_PLAYING) { watched = c; break; }
            }
            if (watched == null && !controllers.isEmpty()) watched = controllers.get(0);
        }
        if (watched != null) watched.registerCallback(mediaCallback, main);
        sendMedia();
    }

    private void sendMedia() {
        if (current.get() == null) return;
        MediaController c = watched;
        try {
            JSONObject m = new JSONObject().put("t", "media");
            MediaMetadata md = c == null ? null : c.getMetadata();
            if (md != null && md.getString(MediaMetadata.METADATA_KEY_TITLE) != null) {
                m.put("title", md.getString(MediaMetadata.METADATA_KEY_TITLE));
                String artist = md.getString(MediaMetadata.METADATA_KEY_ARTIST);
                m.put("artist", artist == null ? "" : artist);
                m.put("app", appLabel(c.getPackageName()));
                PlaybackState st = c.getPlaybackState();
                m.put("playing", st != null && st.getState() == PlaybackState.STATE_PLAYING);
                Bitmap art = md.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
                if (art == null) art = md.getBitmap(MediaMetadata.METADATA_KEY_ART);
                if (art != null) m.put("art", LinkProtocol.b64(thumbnail(art)));
            }
            sender.execute(() -> send(m));
        } catch (org.json.JSONException ignored) {}
    }

    private String appLabel(String pkg) {
        try {
            PackageManager pm = getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return pkg;
        }
    }

    /** Pochette réduite (160 px, JPEG) : légère sur le Wi-Fi. */
    private static byte[] thumbnail(Bitmap art) {
        Bitmap small = Bitmap.createScaledBitmap(art, 160, 160, true);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        small.compress(Bitmap.CompressFormat.JPEG, 80, out);
        return out.toByteArray();
    }

    // ---- Annonce sur le réseau local ------------------------------------------------------

    private void registerNsd() {
        try {
            NsdServiceInfo info = new NsdServiceInfo();
            info.setServiceName("ROADGUARD " + Build.MODEL);
            info.setServiceType(LinkProtocol.SERVICE_TYPE);
            info.setPort(LinkProtocol.PORT);
            nsdListener = new NsdManager.RegistrationListener() {
                @Override public void onRegistrationFailed(NsdServiceInfo s, int e) { Log.w(TAG, "NSD : " + e); }
                @Override public void onUnregistrationFailed(NsdServiceInfo s, int e) {}
                @Override public void onServiceRegistered(NsdServiceInfo s) {}
                @Override public void onServiceUnregistered(NsdServiceInfo s) {}
            };
            ((NsdManager) getSystemService(NSD_SERVICE)).registerService(info, NsdManager.PROTOCOL_DNS_SD, nsdListener);
        } catch (RuntimeException e) {
            Log.w(TAG, "Annonce réseau impossible", e);
        }
    }

    @Override public void onDestroy() {
        running = false;
        instance = null;
        status = "Arrêtée";
        try { if (server != null) server.close(); } catch (IOException ignored) {}
        LinkProtocol.Session s = current.getAndSet(null);
        if (s != null) closeQuietly(s);
        if (locationListener != null) ((LocationManager) getSystemService(LOCATION_SERVICE)).removeUpdates(locationListener);
        if (sessionsListener != null) {
            ((MediaSessionManager) getSystemService(MEDIA_SESSION_SERVICE)).removeOnActiveSessionsChangedListener(sessionsListener);
        }
        if (watched != null) watched.unregisterCallback(mediaCallback);
        sender.shutdownNow();
        http.shutdownNow();
        if (nsdListener != null) {
            try { ((NsdManager) getSystemService(NSD_SERVICE)).unregisterService(nsdListener); } catch (RuntimeException ignored) {}
        }
        super.onDestroy();
    }

    private static void closeQuietly(LinkProtocol.Session s) {
        try { s.close(); } catch (IOException ignored) {}
    }
}
