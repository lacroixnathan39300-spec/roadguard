package fr.roadguard.app.next

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import fr.roadguard.core.DrivingSession
import fr.roadguard.core.ui.BigButton
import java.util.concurrent.Executors

/** État du conducteur vu par la caméra avant. */
enum class Alertness(val color: Long, val text: String) {
    SEARCHING(0xFF455A64, "Cherche ton visage…"), AWAKE(0xFF2E7D32, "Vigilant"),
    AWAY(0xFFF57C00, "Regarde la route"), TIRED(0xFFF57C00, "Fatigue : fais une pause"), ASLEEP(0xFFD32F2F, "RÉVEILLE-TOI !"),
}

/**
 * Détection de somnolence (testable) :
 *  - yeux fermés plus de 1,2 s d'affilée → ASLEEP (alarme) ;
 *  - yeux fermés plus de 25 % du temps sur la dernière minute (PERCLOS), ou 3 bâillements en 10 min → TIRED ;
 *  - pas de visage plus de 3 s → AWAY.
 */
class DrowsyMath {
    private var closedSince = 0L
    private var lastFace = 0L
    private val samples = ArrayDeque<Pair<Long, Boolean>>()   // (temps, yeux fermés) sur 60 s
    private var yawnSince = 0L
    private val yawns = ArrayDeque<Long>()
    var perclos = 0.0; private set
    val yawnCount get() = yawns.size

    fun update(face: Boolean, blink: Float, jaw: Float, now: Long): Alertness {
        if (!face) {
            closedSince = 0L
            return if (lastFace != 0L && now - lastFace > 3_000) Alertness.AWAY else Alertness.SEARCHING
        }
        lastFace = now
        val closed = blink > 0.55f
        if (closed) { if (closedSince == 0L) closedSince = now } else closedSince = 0L
        samples.addLast(now to closed)
        while (samples.isNotEmpty() && now - samples.first().first > 60_000) samples.removeFirst()
        perclos = samples.count { it.second }.toDouble() / samples.size
        if (jaw > 0.6f) { if (yawnSince == 0L) yawnSince = now } else {
            if (yawnSince != 0L && now - yawnSince > 1_500) yawns.addLast(now)
            yawnSince = 0L
        }
        while (yawns.isNotEmpty() && now - yawns.first() > 600_000) yawns.removeFirst()
        return when {
            closedSince != 0L && now - closedSince > 1_200 -> Alertness.ASLEEP
            (samples.size > 100 && perclos > 0.25) || yawns.size >= 3 -> Alertness.TIRED
            else -> Alertness.AWAKE
        }
    }
}

/**
 * Anti-somnolence en arrière-plan de l'accueil : la caméra avant analyse le visage sans rien afficher
 * (la carte reste normale). Yeux fermés → sirène et « Réveille-toi, arrête-toi ! » ; fatigue → conseil de pause.
 * Fonctionne tant que Pilote est à l'écran ; tout est analysé sur le téléphone, aucune image enregistrée ni envoyée.
 * Renvoie l'état courant (pour le petit voyant et l'écran rouge de réveil).
 */
