package fr.roadguard.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import fr.roadguard.core.map.CatalogPack
import fr.roadguard.core.map.MapPackManager
import fr.roadguard.core.map.PackState
import fr.roadguard.core.radar.RadarCountries
import kotlinx.coroutines.launch

/** « Cartes et radars hors ligne » : packs de cartes à télécharger et radars des autres pays. */
@Composable
fun OfflineDataScreen(nearCountries: Set<String>, onClose: () -> Unit) {
    val context = LocalContext.current
    val manager = remember { MapPackManager.get(context) }
    val states by manager.states.collectAsState()
    var tab by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    val radarStatus = remember { mutableStateMapOf<String, String>() }
    var radarRefresh by remember { mutableStateOf(0) }

    Column(Modifier.fillMaxSize().background(DriveColors.Background).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Hors ligne", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            BigButton("Fermer", DriveColors.Panel, onClose)
        }
        Row(Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BigButton("Cartes", if (tab == 0) DriveColors.Accent else DriveColors.Panel) { tab = 0 }
            BigButton("Radars par pays", if (tab == 1) DriveColors.Accent else DriveColors.Panel) { tab = 1 }
        }
        if (tab == 0) {
            Text(
                "Installé : ${manager.installedBytes() / 1_000_000} Mo · libre : ${manager.freeBytes() / 1_000_000} Mo. " +
                    "Sans Internet, la carte s'affiche depuis ces packs (l'itinéraire demande encore Internet).",
                color = DriveColors.TextSecondary, fontSize = 15.sp,
            )
            // Packs installés ou en cours d'abord, puis ceux des pays proches, puis le reste.
            val sorted = manager.catalog.sortedWith(compareBy<CatalogPack>(
                { states[it.id] !is PackState.Installed && states[it.id] !is PackState.Downloading && states[it.id] !is PackState.Paused },
                { it.country !in nearCountries },
                { it.name },
            ))
            LazyColumn(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sorted, key = { it.id }) { pack -> PackRow(pack, states[pack.id] ?: PackState.NotInstalled, manager) }
            }
        } else {
            Text(
                "France, Suisse, Belgique, Allemagne, Italie, Espagne et Luxembourg sont déjà inclus. Autres pays : base " +
                    "communautaire MapAtlas (CC BY 4.0), à vérifier sur place. Respecte la loi du pays visité.",
                color = DriveColors.TextSecondary, fontSize = 15.sp,
            )
            LazyColumn(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(RadarCountries.available, key = { it.first }) { (code, label) ->
                    val installed = remember(radarRefresh) { RadarCountries.installed(context, code) }
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(DriveColors.Panel).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(label, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(radarStatus[code] ?: if (installed) "Installé" else "Non installé",
                                color = DriveColors.TextSecondary, fontSize = 14.sp)
                        }
                        if (installed) {
                            BigButton("Supprimer", Color(0xFF7F1D1D)) {
                                RadarCountries.remove(context, code); radarStatus.remove(code); radarRefresh++
                            }
                        } else {
                            BigButton("Télécharger", DriveColors.Accent) {
                                radarStatus[code] = "Téléchargement…"
                                scope.launch {
                                    runCatching { RadarCountries.download(context, code) { d -> radarStatus[code] = "Téléchargement… ${d / 1000} Ko" } }
                                        .onSuccess { radarStatus[code] = "Installé : $it radars"; radarRefresh++ }
                                        .onFailure { radarStatus[code] = "Échec : ${it.message}" }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PackRow(pack: CatalogPack, state: PackState, manager: MapPackManager) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(DriveColors.Panel).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(pack.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                val info = when {
                    pack.unavailable != null -> pack.unavailable
                    state is PackState.Installed -> "Installé · ${pack.size / 1_000_000} Mo"
                    state is PackState.Downloading -> "${state.done / 1_000_000} / ${pack.size / 1_000_000} Mo"
                    state is PackState.Paused -> "En pause · ${state.done / 1_000_000} / ${pack.size / 1_000_000} Mo"
                    state is PackState.Failed -> "Échec : ${state.message}"
                    else -> "${pack.size / 1_000_000} Mo"
                }
                Text(info, color = if (state is PackState.Failed || pack.unavailable != null) Color(0xFFFF8A80) else DriveColors.TextSecondary, fontSize = 14.sp)
            }
            when {
                pack.unavailable != null -> Unit
                state is PackState.Installed -> BigButton("Supprimer", Color(0xFF7F1D1D)) { manager.delete(pack) }
                state is PackState.Downloading -> BigButton("Pause", DriveColors.Track) { manager.pause(pack) }
                state is PackState.Paused -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BigButton("Reprendre", DriveColors.Accent) { manager.download(pack) }
                    BigButton("✕", Color(0xFF7F1D1D)) { manager.delete(pack) }
                }
                else -> BigButton(if (state is PackState.Failed) "Réessayer" else "Télécharger", DriveColors.Accent) { manager.download(pack) }
            }
        }
        if (state is PackState.Downloading && state.total > 0) {
            Box(Modifier.padding(top = 8.dp)) {
                LinearProgressIndicator(
                    progress = { (state.done.toFloat() / state.total).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = DriveColors.Accent, trackColor = DriveColors.Track,
                )
            }
        }
    }
}
