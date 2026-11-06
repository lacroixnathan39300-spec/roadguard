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
 * Anti-somnolence : téléphone sur son support, caméra avant tournée vers le conducteur.
 * Yeux qui se ferment → alarme forte et « Réveille-toi, arrête-toi ! » ; fatigue → conseil de pause.
 * Tout est analysé sur le téléphone, aucune image n'est enregistrée ni envoyée.
 */
@Composable
fun DrowsinessScreen(session: DrivingSession?, onClose: () -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    val view = LocalView.current
    var granted by remember { mutableStateOf(context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    LaunchedEffect(Unit) { if (!granted) ask.launch(Manifest.permission.CAMERA) }
    var state by remember { mutableStateOf(Alertness.SEARCHING) }
    var perclos by remember { mutableStateOf(0.0) }
    var yawns by remember { mutableStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    val math = remember { DrowsyMath() }
    val landmarker = remember {
        runCatching {
            FaceLandmarker.createFromOptions(context, FaceLandmarker.FaceLandmarkerOptions.builder()
                .setBaseOptions(BaseOptions.builder().setModelAssetPath("models/face_landmarker.task").build())
                .setRunningMode(RunningMode.VIDEO).setNumFaces(1).setOutputFaceBlendshapes(true).build())
        }.onFailure { error = "Modèle de visage absent de cette version : ${it.message}" }.getOrNull()
    }
    val tones = remember { runCatching { ToneGenerator(AudioManager.STREAM_ALARM, 100) }.getOrNull() }
    val executor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) { onDispose { executor.shutdown(); runCatching { landmarker?.close() }; tones?.release() } }

    // Sons : endormi → sirène continue + voix toutes les 4 s ; fatigue → conseil de pause toutes les 5 min.
    var lastTired by remember { mutableStateOf(0L) }
    LaunchedEffect(state) {
        when (state) {
            Alertness.ASLEEP -> {
                var n = 0
                while (true) {
                    tones?.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 900)
                    if (n++ % 4 == 0) session?.voice?.wakeUp()
                    kotlinx.coroutines.delay(1_000)
                }
            }
            Alertness.TIRED -> {
                val now = SystemClock.elapsedRealtime()
                if (lastTired == 0L || now - lastTired > 300_000) { lastTired = now; tones?.startTone(ToneGenerator.TONE_PROP_BEEP2, 400); session?.voice?.tired() }
            }
            Alertness.AWAY -> tones?.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
            else -> Unit
        }
        Unit
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (granted && landmarker != null) {
            AndroidView(factory = { ctx ->
                val previewView = PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
                val providerFuture = ProcessCameraProvider.getInstance(ctx)
                providerFuture.addListener({
                    runCatching {
                        val provider = providerFuture.get()
                        val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                            .build()
                        analysis.setAnalyzer(executor) { proxy ->
                            try {
                                val raw = proxy.toBitmap()
                                val rot = proxy.imageInfo.rotationDegrees
                                val bmp = if (rot == 0) raw else Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, Matrix().apply { postRotate(rot.toFloat()) }, true)
                                val r = landmarker.detectForVideo(BitmapImageBuilder(bmp).build(), SystemClock.uptimeMillis())
                                val shapes = r.faceBlendshapes().orElse(null)?.firstOrNull()
                                fun score(name: String) = shapes?.firstOrNull { it.categoryName() == name }?.score() ?: 0f
                                val blink = (score("eyeBlinkLeft") + score("eyeBlinkRight")) / 2f
                                val st = math.update(shapes != null, blink, score("jawOpen"), SystemClock.elapsedRealtime())
                                view.post { if (st != state) state = st; perclos = math.perclos; yawns = math.yawnCount }
                            } catch (_: Throwable) {
                            } finally { proxy.close() }
                        }
                        provider.unbindAll()
                        provider.bindToLifecycle(lifecycle, CameraSelector.DEFAULT_FRONT_CAMERA, preview, analysis)
                    }.onFailure { error = "Caméra avant indisponible : ${it.message}" }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            }, modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(12.dp).size(150.dp, 200.dp).clip(RoundedCornerShape(18.dp)))
        }

        Row(Modifier.align(Alignment.TopStart).statusBarsPadding().padding(12.dp)) {
            BigButton("✕ Fermer", Color(0xCC000000), onClose)
        }

        Column(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(12.dp).fillMaxWidth()
                .clip(RoundedCornerShape(22.dp)).background(Color(state.color).copy(alpha = 0.92f)).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(error ?: if (!granted) "Autorise la caméra pour la détection de somnolence" else state.text,
                color = Color.White, fontSize = if (state == Alertness.ASLEEP) 40.sp else 28.sp, fontWeight = FontWeight.Black)
            Text("Yeux fermés : ${(perclos * 100).toInt()} % de la dernière minute · bâillements : $yawns (10 min)",
                color = Color.White, fontSize = 16.sp)
            Text("Place le téléphone face à toi (caméra avant). Analyse sur le téléphone, aucune image enregistrée. " +
                "C'est une aide : au moindre signe de fatigue, arrête-toi.", color = Color(0xDDFFFFFF), fontSize = 13.sp)
        }
    }
}