@Composable
fun rememberDrowsyGuard(session: DrivingSession?, enabled: Boolean): Alertness? {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    var granted by remember { mutableStateOf(context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    LaunchedEffect(enabled) { if (enabled && !granted) ask.launch(Manifest.permission.CAMERA) }
    var state by remember { mutableStateOf(Alertness.SEARCHING) }
    if (!enabled || !granted) return null

    val math = remember { DrowsyMath() }
    val main = remember { android.os.Handler(android.os.Looper.getMainLooper()) }
    DisposableEffect(lifecycle) {
        val executor = Executors.newSingleThreadExecutor()
        val landmarker = runCatching {
            FaceLandmarker.createFromOptions(context, FaceLandmarker.FaceLandmarkerOptions.builder()
                .setBaseOptions(BaseOptions.builder().setModelAssetPath("models/face_landmarker.task").build())
                .setRunningMode(RunningMode.VIDEO).setNumFaces(1).setOutputFaceBlendshapes(true).build())
        }.getOrNull()
        val providerFuture = ProcessCameraProvider.getInstance(context)
        var analysis: ImageAnalysis? = null
        if (landmarker != null) providerFuture.addListener({
            runCatching {
                val provider = providerFuture.get()
                val a = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .setTargetResolution(android.util.Size(480, 640))
                    .build()
                var lastRun = 0L
                a.setAnalyzer(executor) { proxy ->
                    try {
                        // 8 images par seconde suffisent, et ménagent la batterie.
                        val now = SystemClock.uptimeMillis()
                        if (now - lastRun < 120) return@setAnalyzer
                        lastRun = now
                        val raw = proxy.toBitmap()
                        val rot = proxy.imageInfo.rotationDegrees
                        val bmp = if (rot == 0) raw else Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, Matrix().apply { postRotate(rot.toFloat()) }, true)
                        val r = landmarker.detectForVideo(BitmapImageBuilder(bmp).build(), now)
                        val shapes = r.faceBlendshapes().orElse(null)?.firstOrNull()
                        fun score(name: String) = shapes?.firstOrNull { it.categoryName() == name }?.score() ?: 0f
                        val blink = (score("eyeBlinkLeft") + score("eyeBlinkRight")) / 2f
                        val st = math.update(shapes != null, blink, score("jawOpen"), SystemClock.elapsedRealtime())
                        main.post { if (st != state) state = st }
                    } catch (_: Throwable) {
                    } finally { proxy.close() }
                }
                analysis = a
                provider.bindToLifecycle(lifecycle, CameraSelector.DEFAULT_FRONT_CAMERA, a)
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            runCatching { analysis?.let { providerFuture.get().unbind(it) } }
            executor.shutdown(); runCatching { landmarker?.close() }
        }
    }

    // Sons : endormi → sirène + voix toutes les 4 s ; fatigue → conseil de pause toutes les 5 min.
    val tones = remember { runCatching { ToneGenerator(AudioManager.STREAM_ALARM, 100) }.getOrNull() }
    DisposableEffect(Unit) { onDispose { tones?.release() } }
    var lastTired by remember { mutableStateOf(0L) }
    LaunchedEffect(state) {
        when (state) {
            Alertness.ASLEEP -> {
                // Sirène de police française en boucle (réglage), arrêtée dès que les yeux se rouvrent.
                val siren = session?.settings?.sirenWake != false
                if (siren) fr.roadguard.core.radar.PoliceSiren.startAlarm()
                try {
                    var n = 0
                    while (true) {
                        if (!siren) tones?.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 900)
                        if (n++ % 4 == 0) session?.voice?.wakeUp()
                        kotlinx.coroutines.delay(1_000)
                    }
                } finally {
                    if (siren) fr.roadguard.core.radar.PoliceSiren.stop()
                }
            }
            Alertness.TIRED -> {
                val now = SystemClock.elapsedRealtime()
                if (lastTired == 0L || now - lastTired > 300_000) { lastTired = now; tones?.startTone(ToneGenerator.TONE_PROP_BEEP2, 400); session?.voice?.tired() }
            }
            else -> Unit
        }
        Unit
    }
    return state
}

/** Écran rouge plein écran quand le conducteur s'endort (un appui le ferme). */
@Composable
fun WakeUpOverlay(onDismiss: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xF0D32F2F)).padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("😴", fontSize = 72.sp)
            Text("RÉVEILLE-TOI !", color = Color.White, fontSize = 46.sp, fontWeight = FontWeight.Black)
            Text("Arrête-toi dès que possible, en sécurité.", color = Color.White, fontSize = 22.sp)
            BigButton("Je suis réveillé", Color(0xFF1B1B1B), onDismiss)
        }
    }
}
