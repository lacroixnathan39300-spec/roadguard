package fr.roadguard.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import fr.roadguard.core.LocalInfo
import fr.roadguard.core.TripComputer
import fr.roadguard.core.TripTotals
import kotlinx.coroutines.delay

/** Météo et adresse actuelle, sur une ligne. */
@Composable
fun WeatherAddressLine(info: LocalInfo, modifier: Modifier = Modifier) {
    val w = info.weather
    val a = info.address
    if (w == null && a == null) return
    Text(
        listOfNotNull(w?.let { "${it.emoji} ${it.tempC}°" }, a).joinToString("  ·  "),
        color = Color.White, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = modifier,
    )
}

/** Ordinateur de bord plein écran. */
@Composable
fun TripScreen(trip: TripComputer, onClose: () -> Unit, tracks: fr.roadguard.core.TrackRecorder? = null) {
    var period by remember { mutableStateOf("trip") }
    val context = androidx.compose.ui.platform.LocalContext.current
    var exportDays by remember { mutableStateOf<List<String>>(emptyList()) }
    var exportMessage by remember { mutableStateOf<String?>(null) }
    val saver = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/gpx+xml")
    ) { uri ->
        if (uri == null || tracks == null) return@rememberLauncherForActivityResult
        exportMessage = runCatching {
            context.contentResolver.openOutputStream(uri)!!.use { it.write(tracks.gpx(exportDays).toByteArray()) }
            "Trajets enregistrés (GPX)"
        }.getOrElse { "Export impossible : ${it.message}" }
    }
    val t: TripTotals = when (period) { "day" -> trip.day; "week" -> trip.week; "month" -> trip.month; "total" -> trip.total; else -> trip.trip }
    Column(Modifier.fillMaxSize().background(DriveColors.Background).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Ordinateur de bord", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            BigButton("Fermer", DriveColors.Panel, onClose)
        }
        Row(Modifier.padding(vertical = 12.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("trip" to "Trajet", "day" to "Aujourd'hui", "week" to "Semaine", "month" to "Mois", "total" to "Total").forEach { (k, l) ->
                BigButton(l, if (period == k) DriveColors.Accent else DriveColors.Panel) { period = k }
            }
        }
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Tile("Distance", TripComputer.km(t.distanceM), Modifier.weight(1f))
                Tile("Conduite", TripComputer.duration(t.movingS), Modifier.weight(1f))
                Tile("Arrêts", TripComputer.duration(t.stoppedS), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Tile("Vitesse moyenne", if (t.avgKmh > 0) "${t.avgKmh.toInt()} km/h" else "—", Modifier.weight(1f))
                Tile("Vitesse maxi", if (t.maxKmh > 0) "${t.maxKmh.toInt()} km/h" else "—", Modifier.weight(1f))
                Tile("Radars franchis", "${t.radars}", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Tile("Meilleur 0-60", trip.best060?.let { "%.1f s".format(java.util.Locale.FRANCE, it) } ?: "—", Modifier.weight(1f))
                Tile("Meilleur 0-100", trip.best0100?.let { "%.1f s".format(java.util.Locale.FRANCE, it) } ?: "—", Modifier.weight(1f))
                Tile("Altitude", trip.altitudeM?.let { "$it m" } ?: "—", Modifier.weight(1f))
            }
            trip.lastRun?.let { Text("Dernier chrono : $it", color = Color.White, fontSize = 16.sp) }
            Text("Les chronos se lancent seuls au départ d'un arrêt. À faire uniquement dans un lieu sûr et autorisé.",
                color = DriveColors.TextSecondary, fontSize = 13.sp)
            if (tracks != null) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BigButton("⤓ GPX d'aujourd'hui", DriveColors.Panel) {
                        exportDays = tracks.days().take(1)
                        if (exportDays.isEmpty()) exportMessage = "Aucun trajet enregistré" else runCatching { saver.launch("roadguard-${exportDays[0]}.gpx") }
                    }
                    BigButton("⤓ GPX des 30 derniers jours", DriveColors.Panel) {
                        exportDays = tracks.days()
                        if (exportDays.isEmpty()) exportMessage = "Aucun trajet enregistré" else runCatching { saver.launch("roadguard-trajets.gpx") }
                    }
                }
                exportMessage?.let { Text(it, color = DriveColors.TextSecondary, fontSize = 14.sp) }
            }
            if (period == "trip" || period == "total") {
                BigButton(if (period == "trip") "Nouveau trajet (remise à zéro)" else "Tout remettre à zéro", Color(0xFF7F1D1D)) { trip.reset(period) }
            }
        }
    }
}

@Composable
private fun Tile(label: String, value: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(DriveStyle.panelBrush).padding(14.dp)) {
        Text(label, color = DriveColors.TextSecondary, fontSize = 14.sp)
        Text(value, color = Color.White, fontSize = 28.sp * DriveColors.Scale, fontWeight = FontWeight.Bold, maxLines = 1, fontFamily = DriveStyle.Numbers)
    }
}

/**
 * Économiseur d'écran : grande horloge et date quand la voiture est arrêtée depuis [afterMs]
 * sans qu'on touche l'écran. Un appui le ferme.
 */
@Composable
fun ScreenSaver(stopped: Boolean, lastTouch: Long, afterMs: Long = 10 * 60_000L, night: Boolean, onDismiss: () -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var stoppedSince by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) { while (true) { delay(5_000); now = System.currentTimeMillis() } }
    if (!stopped) { stoppedSince = 0L; return }
    if (stoppedSince == 0L) stoppedSince = now
    if (now - maxOf(stoppedSince, lastTouch) < afterMs) return
    val c = java.util.Calendar.getInstance()
    Box(
        Modifier.fillMaxSize().background(Color.Black).clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        val dim = if (night) 0.45f else 0.9f
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("%02d:%02d".format(c.get(java.util.Calendar.HOUR_OF_DAY), c.get(java.util.Calendar.MINUTE)),
                color = Color.White.copy(alpha = dim), fontSize = 120.sp, fontWeight = FontWeight.Light)
            Text(java.text.SimpleDateFormat("EEEE d MMMM", java.util.Locale.FRANCE).format(c.time).replaceFirstChar { it.uppercase() },
                color = DriveColors.TextSecondary.copy(alpha = dim), fontSize = 26.sp)
        }
    }
}
