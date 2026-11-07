package fr.roadguard.core.ui

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.roadguard.core.DrivingSession
import fr.roadguard.core.nav.LatLon
import fr.roadguard.core.nav.Place
import fr.roadguard.core.nearby.Fuel
import fr.roadguard.core.nearby.NearbyApi
import fr.roadguard.core.nearby.NearbyPlace
import fr.roadguard.core.nearby.PoiKind
import fr.roadguard.core.safety.FuelEntry
import fr.roadguard.core.safety.TripRecord
import kotlinx.coroutines.launch

private val dateTime = java.text.SimpleDateFormat("EEE dd/MM HH:mm", java.util.Locale.FRANCE)
private val hourMin = java.text.SimpleDateFormat("HH:mm", java.util.Locale.FRANCE)

@Composable
private fun ScreenFrame(title: String, onClose: () -> Unit, actions: @Composable () -> Unit = {}, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().background(DriveColors.Background).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            actions()
            BigButton("Fermer", DriveColors.Panel, onClose)
        }
        content()
    }
}

@Composable
private fun ExCard(modifier: Modifier = Modifier, color: Color = DriveColors.Panel, content: @Composable () -> Unit) {
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(color).padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { content() }
}

@Composable
private fun ExField(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, number: Boolean = false) {
    OutlinedTextField(value, onChange, modifier = modifier, label = { Text(label) }, singleLine = true,
        keyboardOptions = if (number) KeyboardOptions(keyboardType = KeyboardType.Decimal) else KeyboardOptions.Default,
        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White,
            focusedBorderColor = DriveColors.Accent, unfocusedBorderColor = DriveColors.Track,
            focusedLabelColor = DriveColors.Accent, unfocusedLabelColor = DriveColors.TextSecondary))
}

private fun distanceText(m: Double) = if (m < 1000) "${(m / 10).toInt() * 10} m" else String.format(java.util.Locale.FRANCE, "%.1f km", m / 1000)

private fun shareText(context: Context, text: String, title: String) = runCatching {
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), title)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

// ======================= Voiture garée et minuteur de stationnement =======================

/** Minuteur de stationnement : rappel 10 min avant la fin, puis à la fin. */
object ParkingAlarm {
    private const val CHANNEL = "pilote_parking"
    private fun pending(context: Context, code: Int, text: String) = PendingIntent.getBroadcast(context, code,
        Intent(context, ParkingAlarmReceiver::class.java).putExtra("text", text), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    fun schedule(context: Context, endAt: Long) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        cancel(context)
        if (endAt - 10 * 60_000 > System.currentTimeMillis())
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endAt - 10 * 60_000, pending(context, 51, "Stationnement : plus que 10 minutes."))
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endAt, pending(context, 52, "Stationnement terminé : pense à ta voiture."))
    }
    fun cancel(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        am.cancel(pending(context, 51, "")); am.cancel(pending(context, 52, ""))
    }
    fun notify(context: Context, text: String) = runCatching {
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return@runCatching
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Stationnement", NotificationManager.IMPORTANCE_HIGH))
        nm.notify(4401, Notification.Builder(context, CHANNEL).setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("🅿 Pilote").setContentText(text).setAutoCancel(true).build())
    }
}

class ParkingAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) { ParkingAlarm.notify(context, intent.getStringExtra("text") ?: "Stationnement") }
}

