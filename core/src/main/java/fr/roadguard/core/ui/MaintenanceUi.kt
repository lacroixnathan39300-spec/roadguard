package fr.roadguard.core.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.roadguard.core.TripComputer
import org.json.JSONArray
import org.json.JSONObject

/** Une opération d'entretien : tous les [everyKm] km et/ou tous les [everyMonths] mois. */
data class MaintenanceItem(val name: String, val everyKm: Int, val everyMonths: Int, val doneKm: Int, val doneAt: Long) {
    fun dueKm(): Int? = if (everyKm > 0) doneKm + everyKm else null
    fun dueAt(): Long? = if (everyMonths > 0) doneAt + everyMonths * 30L * 86_400_000L else null
    /** Reste avant l'échéance : km (négatif = dépassé) et jours. */
    fun remainingKm(odo: Int) = dueKm()?.minus(odo)
    fun remainingDays(now: Long = System.currentTimeMillis()) = dueAt()?.let { ((it - now) / 86_400_000L).toInt() }
    fun urgent(odo: Int) = (remainingKm(odo) ?: Int.MAX_VALUE) < 1000 || (remainingDays() ?: Int.MAX_VALUE) < 30
    fun toJson(): JSONObject = JSONObject().put("n", name).put("k", everyKm).put("m", everyMonths).put("dk", doneKm).put("dt", doneAt)
    companion object {
        fun from(o: JSONObject) = MaintenanceItem(o.optString("n"), o.optInt("k"), o.optInt("m"), o.optInt("dk"), o.optLong("dt"))
    }
}

/**
 * Carnet d'entretien : compteur kilométrique (mis à jour tout seul avec les km parcourus avec ROADGUARD),
 * vidange, contrôle technique, pneus, freins, assurance… avec ce qu'il reste avant chaque échéance.
 */
class MaintenanceLog(context: Context, private val trip: TripComputer) {
    private val prefs = context.applicationContext.getSharedPreferences("maintenance", Context.MODE_PRIVATE)
    var items by mutableStateOf(load()); private set

    /** Compteur actuel = valeur saisie + distance parcourue depuis la saisie. */
    val odometer: Int get() {
        val base = prefs.getInt("odo", 0)
        val at = prefs.getFloat("odo_trip_m", 0f)
        return if (base == 0) 0 else base + ((trip.total.distanceM - at) / 1000).toInt().coerceAtLeast(0)
    }

    fun setOdometer(km: Int) { prefs.edit().putInt("odo", km).putFloat("odo_trip_m", trip.total.distanceM.toFloat()).apply(); items = items.toList() }

    private fun load(): List<MaintenanceItem> = prefs.getString("items", null)?.let { raw ->
        runCatching { val a = JSONArray(raw); (0 until a.length()).map { MaintenanceItem.from(a.getJSONObject(it)) } }.getOrNull()
    } ?: listOf(
        MaintenanceItem("Vidange et filtres", 15_000, 12, 0, System.currentTimeMillis()),
        MaintenanceItem("Contrôle technique", 0, 24, 0, System.currentTimeMillis()),
        MaintenanceItem("Pneus", 40_000, 0, 0, System.currentTimeMillis()),
        MaintenanceItem("Plaquettes de frein", 30_000, 0, 0, System.currentTimeMillis()),
        MaintenanceItem("Courroie de distribution", 120_000, 72, 0, System.currentTimeMillis()),
        MaintenanceItem("Assurance", 0, 12, 0, System.currentTimeMillis()),
    )

    private fun save(l: List<MaintenanceItem>) { items = l; prefs.edit().putString("items", JSONArray(l.map { it.toJson() }).toString()).apply() }
    fun done(i: Int) = save(items.mapIndexed { j, it -> if (j == i) it.copy(doneKm = odometer, doneAt = System.currentTimeMillis()) else it })
    fun remove(i: Int) = save(items.filterIndexed { j, _ -> j != i })
    fun add(item: MaintenanceItem) = save(items + item)
    fun urgentCount() = items.count { it.urgent(odometer) }
}

