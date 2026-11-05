package fr.roadguard.core.ui

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.roadguard.core.update.AppRelease
import fr.roadguard.core.update.AppUpdater
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Prévenir des nouvelles versions : bandeau dans l'appli et notification Android (une fois par version). */
object UpdateNotifier {
    private const val CHANNEL = "pilote_updates"

    fun appKey(context: Context) = when {
        context.packageName.endsWith(".system") -> "drivesys"
        context.packageName.contains("drive") -> "drive"
        else -> "gps"
    }
    fun appName(context: Context) = when {
        context.packageName.endsWith(".system") -> "Pilote Drive Système"
        context.packageName.contains("drive") -> "Pilote Drive"
        else -> "Pilote"
    }

    /** Vérifie le serveur ; notifie si une version plus récente existe et n'a pas encore été signalée. */
    suspend fun checkAndNotify(context: Context): AppRelease? {
        val rel = runCatching { AppUpdater.check(context, appKey(context)) }.getOrNull() ?: return null
        val prefs = context.getSharedPreferences("updates", Context.MODE_PRIVATE)
        if (prefs.getInt("notified_build", 0) != rel.build) {
            prefs.edit().putInt("notified_build", rel.build).apply()
            notify(context, rel)
        }
        return rel
    }

    private fun notify(context: Context, rel: AppRelease) = runCatching {
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return@runCatching
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Mises à jour", NotificationManager.IMPORTANCE_DEFAULT))
        val open = context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
            PendingIntent.getActivity(context, 7, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val n = Notification.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Nouvelle version de ${appName(context)}")
            .setContentText("Version ${rel.build} disponible : touche pour l'installer.")
            .setAutoCancel(true).apply { open?.let { setContentIntent(it) } }.build()
        nm.notify(4207, n)
    }
}

/** Bandeau « nouvelle version » en bas de l'écran, avec installation directe. Vérifie toutes les 3 h. */
@Composable
fun BoxScope.UpdateBanner() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var release by remember { mutableStateOf<AppRelease?>(null) }
    var dismissed by remember { mutableStateOf(0) }
    var progress by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        delay(8_000) // laisser l'appli démarrer
        while (true) { release = UpdateNotifier.checkAndNotify(context); delay(3 * 3_600_000L) }
    }
    val r = release ?: return
    if (dismissed == r.build) return
    Row(
        Modifier.align(Alignment.BottomCenter).padding(12.dp).widthIn(max = 620.dp).clip(RoundedCornerShape(18.dp))
            .background(Color(0xF0123A6B)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text("🆕 Nouvelle version de ${UpdateNotifier.appName(context)}", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(progress ?: "Version ${r.build} disponible (${r.size / 1_048_576} Mo).", color = Color(0xFFCFE3FF), fontSize = 14.sp)
        }
        if (progress == null) {
            BigButton("Installer", DriveColors.Accent) {
                progress = "Téléchargement…"
                scope.launch {
                    runCatching {
                        AppUpdater.downloadAndInstall(context, r) { done, total -> progress = "Téléchargement ${if (total > 0) done * 100 / total else 0} %" }
                    }.onFailure { progress = "Échec : ${it.message}" }
                }
            }
            BigButton("Plus tard", Color(0x33FFFFFF)) { dismissed = r.build }
        }
    }
}