/** « Où est ma voiture ? » : position mémorisée en fin de trajet, retour à pied, minuteur. */
@Composable
fun ParkingScreen(session: DrivingSession, onClose: () -> Unit) {
    val context = LocalContext.current
    var p by remember { mutableStateOf(session.tripLog.parking) }
    var tick by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(15_000); tick = System.currentTimeMillis() } }
    ScreenFrame("🅿 Ma voiture", onClose) {
        Column(Modifier.verticalScroll(rememberScrollState()).widthIn(max = 900.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val cur = p
            if (cur == null) Text("Aucune position enregistrée. Elle est mémorisée toute seule à la fin de chaque trajet, ou avec le bouton ci-dessous.",
                color = DriveColors.TextSecondary, fontSize = 17.sp)
            else ExCard {
                Text("Garée ${dateTime.format(java.util.Date(cur.at))}", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(cur.address ?: "%.5f, %.5f".format(java.util.Locale.US, cur.lat, cur.lon), color = DriveColors.TextSecondary, fontSize = 16.sp)
                session.here?.let { h -> Text("À ${distanceText(fr.roadguard.core.radar.Geo.distanceMeters(h.lat, h.lon, cur.lat, cur.lon))} d'ici", color = DriveColors.Accent, fontSize = 16.sp) }
                if (cur.timerEnd > tick) Text("⏱ Stationnement jusqu'à ${hourMin.format(java.util.Date(cur.timerEnd))} (encore ${(cur.timerEnd - tick) / 60_000} min)",
                    color = Color(0xFFFFC857), fontSize = 17.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (cur != null) {
                    BigButton("🚶 Y aller à pied", DriveColors.Accent) {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=${cur.lat},${cur.lon}&mode=w")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }.onFailure {
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:${cur.lat},${cur.lon}?q=${cur.lat},${cur.lon}(Ma voiture)")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                        }
                    }
                    BigButton("📤 Partager", DriveColors.Panel) { shareText(context, "Ma voiture : https://maps.google.com/?q=${cur.lat},${cur.lon}", "Partager") }
                }
                BigButton("📍 Garée ici", DriveColors.Panel) {
                    session.here?.let { h ->
                        val np = fr.roadguard.core.safety.Parking(h.lat, h.lon, System.currentTimeMillis(), session.localInfo.address, 0)
                        session.tripLog.parking = np; p = np
                    }
                }
            }
            Text("Minuteur de stationnement (zone bleue, parking payant)", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(30, 60, 90, 120, 180).forEach { m ->
                    BigButton(if (m < 60) "$m min" else "${m / 60} h${if (m % 60 != 0) " ${m % 60}" else ""}", DriveColors.Panel) {
                        val base = p ?: session.here?.let { h -> fr.roadguard.core.safety.Parking(h.lat, h.lon, System.currentTimeMillis(), session.localInfo.address, 0) }
                        if (base != null) {
                            val np = base.copy(timerEnd = System.currentTimeMillis() + m * 60_000L)
                            session.tripLog.parking = np; p = np
                            ParkingAlarm.schedule(context, np.timerEnd)
                        }
                    }
                }
                if ((p?.timerEnd ?: 0) > tick) BigButton("Arrêter", Color(0xFF7F1D1D)) {
                    ParkingAlarm.cancel(context); p?.let { val np = it.copy(timerEnd = 0); session.tripLog.parking = np; p = np }
                }
            }
        }
    }
}

// ======================= Historique, journal kilométrique, carburant, entretien =======================

@Composable
fun TripHistoryScreen(session: DrivingSession, onClose: () -> Unit) {
    val context = LocalContext.current
    val log = session.tripLog
    var tab by remember { mutableStateOf(0) }
    var trips by remember { mutableStateOf(log.trips()) }
    var fuel by remember { mutableStateOf(log.fuel()) }
    var csvFor by remember { mutableStateOf<List<TripRecord>>(emptyList()) }
    val save = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(log.csv(csvFor).toByteArray(Charsets.UTF_8)) } }
    }
    ScreenFrame("📒 Trajets", onClose) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Trajets", "Journal km", "Carburant", "Entretien", "🏆 Badges").forEachIndexed { i, l ->
                BigButton(l, if (tab == i) DriveColors.Accent else DriveColors.Panel) { tab = i }
            }
        }
        when (tab) {
            0 -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (trips.isEmpty()) item { Text("Aucun trajet pour l'instant : ils s'enregistrent tout seuls.", color = DriveColors.TextSecondary, fontSize = 16.sp) }
                items(trips, key = { it.start }) { t ->
                    ExCard {
                        Text("${dateTime.format(java.util.Date(t.start))} → ${hourMin.format(java.util.Date(t.end))}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(listOfNotNull(t.from, t.to).joinToString(" → "), color = DriveColors.TextSecondary, fontSize = 14.sp)
                        Text(String.format(java.util.Locale.FRANCE, "%.1f km · %d min · moyenne %d km/h · max %d km/h · %d radar(s)",
                            t.km, (t.end - t.start) / 60_000, t.avgKmh, t.maxKmh, t.radars), color = Color.White, fontSize = 15.sp)
                        Text("🌱 Éco-conduite ${t.ecoScore}/100 · ${t.hardBrakes} freinage(s) et ${t.hardAccels} accélération(s) brusques · " +
                            "${t.overSeconds / 60} min au-dessus des limites", color = if (t.ecoScore >= 75) Color(0xFF7CE38B) else Color(0xFFFFC857), fontSize = 14.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            BigButton(if (t.pro) "💼 Pro" else "🏠 Perso", if (t.pro) Color(0xFF1565C0) else DriveColors.Track) {
                                log.setPro(t.start, !t.pro); trips = log.trips()
                            }
                            BigButton("🗑", DriveColors.Track) { log.deleteTrip(t.start); trips = log.trips() }
                        }
                    }
                }
            }
            1 -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val month = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH)
                val year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                fun inMonth(t: TripRecord) = java.util.Calendar.getInstance().apply { timeInMillis = t.start }.let { it.get(java.util.Calendar.MONTH) == month && it.get(java.util.Calendar.YEAR) == year }
                fun inYear(t: TripRecord) = java.util.Calendar.getInstance().apply { timeInMillis = t.start }.get(java.util.Calendar.YEAR) == year
                ExCard {
                    Text("Ce mois-ci", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(String.format(java.util.Locale.FRANCE, "💼 Pro : %.0f km   🏠 Perso : %.0f km",
                        trips.filter { inMonth(it) && it.pro }.sumOf { it.km }, trips.filter { inMonth(it) && !it.pro }.sumOf { it.km }), color = Color.White, fontSize = 17.sp)
                    Text("Cette année", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(String.format(java.util.Locale.FRANCE, "💼 Pro : %.0f km   🏠 Perso : %.0f km",
                        trips.filter { inYear(it) && it.pro }.sumOf { it.km }, trips.filter { inYear(it) && !it.pro }.sumOf { it.km }), color = Color.White, fontSize = 17.sp)
                }
                Text("Classe chaque trajet en Pro ou Perso dans l'onglet Trajets, puis exporte le journal (tableur) pour tes frais réels ou ta facturation.",
                    color = DriveColors.TextSecondary, fontSize = 15.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BigButton("⬇ Exporter les trajets pro", DriveColors.Accent) { csvFor = trips.filter { inYear(it) && it.pro }; save.launch("journal-km-pro-$year.csv") }
                    BigButton("⬇ Tout exporter", DriveColors.Panel) { csvFor = trips; save.launch("trajets-$year.csv") }
                }
                ToggleLine("Nouveaux trajets classés « Pro » par défaut", session.settings.defaultTripPro) { session.settings.changeDefaultTripPro(it) }
            }
            2 -> {
                var liters by remember { mutableStateOf("") }; var price by remember { mutableStateOf("") }; var odo by remember { mutableStateOf("") }
                var full by remember { mutableStateOf(true) }
                val (l100, perKm) = remember(fuel) { log.consumption() }
                ExCard {
                    Text(if (l100 != null) String.format(java.util.Locale.FRANCE, "⛽ Consommation réelle : %.1f L/100 km", l100) else "⛽ Consommation : ajoute au moins deux pleins complets",
                        color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    if (perKm != null) Text(String.format(java.util.Locale.FRANCE, "Coût : %.2f € au km · %.0f € pour 1 000 km", perKm, perKm * 1000), color = DriveColors.TextSecondary, fontSize = 15.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    ExField(liters, { liters = it }, "Litres", Modifier.weight(1f), number = true)
                    ExField(price, { price = it }, "Prix total €", Modifier.weight(1f), number = true)
                    ExField(odo, { odo = it }, "Compteur km", Modifier.weight(1f), number = true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BigButton(if (full) "Plein complet ✓" else "Plein partiel", DriveColors.Panel) { full = !full }
                    BigButton("Ajouter le plein", DriveColors.Accent) {
                        val l = liters.replace(',', '.').toDoubleOrNull(); val pr = price.replace(',', '.').toDoubleOrNull()
                        if (l != null && pr != null) {
                            log.addFuel(FuelEntry(System.currentTimeMillis(), l, pr, odo.filter(Char::isDigit).toIntOrNull() ?: 0, full))
                            fuel = log.fuel(); liters = ""; price = ""; odo = ""
                        }
                    }
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(fuel.reversed(), key = { it.at }) { e ->
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(DriveColors.Panel).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(String.format(java.util.Locale.FRANCE, "%s · %.2f L · %.2f € (%.3f €/L)%s%s", dateTime.format(java.util.Date(e.at)), e.liters, e.price,
                                if (e.liters > 0) e.price / e.liters else 0.0, if (e.odometer > 0) " · ${e.odometer} km" else "", if (e.full) "" else " · partiel"),
                                color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f))
                            BigButton("🗑", DriveColors.Track) { log.deleteFuel(e.at); fuel = log.fuel() }
                        }
                    }
                }
            }
            4 -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val c = session.community
                item { Text("Défis du mois", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                items(fr.roadguard.core.safety.Badges.challenges(trips)) { BadgeRow(it) }
                item { Text("Badges", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
                items(fr.roadguard.core.safety.Badges.badges(trips, log.odometerKm, c.points, c.myLevel, c.myJudged)) { BadgeRow(it) }
            }
            else -> Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val odoKm = log.odometerKm
                val due = session.settings.serviceAtKm
                ExCard {
                    Text(String.format(java.util.Locale.FRANCE, "Kilomètres parcourus avec Pilote : %.0f km", odoKm), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(if (due > 0) String.format(java.util.Locale.FRANCE, "Prochain entretien dans %.0f km", (due - odoKm).coerceAtLeast(0.0)) else "Aucun rappel d'entretien",
                        color = if (due > 0 && odoKm >= due) Color(0xFFFF6B6B) else DriveColors.TextSecondary, fontSize = 16.sp)
                }
                Text("Me rappeler l'entretien (vidange, pneus, révision) dans :", color = Color.White, fontSize = 16.sp)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1000, 5000, 10000, 15000, 20000, 30000).forEach { km ->
                        BigButton("${km / 1000} 000 km", DriveColors.Panel) { session.settings.changeServiceAtKm((odoKm + km).toInt()) }
                    }
                    BigButton("Aucun", DriveColors.Track) { session.settings.changeServiceAtKm(0) }
                }
                Text("Ton carnet d'entretien détaillé reste dans Trajet → Entretien.", color = DriveColors.TextSecondary, fontSize = 14.sp)
                Text("📅 Échéances (prévenu un mois avant)", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                val st = session.settings
                @Suppress("UNUSED_VARIABLE") val tick = st.reminderTick
                fr.roadguard.core.safety.Reminders.items.forEach { (key, label) ->
                    val at = st.reminderDate(key)
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(DriveColors.Panel).padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(label, color = Color.White, fontSize = 16.sp)
                            if (at > 0) {
                                val days = ((at - System.currentTimeMillis()) / 86_400_000L).toInt()
                                Text(java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.FRANCE).format(java.util.Date(at)) +
                                    if (days < 0) " · dépassé !" else " · dans $days jours",
                                    color = if (days <= 30) Color(0xFFFFC857) else DriveColors.TextSecondary, fontSize = 14.sp)
                            }
                        }
                        BigButton(if (at > 0) "Changer" else "Date", DriveColors.Track) {
                            val c = java.util.Calendar.getInstance().apply { if (at > 0) timeInMillis = at }
                            android.app.DatePickerDialog(context, { _, y, m, d ->
                                st.setReminderDate(key, java.util.Calendar.getInstance().apply { set(y, m, d, 9, 0, 0) }.timeInMillis)
                            }, c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH), c.get(java.util.Calendar.DAY_OF_MONTH)).show()
                        }
                        if (at > 0) BigButton("✕", DriveColors.Track) { st.setReminderDate(key, 0L) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToggleLine(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(DriveColors.Panel).clickable { onChange(!checked) }.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Text(if (checked) "✅" else "⬜", fontSize = 22.sp)
    }
}

/** Bilan affiché à la fin d'un trajet. */
@Composable
fun TripSummaryCard(session: DrivingSession, modifier: Modifier = Modifier) {
    val t = session.lastTrip ?: return
    ExCard(modifier.widthIn(max = 560.dp), color = Color(0xF0123A6B)) {
        Text("🏁 Trajet terminé", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(String.format(java.util.Locale.FRANCE, "%.1f km en %d min · moyenne %d km/h · éco-conduite %d/100", t.km, (t.end - t.start) / 60_000, t.avgKmh, t.ecoScore),
            color = Color.White, fontSize = 16.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BigButton(if (t.pro) "💼 Pro" else "🏠 Perso", DriveColors.Panel) {
                session.tripLog.setPro(t.start, !t.pro); session.lastTrip = t.copy(pro = !t.pro)
            }
            BigButton("OK", DriveColors.Accent) { session.lastTrip = null }
        }
    }
}

// ======================= À proximité : carburants, bornes, parkings, aires, toilettes =======================

@Composable
fun NearbyScreen(session: DrivingSession, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf(0) }  // 0 carburant, 1.. PoiKind
    var fuel by remember { mutableStateOf(Fuel.GAZOLE) }
    var list by remember { mutableStateOf<List<NearbyPlace>>(emptyList()) }
    var info by remember { mutableStateOf<String?>(null) }
    val here = session.here
    LaunchedEffect(tab, fuel) {
        val h = session.here ?: run { info = "Position GPS indisponible"; return@LaunchedEffect }
        info = "Recherche…"; list = emptyList()
        runCatching {
            if (tab == 0) NearbyApi.fuel(h.lat, h.lon, fuel) else NearbyApi.poi(h.lat, h.lon, PoiKind.entries[tab - 1])
        }.onSuccess { list = it; info = if (it.isEmpty()) "Rien trouvé dans le secteur" else null }
            .onFailure { info = "Recherche impossible : ${it.message}" }
    }
    ScreenFrame("📍 À proximité", onClose) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BigButton("⛽ Carburants", if (tab == 0) DriveColors.Accent else DriveColors.Panel) { tab = 0 }
            PoiKind.entries.forEachIndexed { i, k -> BigButton("${k.emoji} ${k.label}", if (tab == i + 1) DriveColors.Accent else DriveColors.Panel) { tab = i + 1 } }
        }
        if (tab == 0) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Fuel.entries.forEach { f -> BigButton(f.label, if (fuel == f) Color(0xFF1565C0) else DriveColors.Track) { fuel = f } }
        }
        if (tab == 0) Text("Prix officiels en France (prix-carburants.gouv.fr), du moins cher au plus cher, à 10 km.", color = DriveColors.TextSecondary, fontSize = 14.sp)
        info?.let { Text(it, color = DriveColors.TextSecondary, fontSize = 16.sp) }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(list) { p ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(DriveColors.Panel).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(p.name, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text(listOf(distanceText(p.distanceM), p.detail).filter { it.isNotBlank() }.joinToString(" · "), color = DriveColors.TextSecondary, fontSize = 14.sp)
                    }
                    p.price?.let { Text(String.format(java.util.Locale.FRANCE, "%.3f €", it), color = Color(0xFF7CE38B), fontSize = 20.sp, fontWeight = FontWeight.Black) }
                    BigButton("Y aller", DriveColors.Accent) {
                        session.startNavigation(Place(p.name, p.detail, LatLon(p.lat, p.lon))); onClose()
                    }
                }
            }
        }
        if (here == null) Text("Active la position pour chercher autour de toi.", color = DriveColors.TextSecondary, fontSize = 14.sp)
        @Suppress("UNUSED_VARIABLE") val unused = scope
    }
}

