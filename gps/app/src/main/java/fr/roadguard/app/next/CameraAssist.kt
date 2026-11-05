package fr.roadguard.app.next

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetector
import fr.roadguard.core.DrivingSession
import fr.roadguard.core.ui.BigButton
import fr.roadguard.core.ui.DriveColors
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.tan

/** Objet vu par la caméra (coordonnées dans l'image analysée, déjà tournée à l'endroit). */
data class Seen(val box: RectF, val label: String, val distanceM: Double, val lead: Boolean)

/** Niveau d'alerte de l'assistant. */
enum class AssistLevel(val color: Long, val text: String) {
    NONE(0xFF2E7D32, "Route dégagée"), OK(0xFF2E7D32, "Distance correcte"), CLOSE(0xFFF57C00, "Trop près : ralentis"),
    DANGER(0xFFD32F2F, "FREINE !"), GO(0xFF1565C0, "Le véhicule devant a démarré"),
}

/**
 * Calculs de l'assistant (testables) : distance estimée d'après la largeur apparente du véhicule,
 * temps de sécurité (règle des 2 secondes), temps avant collision, départ du véhicule devant.
 */
class AssistMath {
    var smoothed = Double.NaN; private set
    private var lastD = Double.NaN
    private var lastT = 0L
    var closingMs = 0.0; private set
    private var stoppedBase = Double.NaN

    companion object {
        /** Largeur réelle moyenne (m) selon le type détecté. */
        fun realWidth(label: String) = when (label) {
            "truck", "bus" -> 2.5
            "motorcycle", "bicycle" -> 0.8
            "person" -> 0.5
            else -> 1.8
        }

        /** Distance (m) : largeur réelle × focale (px) / largeur apparente (px). */
        fun distance(label: String, boxWidthPx: Float, focalPx: Double): Double =
            if (boxWidthPx <= 1f) Double.NaN else realWidth(label) * focalPx / boxWidthPx

        /** Focale en pixels pour une image dont le grand côté couvre [fovDeg] degrés. */
        fun focalPx(imageLongSide: Int, fovDeg: Double): Double = imageLongSide / 2.0 / tan(Math.toRadians(fovDeg / 2))
    }

    /** Nouvelle mesure du véhicule de devant (ou NaN s'il n'y en a pas) ; renvoie le niveau d'alerte. */
    fun update(distanceM: Double, speedKmh: Double, nowMs: Long): AssistLevel {
        val v = speedKmh / 3.6
        if (distanceM.isNaN() || distanceM > 120) {
            smoothed = Double.NaN; lastD = Double.NaN; closingMs = 0.0; stoppedBase = Double.NaN
            return AssistLevel.NONE
        }
        smoothed = if (smoothed.isNaN()) distanceM else smoothed * 0.6 + distanceM * 0.4
        if (!lastD.isNaN() && nowMs - lastT >= 300) {
            val dt = (nowMs - lastT) / 1000.0
            val c = (lastD - smoothed) / dt
            closingMs = closingMs * 0.5 + c * 0.5
            lastD = smoothed; lastT = nowMs
        } else if (lastD.isNaN()) { lastD = smoothed; lastT = nowMs }

        // À l'arrêt (feu, bouchon) : le véhicule devant s'éloigne de 2,5 m → il a démarré.
        if (v < 1.0) {
            if (stoppedBase.isNaN()) stoppedBase = smoothed
            if (smoothed - stoppedBase > 2.5) { stoppedBase = smoothed + 1000; return AssistLevel.GO }
            return AssistLevel.OK
        } else stoppedBase = Double.NaN

        val ttc = if (closingMs > 1.5) smoothed / closingMs else Double.POSITIVE_INFINITY
        if (ttc < 1.8 || (v > 8 && smoothed / v < 0.5)) return AssistLevel.DANGER
        if (v > 8 && smoothed / v < 1.0) return AssistLevel.CLOSE
        return AssistLevel.OK
    }

    /** Temps de sécurité (s) avec le véhicule de devant, ou NaN. */
    fun headway(speedKmh: Double): Double = if (smoothed.isNaN() || speedKmh < 10) Double.NaN else smoothed / (speedKmh / 3.6)
}

/**
 * Assistant caméra : téléphone fixé au pare-brise, caméra vers la route. Détecte voitures, camions, motos,
 * vélos et piétons (modèle embarqué, sans Internet), estime la distance du véhicule de devant et prévient :
 * trop près (moins d'1 s), risque de collision (bip rapide + « Freine »), véhicule devant qui démarre au feu.
 */
