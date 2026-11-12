package fr.roadguard.drive

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Brush
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

/** Apparence d'un bloc : icône, titre et couleur du cadre (chaque bloc bien séparé et reconnaissable). */
private data class TileLook(val icon: String, val title: String, val color: Color)

private fun look(w: DashWidget, s: DrivingSession): TileLook = when (w) {
    DashWidget.SPEED -> TileLook("🚗", "VITESSE", if (s.overSpeed) Color(0xFFFF5252) else DriveColors.Accent)
    DashWidget.CLOCK -> TileLook("🕒", "HEURE", Color(0xFFB39DDB))
    DashWidget.TRIP -> TileLook("🧭", "TRAJET EN COURS", Color(0xFF4FC3F7))
    DashWidget.TODAY -> TileLook("📅", "AUJOURD'HUI", Color(0xFF81C784))
    DashWidget.WEATHER -> TileLook("🌦", "MÉTÉO", Color(0xFFFFD54F))
    DashWidget.MUSIC -> TileLook("🎵", "MUSIQUE", Color(0xFFF06292))
    DashWidget.RADAR -> when {
        s.alert != null -> TileLook("📷", "RADAR DEVANT", Color(0xFFFF5252))
        !s.settings.radarsOn || s.radarsBlockedHere -> TileLook("📷", "RADARS", DriveColors.TextSecondary)
        else -> TileLook("📷", "RADARS", Color(0xFF66BB6A))
    }
    DashWidget.GUIDANCE -> TileLook("➡️", "PROCHAINE DIRECTION", Color(0xFF4DD0E1))
    DashWidget.ALTITUDE -> TileLook("⛰", "ALTITUDE", Color(0xFFBCAAA4))
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
            Text("Tableau de bord", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold,
                fontFamily = fr.roadguard.core.ui.DriveStyle.Titles, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 1, modifier = Modifier.weight(1f))
            BigButton("✎ Modifier", DriveColors.Panel) { editing = true }
        }
        val rows = packRows(items)
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { item ->
                    val lk = look(item.widget, session)
                    val shape = RoundedCornerShape(22.dp)
                    // Cloison : cadre coloré et fond légèrement teinté, pour que chaque bloc se détache du fond noir.
                    Column(Modifier.weight(if (item.wide) 2f else 1f).fillMaxHeight().clip(shape)
                        .background(fr.roadguard.core.ui.DriveStyle.panelBrush)
                        .background(Brush.verticalGradient(listOf(lk.color.copy(alpha = 0.14f), Color.Transparent)))
                        .border(2.dp, lk.color.copy(alpha = 0.75f), shape)
                        .clickable(enabled = item.widget == DashWidget.TRIP || item.widget == DashWidget.TODAY, onClick = onTrip)
                        .padding(horizontal = 16.dp, vertical = 10.dp)) {
                        TileHeader(lk)
                        Box(Modifier.fillMaxWidth().weight(1f)) {
                            Widget(item.widget, session, music, onPlayPause, onNext, onPrevious)
                        }
                    }
                }
                // Ligne incomplète : place vide pour garder la grille.
                val span = row.fold(0) { acc, it -> acc + if (it.wide) 2 else 1 }
                if (span < 3) Spacer(Modifier.weight((3 - span).toFloat()))
            }
        }
    }
}

/** Titre du bloc (icône + nom en couleur) et trait de séparation. */
@Composable
private fun TileHeader(lk: TileLook) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(lk.icon, fontSize = 15.sp)
        Text(lk.title, color = lk.color, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    Box(Modifier.padding(top = 5.dp, bottom = 4.dp).fillMaxWidth().height(1.dp).background(lk.color.copy(alpha = 0.35f)))
}