// ======================= Écran tête haute (HUD) =======================

/** Affichage en miroir pour le pare-brise : vitesse, limite, prochaine consigne. Plein écran noir. */
@Composable
fun HudScreen(session: DrivingSession, onClose: () -> Unit) {
    var mirror by remember { mutableStateOf(session.settings.hudMirror) }
    Box(Modifier.fillMaxSize().background(Color.Black).clickable { onClose() }) {
        Column(Modifier.fillMaxSize().graphicsLayer { scaleX = if (mirror) -1f else 1f }.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            val kmh = session.fix?.kmh ?: 0
            Text("$kmh", color = if (session.overSpeed) Color(0xFFFF4757) else Color(0xFF00E5FF), fontSize = 160.sp, fontWeight = FontWeight.Black)
            Row(horizontalArrangement = Arrangement.spacedBy(30.dp), verticalAlignment = Alignment.CenterVertically) {
                session.speedLimit?.let { Text("⛔ $it", color = Color(0xFFFF4757), fontSize = 54.sp, fontWeight = FontWeight.Bold) }
                session.alert?.let { Text("📷 ${it.distanceM} m", color = Color(0xFFFFC857), fontSize = 54.sp, fontWeight = FontWeight.Bold) }
            }
            session.guidanceState?.let { g ->
                g.next?.let { m -> Text("${fr.roadguard.core.nav.Guidance.shortDistance(g.toNextM)} · ${m.instruction}", color = Color.White, fontSize = 40.sp, maxLines = 2) }
            }
        }
        Row(Modifier.align(Alignment.TopEnd).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BigButton(if (mirror) "Miroir ✓" else "Miroir", Color(0x33FFFFFF)) { mirror = !mirror; session.settings.changeHudMirror(mirror) }
            BigButton("✕", Color(0x33FFFFFF), onClose)
        }
    }
}

