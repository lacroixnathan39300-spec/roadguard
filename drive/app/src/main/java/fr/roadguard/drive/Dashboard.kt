package fr.roadguard.drive

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.roadguard.core.DrivingSession
import fr.roadguard.core.TripComputer
import fr.roadguard.core.media.NowPlaying
import fr.roadguard.core.nav.Guidance
import fr.roadguard.core.radar.AlertText
import fr.roadguard.core.ui.BigButton
import fr.roadguard.core.ui.DriveColors
import fr.roadguard.core.ui.Speedometer
import fr.roadguard.core.ui.maneuverSymbol
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Blocs disponibles sur la page « Tableau de bord ». */
enum class DashWidget(val label: String) {
    SPEED("Compteur"), CLOCK("Horloge et date"), TRIP("Trajet en cours"), TODAY("Aujourd'hui"),
    WEATHER("Météo et adresse"), MUSIC("Musique"), RADAR("Prochain radar"), GUIDANCE("Prochaine direction"), ALTITUDE("Altitude"),
}

data class DashItem(val widget: DashWidget, val wide: Boolean)

/** Composition du tableau de bord, mémorisée sur le poste. */
object DashboardConfig {
    private val default = listOf(
        DashItem(DashWidget.SPEED, true), DashItem(DashWidget.CLOCK, false), DashItem(DashWidget.WEATHER, false),
        DashItem(DashWidget.TRIP, false), DashItem(DashWidget.RADAR, false), DashItem(DashWidget.MUSIC, true),
        DashItem(DashWidget.TODAY, false), DashItem(DashWidget.ALTITUDE, false),
    )

    fun load(c: Context): List<DashItem> {
        val raw = c.getSharedPreferences("settings", Context.MODE_PRIVATE).getString("dashboard", null) ?: return default
        return raw.split(',').mapNotNull { e ->
            val (n, w) = e.split(':').let { it[0] to (it.getOrNull(1) == "2") }
            runCatching { DashItem(DashWidget.valueOf(n), w) }.getOrNull()
        }.ifEmpty { default }
    }

    fun save(c: Context, items: List<DashItem>) {
        c.getSharedPreferences("settings", Context.MODE_PRIVATE).edit()
            .putString("dashboard", items.joinToString(",") { "${it.widget.name}:${if (it.wide) 2 else 1}" }).apply()
    }
}

/** Regroupe les blocs en lignes de 3 colonnes (un bloc large prend 2 colonnes). */
fun packRows(items: List<DashItem>): List<List<DashItem>> {
    val rows = mutableListOf<MutableList<DashItem>>()
    var used = 3
    for (i in items) {
        val span = if (i.wide) 2 else 1
        if (used + span > 3) { rows += mutableListOf<DashItem>(); used = 0 }
        rows.last() += i; used += span
    }
    return rows
}

/** Page « Tableau de bord » : grands blocs lisibles, au choix et dans l'ordre voulu. */
@Composable
fun DashboardPage(
    session: DrivingSession, music: NowPlaying?, onPlayPause: () -> Unit, onNext: () -> Unit, onPrevious: () -> Unit,
    onMap: () -> Unit, onTrip: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var items by remember { mutableStateOf(DashboardConfig.load(context)) }
    var editing by remember { mutableStateOf(false) }
    if (editing) {
        DashboardEditor(items, onChange = { items = it; DashboardConfig.save(context, it) }, onClose = { editing = false })
        return
    }
    Column(Modifier.fillMaxSize().background(DriveColors.Background).padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BigButton("◂ Carte", DriveColors.Accent, onMap)
            Spacer(Modifier.weight(1f))
            BigButton("✎ Modifier", DriveColors.Panel) { editing = true }
        }
        val rows = packRows(items)
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { item ->
                    Box(Modifier.weight(if (item.wide) 2f else 1f).fillMaxHeight().clip(RoundedCornerShape(22.dp)).background(DriveColors.Panel)
                        .clickable(enabled = item.widget == DashWidget.TRIP || item.widget == DashWidget.TODAY, onClick = onTrip)
                        .padding(14.dp)) {
                        Widget(item.widget, session, music, onPlayPause, onNext, onPrevious)
                    }
                }
                // Ligne incomplète : place vide pour garder la grille.
                val span = row.fold(0) { acc, it -> acc + if (it.wide) 2 else 1 }
                if (span < 3) Spacer(Modifier.weight((3 - span).toFloat()))
            }
        }
    }
}