@Composable
private fun Widget(w: DashWidget, s: DrivingSession, music: NowPlaying?, onPlayPause: () -> Unit, onNext: () -> Unit, onPrevious: () -> Unit) {
    when (w) {
        DashWidget.SPEED -> androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (maxWidth > maxHeight * 1.8f) {
                // Bloc large et bas : grands chiffres lisibles d'un coup d'œil, avec la limite à côté.
                val density = androidx.compose.ui.platform.LocalDensity.current
                val h = maxHeight
                val big = with(density) { (h * 0.78f).toSp() }
                val kmh = s.fix?.kmh
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(kmh?.toString() ?: "—", color = if (s.overSpeed) Color(0xFFFF5252) else Color.White, fontSize = big,
                        fontWeight = FontWeight.Bold, fontFamily = fr.roadguard.core.ui.DriveStyle.Numbers)
                    Text("km/h", color = DriveColors.TextSecondary, fontSize = with(density) { (h * 0.22f).toSp() })
                    s.speedLimit?.let { lim ->
                        Box(Modifier.size(h * 0.72f).clip(androidx.compose.foundation.shape.CircleShape).background(Color(0xFFD32F2F)).padding(h * 0.07f)
                            .clip(androidx.compose.foundation.shape.CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                            Text("$lim", color = Color.Black, fontSize = with(density) { (h * 0.28f).toSp() }, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Compteur rond : toujours carré et entier, quelle que soit la forme du bloc.
                Speedometer(kmh = s.fix?.kmh, unit = "km/h", waiting = "GPS…", limit = s.speedLimit, over = s.overSpeed,
                    modifier = Modifier.fillMaxHeight().aspectRatio(1f, matchHeightConstraintsFirst = true))
            }
        }
        DashWidget.CLOCK -> {
            var now by remember { mutableStateOf(Date()) }
            LaunchedEffect(Unit) { while (true) { now = Date(); delay(1000) } }
            Big(SimpleDateFormat("HH:mm", Locale.FRANCE).format(now),
                SimpleDateFormat("EEEE d MMMM", Locale.FRANCE).format(now).replaceFirstChar { it.uppercase() })
        }
        DashWidget.TRIP -> Big(TripComputer.km(s.trip.trip.distanceM),
            TripComputer.duration(s.trip.trip.movingS) + if (s.trip.trip.avgKmh > 0) " · moyenne ${s.trip.trip.avgKmh.toInt()} km/h" else "")
        DashWidget.TODAY -> Big(TripComputer.km(s.trip.day.distanceM), "${TripComputer.duration(s.trip.day.movingS)} de conduite")
        DashWidget.WEATHER -> {
            val wx = s.localInfo.weather
            Big(wx?.let { "${it.emoji} ${it.tempC}°" } ?: "—",
                listOfNotNull(wx?.label?.takeIf { it.isNotBlank() }, s.localInfo.address).joinToString(" · ").ifBlank { "En attente de la météo" })
        }
        DashWidget.ALTITUDE -> {
            val alt = s.trip.altitudeM
            Big(alt?.let { "$it m" } ?: "—", if (alt != null) "au-dessus du niveau de la mer" else "En attente du GPS")
        }
        DashWidget.RADAR -> Column(Modifier.fillMaxSize()) {
            val context = androidx.compose.ui.platform.LocalContext.current
            Box(Modifier.fillMaxWidth().weight(1f)) {
                val a = s.alert
                when {
                    a != null -> Big("${AlertText.roundedDistance(a.distanceM)} m",
                        AlertText.kindLabel(a.radar.kind) + (a.radar.speedKmh?.let { " · limité à $it km/h" } ?: ""), Color(0xFFFF8A80))
                    s.radarsBlockedHere -> Big("Coupés", "Suisse : alertes radars interdites", DriveColors.TextSecondary)
                    !s.settings.radarsOn -> Big("Coupés", "Alertes radars désactivées", DriveColors.TextSecondary)
                    else -> Big("✓ Aucun", "Pas de radar devant toi", Color(0xFF66BB6A))
                }
            }
            // Interrupteurs du module : bouton son (niveaux, et activation des radars avec les consignes légales),
            // et radars en Suisse (interdits là-bas : coupés par défaut, à ses risques).
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                var soundOpen by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                val lvl = s.settings.soundLevel
                RadarChip("${fr.roadguard.core.radar.SoundLevel.emoji(lvl)} ${fr.roadguard.core.radar.SoundLevel.caption(lvl)}" +
                    if (s.settings.radarsOn) "" else " · radars off", lvl != fr.roadguard.core.radar.SoundLevel.MUTE) { soundOpen = true }
                if (soundOpen) fr.roadguard.core.ui.SoundPanel(s.settings, s.voice, onClose = { soundOpen = false })
                RadarChip("🇨🇭 Suisse", s.settings.radarsOn && !s.settings.swissMode) {
                    fr.roadguard.core.ui.Legal.toggleSwiss(context, s.settings)?.let {
                        android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        DashWidget.GUIDANCE -> {
            val g = s.guidanceState
            if (g?.next != null && !g.arrived) Big("${maneuverSymbol(g.next)} ${Guidance.shortDistance(g.toNextM)}", g.next?.instruction.orEmpty())
            else Big("—", "Choisis une destination sur la carte", DriveColors.TextSecondary)
        }
        DashWidget.MUSIC -> androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
            val title = music?.title ?: "Aucune musique"
            val sub = music?.artist?.takeIf { it.isNotBlank() } ?: if (music == null) "Lance ta musique depuis une appli" else ""
            if (maxWidth >= 380.dp) {
                // Bloc large : titre à gauche, commandes à droite (tout tient sur une ligne).
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (sub.isNotBlank()) Text(sub, color = DriveColors.TextSecondary, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    BigButton("⏮", DriveColors.Track, onPrevious)
                    BigButton(if (music?.playing == true) "⏸" else "▶", DriveColors.Accent, onPlayPause)
                    BigButton("⏭", DriveColors.Track, onNext)
                }
            } else Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (sub.isNotBlank()) Text(sub, color = DriveColors.TextSecondary, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    BigButton("⏮", DriveColors.Track, onPrevious)
                    BigButton(if (music?.playing == true) "⏸" else "▶", DriveColors.Accent, onPlayPause)
                    BigButton("⏭", DriveColors.Track, onNext)
                }
            }
        }
    }
}

/** Petit interrupteur du module radars (vert : actif, rouge : coupé). */
@Composable
private fun RadarChip(label: String, on: Boolean, onClick: () -> Unit) {
    val c = if (on) Color(0xFF2ED573) else Color(0xFFFF4757)
    Text(if (on) "$label ✓" else "$label ✕", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1,
        modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(c.copy(alpha = 0.18f)).border(1.5.dp, c, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 6.dp))
}

/** Valeur en grand, et en dessous le détail (le titre du bloc est déjà en haut). */
@Composable
private fun Big(value: String, detail: String, color: Color = Color.White) {
    // Tailles calculées sur la hauteur disponible : lisible d'un coup d'œil, détail toujours visible.
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val hasDetail = detail.isNotBlank()
        val valueSize = with(density) { (maxHeight * (if (hasDetail) 0.56f else 0.72f)).toSp() }.let { if (it.value > 60f) 60.sp else it }
        val detailSize = with(density) { (maxHeight * 0.22f).toSp() }.let { if (it.value > 19f) 19.sp else it }
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
            Text(value, color = color, fontSize = valueSize, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                fontFamily = fr.roadguard.core.ui.DriveStyle.Numbers)
            if (hasDetail) Text(detail, color = DriveColors.TextSecondary, fontSize = detailSize, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
