package fr.roadguard.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import fr.roadguard.core.radar.PersonalRadars
import fr.roadguard.core.radar.RadarCountries
import fr.roadguard.core.radar.RadarKind
import kotlinx.coroutines.launch

/** « Cartes et radars hors ligne » : packs de cartes à télécharger et radars des autres pays. */
@Composable
fun OfflineDataScreen(nearCountries: Set<String>, onClose: () -> Unit, here: Pair<Double, Double>? = null, startTab: Int = 0) {
    val context = LocalContext.current
    val manager = remember { MapPackManager.get(context) }
    val states by manager.states.collectAsState()
    var tab by remember { mutableStateOf(startTab) }
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
            BigButton("Mes radars", if (tab == 2) DriveColors.Accent else DriveColors.Panel) { tab = 2 }
        }
        if (tab == 2) { PersonalRadarsTab(here); return@Column }
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
                item { RegionsCard(here) }
                item { Text("Grands packs (projet extérieur)", color = DriveColors.TextSecondary, fontSize = 15.sp, modifier = Modifier.padding(top = 6.dp)) }
                // Packs publiés cassés à la source : masqués plutôt que proposés en erreur.
                items(sorted.filter { it.unavailable == null }, key = { it.id }) { pack -> PackRow(pack, states[pack.id] ?: PackState.NotInstalled, manager) }
            }
        } else {
            Text(
                "France, Suisse, Belgique, Allemagne, Italie, Espagne et Luxembourg sont déjà inclus. Autres pays : base " +
                    "communautaire MapAtlas (CC BY 4.0), à vérifier sur place. Respecte la loi du pays visité.",
                color = DriveColors.TextSecondary, fontSize = 15.sp,
            )
            LazyColumn(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Pays déjà dans l'appli : rien à télécharger, toujours à jour avec l'appli.
                items(listOf("France", "Suisse", "Belgique", "Allemagne", "Italie", "Espagne", "Luxembourg"), key = { "in-$it" }) { name ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(DriveColors.Panel).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("Inclus dans l'appli · mis à jour avec elle", color = DriveColors.TextSecondary, fontSize = 14.sp)
                        }
                        Text("✓", color = Color(0xFF22C55E), fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    }
                }
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

/** Radars perso : ajout à la position actuelle, import d'un fichier CSV, liste avec suppression. */
@Composable
private fun PersonalRadarsTab(here: Pair<Double, Double>?) {
    val context = LocalContext.current
    var refresh by remember { mutableStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }
    var kind by remember { mutableStateOf(RadarKind.FIXED) }
    var speed by remember { mutableStateOf<Int?>(50) }
    val radars = remember(refresh) { PersonalRadars.list(context) }
    val scope = rememberCoroutineScope()
    val picker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            message = "Import en cours…"
            message = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching {
                    val bytes = context.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
                        .also { if (it.size > 40_000_000) throw java.io.IOException("fichier trop gros") }
                    val name = runCatching {
                        context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
                            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
                    }.getOrNull() ?: uri.lastPathSegment.orEmpty()
                    val (added, skipped) = PersonalRadars.importFile(context, name, bytes)
                    "$added radar(s) importé(s)" + if (skipped > 0) ", $skipped ligne(s) ignorée(s) (doublons ou illisibles)" else ""
                }.getOrElse { "Import impossible : ${it.message}" }
            }
            refresh++
        }
    }
    LazyColumn(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(DriveColors.Panel).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Ajouter un radar à ma position", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(RadarKind.FIXED, RadarKind.REDLIGHT, RadarKind.AVERAGE, RadarKind.MOBILE).forEach { k ->
                        BigButton(fr.roadguard.core.radar.AlertText.kindLabel(k), if (kind == k) DriveColors.Accent else DriveColors.Track) { kind = k }
                    }
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(null, 30, 50, 70, 80, 90, 110, 130).forEach { v ->
                        BigButton(v?.toString() ?: "?", if (speed == v) DriveColors.Accent else DriveColors.Track) { speed = v }
                    }
                }
                BigButton(if (here == null) "En attente du GPS…" else "＋ Ajouter ici", if (here == null) DriveColors.Track else DriveColors.Accent) {
                    val h = here ?: return@BigButton
                    PersonalRadars.add(context, h.first, h.second, kind, speed, null)
                    message = "Radar ajouté"; refresh++
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                BigButton("📄 Importer un fichier CSV", DriveColors.Accent) {
                    runCatching { picker.launch(arrayOf("text/*", "application/csv", "application/vnd.ms-excel", "application/zip", "application/octet-stream")) }
                        .onFailure { message = "Aucun gestionnaire de fichiers sur cet appareil" }
                }
                if (radars.isNotEmpty()) BigButton("Tout effacer", Color(0xFF7F1D1D)) { PersonalRadars.clear(context); refresh++ }
            }
        }
        item {
            Text(message ?: "Fichiers acceptés : CSV (latitude, longitude, vitesse), ASC Lufop, archives ZIP, " +
                "en-têtes lat/lon/vitesse/type, ou format iGO (X,Y,TYPE,SPEED…). ${radars.size} radar(s) perso.",
                color = DriveColors.TextSecondary, fontSize = 15.sp)
        }
        items(radars.asReversed().take(300), key = { it.id }) { r ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(DriveColors.Panel).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(r.label + (r.speedKmh?.let { " · $it km/h" } ?: ""), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("%.5f, %.5f".format(java.util.Locale.US, r.lat, r.lon), color = DriveColors.TextSecondary, fontSize = 14.sp)
                }
                BigButton("Supprimer", Color(0xFF7F1D1D)) { PersonalRadars.remove(context, r.id); refresh++ }
            }
        }
    }
}