@Composable
fun CameraAssistScreen(session: DrivingSession?, onClose: () -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    val view = LocalView.current
    DisposableEffect(Unit) { view.keepScreenOn = true; onDispose { view.keepScreenOn = false } }
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    LaunchedEffect(Unit) { if (!granted) ask.launch(Manifest.permission.CAMERA) }

    val prefs = remember { context.getSharedPreferences("camera_assist", Context.MODE_PRIVATE) }
    var fov by remember { mutableStateOf(prefs.getFloat("fov", 66f)) }
    var seen by remember { mutableStateOf<List<Seen>>(emptyList()) }
    var imageSize by remember { mutableStateOf(Size(1f, 1f)) }
    var level by remember { mutableStateOf(AssistLevel.NONE) }
    var leadDistance by remember { mutableStateOf(Double.NaN) }
    var headway by remember { mutableStateOf(Double.NaN) }
    var error by remember { mutableStateOf<String?>(null) }
    val math = remember { AssistMath() }

    val detector = remember {
        runCatching {
            ObjectDetector.createFromOptions(context, ObjectDetector.ObjectDetectorOptions.builder()
                .setBaseOptions(BaseOptions.builder().setModelAssetPath("models/efficientdet_lite0.tflite").build())
                .setRunningMode(RunningMode.VIDEO).setScoreThreshold(0.35f).setMaxResults(10)
                .setCategoryAllowlist(listOf("car", "truck", "bus", "motorcycle", "bicycle", "person")).build())
        }.onFailure { error = "Modèle de détection absent de cette version : ${it.message}" }.getOrNull()
    }
    val tones = remember { runCatching { ToneGenerator(AudioManager.STREAM_ALARM, 100) }.getOrNull() }
    val executor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) { onDispose { executor.shutdown(); runCatching { detector?.close() }; tones?.release() } }

    // Sons : collision (bip rapide répété), trop près (double bip), démarrage (carillon). Sans spammer.
    var lastSound by remember { mutableStateOf(0L) }
    LaunchedEffect(level) {
        val now = SystemClock.elapsedRealtime()
        when (level) {
            AssistLevel.DANGER -> while (true) {
                tones?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 250)
                if (now - lastSound > 4000) { lastSound = now; session?.voice?.speak("Freine !") }
                kotlinx.coroutines.delay(350)
            }
            AssistLevel.CLOSE -> { if (now - lastSound > 5000) { lastSound = now; tones?.startTone(ToneGenerator.TONE_PROP_BEEP2, 300) } }
            AssistLevel.GO -> { tones?.startTone(ToneGenerator.TONE_PROP_ACK, 400); session?.voice?.speak("Ça avance devant") }
            else -> Unit
        }
        Unit
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (granted && detector != null) {
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
                                val result = detector.detectForVideo(BitmapImageBuilder(bmp).build(), SystemClock.uptimeMillis())
                                val focal = AssistMath.focalPx(max(bmp.width, bmp.height), fov.toDouble())
                                // Véhicule de devant : le plus proche dont le centre est dans le tiers central de l'image.
                                val all = result.detections().map { d ->
                                    val label = d.categories().firstOrNull()?.categoryName() ?: "car"
                                    val b = d.boundingBox()
                                    Triple(b, label, AssistMath.distance(label, b.width(), focal))
                                }
                                val lead = all.filter { (b, label, _) -> label != "person" && abs(b.centerX() / bmp.width - 0.5f) < 0.17f && b.bottom > bmp.height * 0.4f }
                                    .minByOrNull { it.third }
                                val kmh = (session?.fix?.kmh ?: 0).toDouble()
                                val lv = math.update(lead?.third ?: Double.NaN, kmh, SystemClock.elapsedRealtime())
                                view.post {
                                    imageSize = Size(bmp.width.toFloat(), bmp.height.toFloat())
                                    seen = all.map { t -> Seen(t.first, t.second, t.third, t === lead) }
                                    leadDistance = math.smoothed; headway = math.headway(kmh)
                                    if (lv != level) level = lv
                                }
                            } catch (_: Throwable) {
                            } finally { proxy.close() }
                        }
                        provider.unbindAll()
                        provider.bindToLifecycle(lifecycle, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                    }.onFailure { error = "Caméra indisponible : ${it.message}" }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            }, modifier = Modifier.fillMaxSize())

            // Cadres autour des objets détectés (même mise à l'échelle que l'aperçu « FILL_CENTER »).
            Canvas(Modifier.fillMaxSize()) {
                val scale = max(size.width / imageSize.width, size.height / imageSize.height)
                val dx = (size.width - imageSize.width * scale) / 2
                val dy = (size.height - imageSize.height * scale) / 2
                seen.forEach { o ->
                    val c = if (o.lead) Color(level.color) else Color(0xAAFFFFFF)
                    drawRect(c, topLeft = Offset(o.box.left * scale + dx, o.box.top * scale + dy),
                        size = Size(o.box.width() * scale, o.box.height() * scale), style = Stroke(width = if (o.lead) 8f else 3f))
                }
            }
        }

        // En-tête : fermer, étalonnage.
        Row(Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(10.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            BigButton("✕ Fermer", Color(0xCC000000), onClose)
            Text("Étalonnage", color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
            BigButton("−", Color(0xCC000000)) { fov = (fov + 3f).coerceAtMost(90f); prefs.edit().putFloat("fov", fov).apply() }
            BigButton("+", Color(0xCC000000)) { fov = (fov - 3f).coerceAtLeast(40f); prefs.edit().putFloat("fov", fov).apply() }
        }

        // Bas : distance, temps de sécurité, état.
        Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(12.dp).fillMaxWidth()
            .clip(RoundedCornerShape(22.dp)).background(Color(level.color).copy(alpha = 0.88f)).padding(16.dp)) {
            val msg = error ?: if (!granted) "Autorise la caméra pour utiliser l'assistant" else level.text
            Text(msg, color = Color.White, fontSize = if (level == AssistLevel.DANGER) 34.sp else 22.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Text(if (leadDistance.isNaN()) "— m" else "${leadDistance.toInt()} m", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Black)
                Column {
                    Text(if (headway.isNaN()) "Temps de sécurité —" else "Temps de sécurité %.1f s".format(java.util.Locale.FRANCE, headway),
                        color = Color.White, fontSize = 17.sp)
                    Text("Vitesse ${session?.fix?.kmh ?: 0} km/h · règle des 2 secondes", color = Color(0xDDFFFFFF), fontSize = 14.sp)
                }
            }
            Text("Fixe le téléphone au pare-brise, caméra vers la route. Distance estimée : aide à la conduite, ne remplace pas ta vigilance.",
                color = Color(0xCCFFFFFF), fontSize = 12.sp)
        }
    }
}