@Composable
private fun Widget(w: DashWidget, s: DrivingSession, music: NowPlaying?, onPlayPause: () -> Unit, onNext: () -> Unit, onPrevious: () -> Unit) {
    when (w) {
        DashWidget.SPEED -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            // Compteur rond : toujours carré et entier, quelle que soit la forme du bloc.
            Speedometer(kmh = s.fix?.kmh, unit = "km/h", waiting = "GPS…", limit = s.speedLimit, over = s.overSpeed,
                modifier = Modifier.fillMaxHeight().aspectRatio(1f, matchHeightConstraintsFirst = true))
        }
        DashWidget.CLOCK -> {
            var now by remember { mutableStateOf(Date()) }
            LaunchedEffect(Unit) { while (true) { now = Date(); delay(1000) } }
            Big(SimpleDateFormat("HH:mm", Locale.FRANCE).format(now),
                SimpleDateFormat("EEEE d MMMM", Locale.FRANCE).format(now).replaceFirstChar { it.uppercase() })
        }
        DashWidget.TRIP -> Big(TripComputer.km(s.trip.trip.distanceM),
            "Trajet · ${TripComputer.duration(s.trip.trip.movingS)}" + if (s.trip.trip.avgKmh > 0) " · moy. ${s.trip.trip.avgKmh.toInt()} km/h" else "")
        DashWidget.TODAY -> Big(TripComputer.km(s.trip.day.distanceM), "Aujourd'hui · ${TripComputer.duration(s.trip.day.movingS)} de conduite")
        DashWidget.WEATHER -> {
            val wx = s.localInfo.weather
            Big(wx?.let { "${it.emoji} ${it.tempC}°" } ?: "—", listOfNotNull(wx?.label, s.localInfo.address).joinToString(" · ").ifBlank { "Météo" })
        }
        DashWidget.ALTITUDE -> Big(s.trip.altitudeM?.let { "$it m" } ?: "—", "Altitude")
        DashWidget.RADAR -> {
            val a = s.alert
            if (a != null) Big("${AlertText.roundedDistance(a.distanceM)} m", AlertText.kindLabel(a.radar.kind) + (a.radar.speedKmh?.let { " · $it km/h" } ?: ""), Color(0xFFFF8A80))
            else Big("✓", "Aucun radar devant")
        }
        DashWidget.GUIDANCE -> {
            val g = s.guidanceState
            if (g?.next != null && !g.arrived) Big("${maneuverSymbol(g.next)} ${Guidance.shortDistance(g.toNextM)}", g.next?.instruction.orEmpty())
            else Big("—", "Pas de guidage en cours")
        }
        DashWidget.MUSIC -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Text(music?.title ?: "Aucune musique", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(music?.artist.orEmpty(), color = DriveColors.TextSecondary, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                BigButton("⏮", DriveColors.Track, onPrevious)
                BigButton(if (music?.playing == true) "⏸" else "▶", DriveColors.Accent, onPlayPause)
                BigButton("⏭", DriveColors.Track, onNext)
            }
        }
    }
}

@Composable
private fun Big(value: String, caption: String, color: Color = Color.White) {
    // Tailles calculées sur la hauteur du bloc : légende toujours visible, même sur un écran bas.
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val valueSize = with(density) { (maxHeight * 0.42f).toSp() }.let { if (it.value > 44f) 44.sp else it }
        val captionSize = with(density) { (maxHeight * 0.16f).toSp() }.let { if (it.value > 16f) 16.sp else it }
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
            Text(caption, color = DriveColors.TextSecondary, fontSize = captionSize, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(value, color = color, fontSize = valueSize, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Choix des blocs : afficher ou non, ordre, taille. */
@Composable
private fun DashboardEditor(items: List<DashItem>, onChange: (List<DashItem>) -> Unit, onClose: () -> Unit) {
    val absent = DashWidget.entries.filter { w -> items.none { it.widget == w } }
    Column(Modifier.fillMaxSize().background(DriveColors.Background).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Tableau de bord", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            BigButton("Terminé", DriveColors.Accent, onClose)
        }
        Text("↑ ↓ pour l'ordre, « Large » pour la taille. 6 à 9 blocs conseillés.", color = DriveColors.TextSecondary, fontSize = 15.sp,
            modifier = Modifier.padding(vertical = 8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(items, key = { _, it -> it.widget.name }) { i, item ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(DriveColors.Panel).padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(item.widget.label, color = Color.White, fontSize = 18.sp, modifier = Modifier.weight(1f))
                    BigButton("↑", DriveColors.Track) { if (i > 0) onChange(items.toMutableList().apply { add(i - 1, removeAt(i)) }) }
                    BigButton("↓", DriveColors.Track) { if (i < items.size - 1) onChange(items.toMutableList().apply { add(i + 1, removeAt(i)) }) }
                    BigButton(if (item.wide) "Large ✓" else "Large", if (item.wide) DriveColors.Accent else DriveColors.Track) {
                        onChange(items.toMutableList().apply { set(i, item.copy(wide = !item.wide)) })
                    }
                    BigButton("✕", Color(0xFF7F1D1D)) { onChange(items.filterIndexed { j, _ -> j != i }) }
                }
            }
            itemsIndexed(absent, key = { _, w -> "add-" + w.name }) { _, w ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(DriveColors.Panel.copy(alpha = 0.5f)).padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(w.label, color = DriveColors.TextSecondary, fontSize = 18.sp, modifier = Modifier.weight(1f))
                    BigButton("＋ Ajouter", DriveColors.Accent) { onChange(items + DashItem(w, false)) }
                }
            }
        }
    }
}