// ======================= Convoi entre amis et partage de trajet =======================

@Composable
fun ConvoyScreen(session: DrivingSession, onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val c = session.community
    var code by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    ScreenFrame("🚗 Convoi et partage", onClose) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                ExCard {
                    Text("Convoi entre amis", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    val cur = c.convoyCode
                    if (cur == null) {
                        Text("Choisis un code (ex. JURA39) et donne-le à tes amis : vous vous voyez sur la carte, et vous pouvez vous envoyer un message dit à voix haute.",
                            color = DriveColors.TextSecondary, fontSize = 15.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            ExField(code, { code = it.uppercase().take(8) }, "Code du convoi", Modifier.weight(1f))
                            BigButton("Rejoindre", DriveColors.Accent) { c.joinConvoy(code) }
                            BigButton("Nouveau", DriveColors.Panel) {
                                val n = (1..6).map { "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".random() }.joinToString(""); code = n; c.joinConvoy(n)
                            }
                        }
                    } else {
                        Text("Convoi $cur · ${c.convoy.size} ami(s) visible(s) sur la carte", color = DriveColors.Accent, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        c.convoy.forEach { d ->
                            val h = session.here
                            Text("${d.name}${d.speed?.let { " · $it km/h" } ?: ""}${h?.let { " · à " + distanceText(fr.roadguard.core.radar.Geo.distanceMeters(it.lat, it.lon, d.lat, d.lon)) } ?: ""}",
                                color = Color.White, fontSize = 16.sp)
                        }
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("On s'arrête à la prochaine aire", "Je fais le plein", "J'ai du retard", "Attendez-moi", "Tout va bien").forEach { m ->
                                BigButton(m, DriveColors.Panel) { c.convoyMessage(m) }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            ExField(msg, { msg = it.take(140) }, "Message au convoi", Modifier.weight(1f))
                            BigButton("Envoyer", DriveColors.Accent) { if (msg.isNotBlank()) { c.convoyMessage(msg.trim()); msg = "" } }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            BigButton("📤 Inviter", DriveColors.Panel) { shareText(context, "Rejoins mon convoi Pilote avec le code $cur (Menu → Convoi et partage).", "Inviter") }
                            BigButton("Quitter le convoi", Color(0xFF7F1D1D)) { c.leaveConvoy() }
                        }
                    }
                }
            }
            item {
                ExCard {
                    Text("Partager mon trajet en direct", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Envoie un lien : la personne suit ta position et ton heure d'arrivée sur une carte, sans appli. Le lien expire au bout de 12 h.",
                        color = DriveColors.TextSecondary, fontSize = 15.sp)
                    val link = c.shareLink
                    if (link == null) BigButton("📤 Partager mon trajet", DriveColors.Accent) {
                        scope.launch {
                            c.startShare {
                                val dest = session.recents.firstOrNull()?.name?.takeIf { session.navigating }
                                val eta = session.guidanceState?.remainingS?.let { System.currentTimeMillis() + it * 1000L }
                                dest to eta
                            }.onSuccess { shareText(context, "Suis mon trajet en direct : $it", "Partager mon trajet") }
                        }
                    } else Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BigButton("📤 Renvoyer le lien", DriveColors.Panel) { shareText(context, "Suis mon trajet en direct : $link", "Partager mon trajet") }
                        BigButton("Arrêter le partage", Color(0xFF7F1D1D)) { c.stopShare() }
                    }
                }
            }
        }
    }
}