/** « Ma région » : zone enregistrée depuis la carte en ligne (aucun fichier à héberger). */
@Composable
private fun RegionsCard(here: Pair<Double, Double>?) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var regions by remember { mutableStateOf<List<fr.roadguard.core.map.SavedRegion>>(emptyList()) }
    var refresh by remember { mutableStateOf(0) }
    var job by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    val progress by fr.roadguard.core.map.OfflineRegions.progress.collectAsState()
    androidx.compose.runtime.LaunchedEffect(refresh) { regions = runCatching { fr.roadguard.core.map.OfflineRegions.list(context) }.getOrDefault(emptyList()) }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(DriveColors.Panel).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("📍 Ma région", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Enregistre la carte sur environ 80 km autour de toi, pour l'avoir sans réseau. " +
            "Tout ce que tu as déjà vu en ligne reste aussi en mémoire.", color = DriveColors.TextSecondary, fontSize = 14.sp)
        val p = progress
        if (job != null && p != null) {
            Text(if (p.second > 0) "Téléchargement… ${p.first} / ${p.second} éléments" else "Préparation…", color = Color.White, fontSize = 15.sp)
            LinearProgressIndicator(
                progress = { if (p.second > 0) (p.first.toFloat() / p.second).coerceIn(0f, 1f) else 0f },
                modifier = Modifier.fillMaxWidth().height(6.dp), color = DriveColors.Accent, trackColor = DriveColors.Track,
            )
            BigButton("Arrêter", DriveColors.Track) { job?.cancel(); job = null; refresh++ }
        } else {
            BigButton(if (here == null) "En attente du GPS…" else "Télécharger ma région", if (here == null) DriveColors.Track else DriveColors.Accent) {
                val h = here ?: return@BigButton
                message = null
                job = scope.launch {
                    runCatching {
                        fr.roadguard.core.map.OfflineRegions.download(context, fr.roadguard.core.map.STYLE_URL, h.first, h.second, 80.0,
                            "Région autour de %.2f, %.2f".format(java.util.Locale.FRANCE, h.first, h.second))
                    }.onSuccess { message = "Région enregistrée ✓" }
                        .onFailure { if (it !is kotlinx.coroutines.CancellationException) message = "Échec : ${it.message}" }
                    job = null; refresh++
                }
            }
        }
        message?.let { Text(it, color = DriveColors.TextSecondary, fontSize = 14.sp) }
        BigButton("Vider la mémoire de la carte", DriveColors.Track) {
            scope.launch { fr.roadguard.core.map.OfflineRegions.clearCache(context); message = "Mémoire de la carte vidée : elle se recharge." }
        }
        regions.forEach { r ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(r.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text((if (r.done) "Complète" else "Incomplète") + " · ${r.bytes / 1_000_000} Mo", color = DriveColors.TextSecondary, fontSize = 13.sp)
                }
                BigButton("Supprimer", Color(0xFF7F1D1D)) { scope.launch { fr.roadguard.core.map.OfflineRegions.delete(context, r.id); refresh++ } }
            }
        }
    }
}