@Composable
private fun NumberField(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value, onValueChange = { v -> onChange(v.filter { it.isDigit() }.take(7)) }, singleLine = true, label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 18.sp),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DriveColors.Accent, unfocusedBorderColor = DriveColors.Track,
            focusedLabelColor = DriveColors.Accent, unfocusedLabelColor = DriveColors.TextSecondary),
        modifier = modifier,
    )
}

@Composable
fun MaintenanceScreen(trip: TripComputer, onClose: () -> Unit) {
    val context = LocalContext.current
    val log = remember { MaintenanceLog(context, trip) }
    var odoField by remember { mutableStateOf(log.odometer.takeIf { it > 0 }?.toString() ?: "") }
    var name by remember { mutableStateOf("") }
    var everyKm by remember { mutableStateOf("") }
    var everyMonths by remember { mutableStateOf("") }
    val odo = log.odometer
    Column(Modifier.fillMaxSize().background(DriveColors.Background).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Carnet d'entretien", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            BigButton("Fermer", DriveColors.Panel, onClose)
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NumberField(odoField, { odoField = it }, "Compteur de la voiture (km)", Modifier.weight(1f))
                BigButton("Enregistrer", DriveColors.Accent) { odoField.toIntOrNull()?.let { log.setOdometer(it) } }
            }
            Text(if (odo > 0) "Compteur : $odo km (mis à jour avec les km parcourus)" else "Entre le kilométrage affiché sur ta voiture pour suivre les échéances.",
                color = DriveColors.TextSecondary, fontSize = 14.sp)
            log.items.forEachIndexed { i, it ->
                val km = it.remainingKm(odo)
                val days = it.remainingDays()
                val parts = listOfNotNull(
                    km?.let { k -> if (odo == 0) null else if (k < 0) "dépassé de ${-k} km" else "dans $k km" },
                    days?.let { d -> if (d < 0) "en retard de ${-d} jours" else if (d < 60) "dans $d jours" else "le ${java.text.SimpleDateFormat("MMM yyyy", java.util.Locale.FRANCE).format(java.util.Date(it.dueAt()!!))}" },
                )
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (it.urgent(odo)) Color(0xFF5D2E00) else DriveColors.Panel).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text((if (it.urgent(odo)) "⚠️ " else "🔧 ") + it.name, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Text(listOfNotNull(if (it.everyKm > 0) "tous les ${it.everyKm} km" else null, if (it.everyMonths > 0) "tous les ${it.everyMonths} mois" else null).joinToString(" ou ") +
                        (if (parts.isNotEmpty()) " · prochain : " + parts.joinToString(", ") else ""), color = DriveColors.TextSecondary, fontSize = 14.sp)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BigButton("✓ Fait aujourd'hui", DriveColors.Accent) { log.done(i) }
                        BigButton("Retirer", DriveColors.Track) { log.remove(i) }
                    }
                }
            }
            Text("Ajouter un entretien", color = DriveColors.Accent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            OutlinedTextField(value = name, onValueChange = { name = it.take(40) }, singleLine = true, label = { Text("Nom (ex. liquide de frein)") },
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 18.sp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DriveColors.Accent, unfocusedBorderColor = DriveColors.Track,
                    focusedLabelColor = DriveColors.Accent, unfocusedLabelColor = DriveColors.TextSecondary),
                modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NumberField(everyKm, { everyKm = it }, "Tous les … km", Modifier.weight(1f))
                NumberField(everyMonths, { everyMonths = it }, "Tous les … mois", Modifier.weight(1f))
            }
            BigButton("＋ Ajouter", DriveColors.Accent) {
                val k = everyKm.toIntOrNull() ?: 0; val m = everyMonths.toIntOrNull() ?: 0
                if (name.isNotBlank() && (k > 0 || m > 0)) {
                    log.add(MaintenanceItem(name.trim(), k, m, odo, System.currentTimeMillis())); name = ""; everyKm = ""; everyMonths = ""
                }
            }
        }
    }
}
