package fr.roadguard.core.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import fr.roadguard.core.community.Community
import fr.roadguard.core.community.ReportType
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Modération (administrateur) : signalements à valider (nouveaux, peu fiables, suspects) et comptes de la communauté,
 * avec « de confiance » et « bloquer ».
 */
@Composable
fun ModerationScreen(community: Community, onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf(0) }
    var pending by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var accounts by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var info by remember { mutableStateOf<String?>("Chargement…") }
    suspend fun load() {
        runCatching { community.modLink() }
        runCatching { if (tab == 0) pending = community.modPending() else accounts = community.modAccounts() }
            .onSuccess { info = null }
            .onFailure { info = "Impossible de charger : ${it.message}. Ouvre Communauté une fois pour relier ton compte." }
    }
    LaunchedEffect(tab) { load() }
    val df = java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.FRANCE)
    FullScreenDialog(onDismissRequest = onClose) {
        Column(Modifier.fillMaxSize().background(DriveColors.Background).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🛡 Communauté", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                BigButton("Fermer", DriveColors.Panel, onClose)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BigButton("À valider", if (tab == 0) DriveColors.Accent else DriveColors.Panel) { tab = 0 }
                BigButton("Comptes", if (tab == 1) DriveColors.Accent else DriveColors.Panel) { tab = 1 }
            }
            info?.let { Text(it, color = DriveColors.TextSecondary, fontSize = 15.sp) }
            if (tab == 0) LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (pending.isEmpty() && info == null) item { Text("Rien à valider. 👍", color = DriveColors.TextSecondary, fontSize = 16.sp) }
                items(pending, key = { it.optString("id") }) { r ->
                    val type = ReportType.of(r.optString("type"))
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(DriveColors.Panel).padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${type?.emoji ?: "⚠"} ${type?.label ?: r.optString("type")} · ${df.format(java.util.Date(r.optLong("created_at")))}",
                            color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        val stars = if (r.isNull("author_stars")) null else r.optDouble("author_stars")
                        Text("Par ${r.optString("author")}${trustBadge(stars, r.optInt("trusted") == 1)} · ${levelLabel(r.optString("author_level"))}",
                            color = DriveColors.TextSecondary, fontSize = 14.sp)
                        Text("État : ${when (r.optString("status")) { "pending" -> "à confirmer"; "hidden" -> "masqué (auteur peu fiable ou suspect)"; else -> "visible, pas encore confirmé" }} · " +
                            "👍 ${r.optInt("yes")} · 👎 ${r.optInt("no")}", color = DriveColors.TextSecondary, fontSize = 14.sp)
                        if (!r.isNull("text") && r.optString("text").isNotBlank()) Text("« ${r.optString("text")} »", color = Color.White, fontSize = 15.sp)
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            BigButton("🗺 Voir", DriveColors.Track) {
                                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:${r.optDouble("lat")},${r.optDouble("lon")}?q=${r.optDouble("lat")},${r.optDouble("lon")}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                            }
                            BigButton("✅ Valider", Color(0xFF2E7D32)) {
                                scope.launch { runCatching { community.modReport(r.optString("id"), true) }; load() }
                            }
                            BigButton("❌ Refuser", Color(0xFF7F1D1D)) {
                                scope.launch { runCatching { community.modReport(r.optString("id"), false) }; load() }
                            }
                        }
                    }
                }
            } else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(accounts, key = { it.optString("id") }) { a ->
                    val id = a.optString("id")
                    val stars = if (a.isNull("stars")) null else a.optDouble("stars")
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(DriveColors.Panel).padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${a.optString("name")}${trustBadge(stars, a.optInt("trusted") == 1)}" +
                            (a.optString("owner").takeIf { it.isNotBlank() && it != "null" }?.let { " · compte Pilote : $it" } ?: ""),
                            color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("${levelLabel(a.optString("level"))} · ${a.optInt("reports")} signalement(s) · ✔ ${a.optInt("good")} · ✖ ${a.optInt("bad")} · ${a.optInt("points")} pts",
                            color = DriveColors.TextSecondary, fontSize = 14.sp)
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            fun act(action: String) = scope.launch { runCatching { community.modAccount(id, action) }; load() }
                            if (a.optInt("trusted") == 1) BigButton("Retirer la confiance", DriveColors.Track) { act("untrust") }
                            else BigButton("👑 De confiance", Color(0xFF1565C0)) { act("trust") }
                            if (a.optString("level") == "suspect") BigButton("Lever le soupçon", DriveColors.Track) { act("clear") }
                            if (a.optInt("banned") == 1) BigButton("Débloquer", DriveColors.Track) { act("unban") }
                            else BigButton("🚫 Bloquer", Color(0xFF7F1D1D)) { act("ban") }
                        }
                    }
                }
            }
        }
    }
}