// ======================= Accident : compte à rebours =======================

@Composable
fun CrashOverlay(session: DrivingSession) {
    val at = session.crashAt
    if (at == 0L) return
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(at) { while (true) { kotlinx.coroutines.delay(500); now = System.currentTimeMillis() } }
    val left = ((30_000 - (now - at)) / 1000).coerceAtLeast(0)
    Box(Modifier.fillMaxSize().background(Color(0xF0B71C1C)).padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("💥 Choc détecté", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Black)
            Text("Tout va bien ?", color = Color.White, fontSize = 28.sp)
            Text("Sans réponse, ${session.settings.emergencyName.ifBlank { "ton contact d'urgence" }} sera prévenu dans $left s, avec ta position.",
                color = Color.White, fontSize = 20.sp)
            BigButton("✅ Je vais bien", Color(0xFF1B5E20)) { session.cancelCrash() }
        }
    }
}

// ======================= Réglages : sécurité et pratique =======================

@Composable
fun SafetySettings(settings: fr.roadguard.core.DriveSettings) {
    val context = LocalContext.current
    val phone = !context.packageName.contains("drive")
    Text("Sécurité et code de la route", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
    ToggleLine("Suisse : couper les radars automatiquement (interdits dans le pays)", settings.swissMode) { v ->
        settings.changeSwissMode(v)
        // Désactivation seulement après la case dédiée (sinon, la confirmation s'affiche).
        if (!v && settings.swissMode) Legal.askSwiss = true
    }
    ToggleLine("Pluie : abaisser les limitations (130→110, 110→100, 90→80)", settings.rainLimits, settings::changeRainLimits)
    ToggleLine("Jeune conducteur (permis probatoire : 110 / 100 / 80)", settings.driverProfile == 1) { settings.changeDriverProfile(if (it) 1 else 0) }
    Text("Vitesse maximale personnelle (remorque, caravane…)", color = Color.White, fontSize = 16.sp)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(0 to "Aucune", 80 to "80", 90 to "90", 100 to "100", 110 to "110", 120 to "120").forEach { (v, l) ->
            BigButton(l, if (settings.personalMax == v) DriveColors.Accent else DriveColors.Panel) { settings.changePersonalMax(v) }
        }
    }
    ToggleLine("Météo dangereuse : verglas, brouillard, fortes pluies (ici et sur l'itinéraire)", settings.weatherAlerts, settings::changeWeatherAlerts)
    ToggleLine("Passages à niveau, virages dangereux, écoles", settings.hazardAlerts, settings::changeHazardAlerts)
    ToggleLine("Fin de trajet : « rien oublié à l'arrière ? »", settings.forgetReminder, settings::changeForgetReminder)
    ToggleLine("Signalements : « toujours là ? » à la voix, réponse « oui / non » à la voix", settings.voiceConfirm) { v ->
        settings.changeVoiceConfirm(v)
        if (v && context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            (context as? android.app.Activity)?.requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), 81)
    }
    if (phone) {
        ToggleLine("Détection d'accident (choc + arrêt → SMS au contact d'urgence après 30 s)", settings.crashDetect) { v ->
            settings.changeCrashDetect(v)
            if (v && android.os.Build.VERSION.SDK_INT >= 23 && context.checkSelfPermission(android.Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                (context as? android.app.Activity)?.requestPermissions(arrayOf(android.Manifest.permission.SEND_SMS), 77)
            }
        }
        ToggleLine("🚨 Sirène de police française si je m'endors (anti-somnolence)", settings.sirenWake) { v ->
            settings.changeSirenWake(v)
            if (v) fr.roadguard.core.radar.PoliceSiren.burst(2) // pour l'entendre
        }
        Text("Confort", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        ToggleLine("Lire mes messages à voix haute (SMS, WhatsApp…), avec réponse dictée", settings.speakNotifications) { v ->
            settings.changeSpeakNotifications(v)
            val enabled = android.provider.Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")?.contains(context.packageName) == true
            if (v && !enabled) runCatching { context.startActivity(Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }
        ToggleLine("Vibrer à chaque radar (relayé sur ma montre connectée)", settings.watchAlerts, settings::changeWatchAlerts)
        ToggleLine("Mode moto : gros boutons, tout à la voix (casque)", settings.motoMode) { v ->
            settings.changeMotoMode(v); settings.changeScale(if (v) 1.35f else 1f)
        }
        ToggleLine("Dashcam : filmer la route en boucle (caméra arrière), vidéo gardée en cas de choc", settings.dashcam) { v ->
            settings.changeDashcam(v)
            if (v && settings.drowsyGuard) settings.changeDrowsyGuard(false)
            if (v && context.checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
                (context as? android.app.Activity)?.requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 78)
        }
        if (settings.dashcam) Text("La dashcam et l'anti-somnolence utilisent chacune une caméra : une seule à la fois.", color = DriveColors.TextSecondary, fontSize = 13.sp)
        CarBluetoothPicker(settings)
        Text("🔋 Économie de batterie", color = Color.White, fontSize = 16.sp)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0 to "Auto (batterie ≤ 20 %)", 1 to "Toujours", 2 to "Jamais").forEach { (v, l) ->
                BigButton(l, if (settings.batterySaver == v) DriveColors.Accent else DriveColors.Panel) { settings.changeBatterySaver(v) }
            }
        }
        ToggleLine("Agenda : proposer l'itinéraire vers mes prochains rendez-vous", settings.calendarHints) { v ->
            settings.changeCalendarHints(v)
            if (v && context.checkSelfPermission(android.Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED)
                (context as? android.app.Activity)?.requestPermissions(arrayOf(android.Manifest.permission.READ_CALENDAR), 79)
        }
        if (settings.crashDetect) {
            var name by remember { mutableStateOf(settings.emergencyName) }
            var num by remember { mutableStateOf(settings.emergencyPhone) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                ExField(name, { name = it.take(40) }, "Contact (nom)", Modifier.weight(1f))
                ExField(num, { num = it.filter { c -> c.isDigit() || c == '+' || c == ' ' }.take(20) }, "Téléphone", Modifier.weight(1f), number = true)
                BigButton("Enregistrer", DriveColors.Accent) { settings.changeEmergency(name.trim(), num.trim()) }
            }
        }
    }
}



/** Choix du Bluetooth de la voiture : Pilote démarre à la connexion, et le trajet se termine à la déconnexion. */
@android.annotation.SuppressLint("MissingPermission")
@Composable
private fun CarBluetoothPicker(settings: fr.roadguard.core.DriveSettings) {
    val context = LocalContext.current
    var open by remember { mutableStateOf(false) }
    val label = if (settings.carBluetooth.isBlank()) "Bluetooth de la voiture : aucun (touche pour choisir)" else "Bluetooth de la voiture : ${settings.carBluetoothName}"
    ToggleLine(label, settings.carBluetooth.isNotBlank()) { open = !open }
    if (!open) return
    val granted = android.os.Build.VERSION.SDK_INT < 31 || context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
    if (!granted) {
        BigButton("Autoriser le Bluetooth", DriveColors.Accent) {
            (context as? android.app.Activity)?.requestPermissions(arrayOf(android.Manifest.permission.BLUETOOTH_CONNECT), 80)
        }
        return
    }
    val devices = remember { runCatching {
        context.getSystemService(android.bluetooth.BluetoothManager::class.java)?.adapter?.bondedDevices?.map { (it.name ?: it.address) to it.address }
    }.getOrNull().orEmpty() }
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        devices.forEach { (name, addr) ->
            BigButton(name, if (settings.carBluetooth == addr) DriveColors.Accent else DriveColors.Panel) { settings.changeCarBluetooth(addr, name); open = false }
        }
        BigButton("Aucun", DriveColors.Track) { settings.changeCarBluetooth("", ""); open = false }
    }
    if (devices.isEmpty()) Text("Aucun appareil Bluetooth appairé.", color = DriveColors.TextSecondary, fontSize = 14.sp)
}


@Composable
private fun BadgeRow(b: fr.roadguard.core.safety.Badge) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (b.done) Color(0xFF1B3A2A) else DriveColors.Panel).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(b.emoji, fontSize = 30.sp, color = Color.White.copy(alpha = if (b.done) 1f else 0.4f))
        Column(Modifier.weight(1f)) {
            Text(b.title + if (b.done) " ✓" else "", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(b.detail, color = DriveColors.TextSecondary, fontSize = 14.sp)
            if (!b.done) Box(Modifier.padding(top = 6.dp).fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(DriveColors.Track)) {
                Box(Modifier.fillMaxWidth(b.progress).padding(vertical = 3.dp).background(DriveColors.Accent))
            }
        }
    }
}
