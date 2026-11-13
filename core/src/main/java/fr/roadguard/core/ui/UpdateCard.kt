package fr.roadguard.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.roadguard.core.update.AppRelease
import fr.roadguard.core.update.AppUpdater
import kotlinx.coroutines.launch

/**
 * Téléchargement en cours, bien visible : « En cours de téléchargement… », pourcentage, grosse barre, Mo reçus.
 * L'écran reste allumé pendant le téléchargement.
 */
@Composable
fun DownloadProgress(fraction: Float, done: Long, total: Long, modifier: Modifier = Modifier) {
    val view = androidx.compose.ui.platform.LocalView.current
    androidx.compose.runtime.DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
    val f = fraction.coerceIn(0f, 1f)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(if (f >= 0.999f) "🔎 Vérification, puis installation…" else "⬇ En cours de téléchargement…",
                color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("${(f * 100).toInt()} %", color = DriveColors.Accent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(progress = { f }, modifier = Modifier.fillMaxWidth().height(16.dp).clip(RoundedCornerShape(8.dp)),
            color = DriveColors.Accent, trackColor = DriveColors.Track)
        Text(
            if (f >= 0.999f) "Confirme ensuite dans la fenêtre d'Android (« Installer » ou « Mettre à jour »)."
            else String.format(java.util.Locale.FRANCE, "%.1f Mo sur %.0f Mo · ne ferme pas l'appli", done / 1_048_576.0, total / 1_048_576.0),
            color = Color(0xFFC7D2DE), fontSize = 14.sp,
        )
    }
}

/** « Mise à jour » dans les réglages : vérifier, télécharger, installer. */
@Composable
fun UpdateCard(app: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<String?>(null) }
    var release by remember { mutableStateOf<AppRelease?>(null) }
    var progress by remember { mutableStateOf<Float?>(null) }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(DriveColors.Panel).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Mise à jour de l'appli", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        val r = release
        when {
            progress != null -> DownloadProgress(progress ?: 0f, ((progress ?: 0f) * (r?.size ?: 0L)).toLong(), r?.size ?: 0L)
            r != null -> BigButton("Installer la version ${r.build} (${r.size / 1_000_000} Mo)", DriveColors.Accent) {
                progress = 0f
                scope.launch {
                    runCatching { AppUpdater.downloadAndInstall(context, r) { d, t -> progress = if (t > 0) d.toFloat() / t else 0f } }
                        .onSuccess { status = "Confirme l'installation dans la fenêtre Android." }
                        .onFailure { status = "Échec : ${it.message}" }
                    progress = null
                }
            }
            else -> BigButton("Rechercher une mise à jour", DriveColors.Track) {
                status = "Recherche…"
                scope.launch {
                    runCatching { AppUpdater.check(context, app) }
                        .onSuccess { release = it; status = if (it == null) "L'appli est à jour ✓" else "Nouvelle version disponible" }
                        .onFailure { status = "Recherche impossible : ${it.message}" }
                }
            }
        }
        status?.let { Text(it, color = DriveColors.TextSecondary, fontSize = 14.sp) }
    }
}
