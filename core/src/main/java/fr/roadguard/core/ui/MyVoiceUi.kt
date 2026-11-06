package fr.roadguard.core.ui

import android.media.MediaPlayer
import android.media.MediaRecorder
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import fr.roadguard.core.radar.AlertVoice
import java.io.File

/**
 * « Ma voix » : enregistre soi-même (ou fait enregistrer) chaque phrase des annonces.
 * Les phrases sont courtes et assemblées à la suite : « Attention, radar devant ! » + « Radar fixe » + « limité à » + « 80 ».
 * Une phrase non enregistrée est dite par la voix du téléphone.
 */
@Composable
fun MyVoiceScreen(voice: AlertVoice, onClose: () -> Unit) {
    val context = LocalContext.current
    val pack = voice.pack
    val items = remember { pack.recordingList() }
    val recordingKey = remember { mutableStateOf<String?>(null) }
    val tick = remember { mutableStateOf(0) }
    val recorder = remember { mutableStateOf<MediaRecorder?>(null) }
    val player = remember { mutableStateOf<MediaPlayer?>(null) }
    val micOk = remember {
        mutableStateOf(context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED)
    }
    val askMic = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { micOk.value = it }

    fun stop() {
        val r = recorder.value ?: return
        val key = recordingKey.value
        runCatching { r.stop() }; r.release(); recorder.value = null; recordingKey.value = null
        if (key != null) {
            val tmp = File(pack.myDir, "$key.tmp")
            if (tmp.length() > 2_000) tmp.renameTo(pack.myFile(key)) else tmp.delete()
        }
        tick.value++
    }

    fun start(key: String) {
        if (!micOk.value) { askMic.launch(android.Manifest.permission.RECORD_AUDIO); return }
        stop()
        val tmp = File(pack.myDir, "$key.tmp")
        @Suppress("DEPRECATION")
        val r = if (android.os.Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else MediaRecorder()
        val ok = runCatching {
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioSamplingRate(44_100); r.setAudioEncodingBitRate(96_000)
            r.setOutputFile(tmp.absolutePath)
            r.prepare(); r.start()
        }.isSuccess
        if (ok) { recorder.value = r; recordingKey.value = key } else r.release()
    }

    fun play(file: File) {
        runCatching { player.value?.release() }
        player.value = runCatching { MediaPlayer().apply { setDataSource(file.absolutePath); prepare(); start() } }.getOrNull()
    }

    DisposableEffect(Unit) { onDispose { stop(); runCatching { player.value?.release() } } }

    Dialog(onDismissRequest = { stop(); onClose() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxSize().background(DriveColors.Background).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🎙 Ma voix", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                BigButton("Fermer", DriveColors.Panel) { stop(); onClose() }
            }
            Text("Appuie sur ● et dis la phrase, puis ■ pour arrêter. ▶ pour réécouter. " +
                "Ensuite, choisis le ton « Ma voix 🎙 » dans Réglages → Voix. " +
                "Les phrases sont assemblées : « Attention, radar devant ! » + « Radar fixe » + « limité à » + « 80 ».",
                color = DriveColors.TextSecondary, fontSize = 15.sp)
            val done = remember(tick.value) { items.count { pack.myFile(it.first).length() > 0 } }
            Text("$done / ${items.size} phrases enregistrées", color = DriveColors.Accent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items, key = { it.first }) { (key, text) ->
                    val file = pack.myFile(key)
                    val has = remember(tick.value) { file.length() > 0 }
                    val rec = recordingKey.value == key
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                            .background(if (rec) Color(0xFF5A1620) else DriveColors.Panel).padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(if (has) "✅" else "⬜", fontSize = 20.sp)
                        Text("« $text »", color = Color.White, fontSize = 17.sp, modifier = Modifier.weight(1f))
                        if (rec) BigButton("■ Stop", Color(0xFFD63031)) { stop() }
                        else BigButton("● Enregistrer", DriveColors.Accent) { start(key) }
                        if (has && !rec) {
                            BigButton("▶", DriveColors.Track) { play(file) }
                            BigButton("🗑", DriveColors.Track) { file.delete(); tick.value++ }
                        }
                    }
                }
            }
        }
    }
}
