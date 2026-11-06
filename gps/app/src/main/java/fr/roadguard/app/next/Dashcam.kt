package fr.roadguard.app.next

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import fr.roadguard.core.DrivingSession
import java.io.File

/**
 * Dashcam : la caméra arrière filme en boucle par séquences d'une minute (les 3 dernières sont gardées).
 * Choc détecté, ou appui sur « Garder » : les séquences en cours sont copiées dans la galerie (Films/Pilote).
 * Sans son ; fonctionne tant que Pilote est à l'écran.
 */
object DashcamStore {
    val saved = mutableIntStateOf(0)
    fun dir(context: Context) = File(context.getExternalFilesDir(null) ?: context.filesDir, "dashcam").apply { mkdirs() }

    /** Copie les dernières séquences dans la galerie. Renvoie le nombre de vidéos gardées. */
    fun keep(context: Context): Int {
        val files = dir(context).listFiles { f -> f.name.endsWith(".mp4") && f.length() > 50_000 }?.sortedBy { it.lastModified() }?.takeLast(2) ?: return 0
        var n = 0
        files.forEach { f ->
            runCatching {
                if (Build.VERSION.SDK_INT >= 29) {
                    val v = ContentValues().apply {
                        put(MediaStore.Video.Media.DISPLAY_NAME, "Pilote-${f.nameWithoutExtension}.mp4")
                        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                        put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Pilote")
                    }
                    val uri = context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, v) ?: return@runCatching
                    context.contentResolver.openOutputStream(uri)?.use { out -> f.inputStream().use { it.copyTo(out) } }
                } else {
                    f.copyTo(File(dir(context), "garde-${f.name}"), overwrite = true)
                }
                n++
            }
        }
        saved.intValue += n
        return n
    }
}

@Composable
fun rememberDashcam(session: DrivingSession?, enabled: Boolean) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    if (!enabled || context.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) return
    val impacts = session?.impacts ?: 0
    val lastSeen = remember { intArrayOf(impacts) }
    // Choc : on garde la vidéo une fois la séquence en cours terminée (quelques secondes après).
    LaunchedEffect(impacts) {
        if (impacts > lastSeen[0]) {
            lastSeen[0] = impacts
            kotlinx.coroutines.delay(20_000)
            val n = DashcamStore.keep(context)
            if (n > 0) session?.voice?.speak("Dashcam : vidéo du choc enregistrée dans la galerie.")
        }
    }
    DisposableEffect(lifecycle) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        var recording: Recording? = null
        var capture: VideoCapture<Recorder>? = null
        var running = true
        val main = android.os.Handler(android.os.Looper.getMainLooper())
        fun cleanup() {
            val files = DashcamStore.dir(context).listFiles { f -> f.name.startsWith("seg-") }?.sortedBy { it.lastModified() } ?: return
            files.dropLast(3).forEach { it.delete() }
        }
        fun startSegment() {
            val c = capture ?: return
            if (!running) return
            cleanup()
            val file = File(DashcamStore.dir(context), "seg-${java.text.SimpleDateFormat("yyyyMMdd-HHmmss", java.util.Locale.FRANCE).format(java.util.Date())}.mp4")
            recording = runCatching {
                c.output.prepareRecording(context, FileOutputOptions.Builder(file).build())
                    .start(ContextCompat.getMainExecutor(context)) { e ->
                        if (e is VideoRecordEvent.Finalize && running) main.postDelayed({ startSegment() }, 300)
                    }
            }.getOrNull()
            main.postDelayed({ recording?.stop() }, 60_000)
        }
        providerFuture.addListener({
            runCatching {
                val provider = providerFuture.get()
                val recorder = Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.HD)).build()
                val vc = VideoCapture.withOutput(recorder)
                provider.bindToLifecycle(lifecycle, CameraSelector.DEFAULT_BACK_CAMERA, vc)
                capture = vc
                startSegment()
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            running = false
            main.removeCallbacksAndMessages(null)
            runCatching { recording?.stop() }
            runCatching { capture?.let { providerFuture.get().unbind(it) } }
        }
    }
}
