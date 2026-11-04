package fr.roadguard.core

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fr.roadguard.core.radar.Geo
import org.json.JSONObject
import java.util.Calendar

/** Totaux d'une période (trajet, jour, semaine, mois, depuis toujours). */
data class TripTotals(
    val distanceM: Double = 0.0,
    val movingS: Double = 0.0,
    val stoppedS: Double = 0.0,
    val maxKmh: Double = 0.0,
    /** Clé de la période (jour, semaine…) : changement → remise à zéro. */
    val key: String = "",
) {
    val avgKmh: Double get() = if (movingS > 30) distanceM / movingS * 3.6 else 0.0

    fun add(d: Double, dt: Double, moving: Boolean, kmh: Double) = copy(
        distanceM = distanceM + d,
        movingS = movingS + if (moving) dt else 0.0,
        stoppedS = stoppedS + if (moving) 0.0 else dt,
        maxKmh = maxOf(maxKmh, kmh),
    )

    fun toJson(): JSONObject = JSONObject().put("d", distanceM).put("m", movingS).put("s", stoppedS).put("x", maxKmh).put("k", key)

    companion object {
        fun fromJson(o: JSONObject?) = if (o == null) TripTotals() else
            TripTotals(o.optDouble("d"), o.optDouble("m"), o.optDouble("s"), o.optDouble("x"), o.optString("k"))
    }
}

/**
 * Ordinateur de bord : distance, temps de conduite et d'arrêt, vitesse maxi et moyenne, altitude,
 * chronos 0-60 / 0-100, par trajet, jour, semaine, mois et au total. Mémorisé sur l'appareil.
 */
class TripComputer(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("trip", Context.MODE_PRIVATE)

    var trip by mutableStateOf(load("trip")); private set
    var day by mutableStateOf(load("day")); private set
    var week by mutableStateOf(load("week")); private set
    var month by mutableStateOf(load("month")); private set
    var total by mutableStateOf(load("total")); private set
    var altitudeM by mutableStateOf<Int?>(null); private set
    var best060 by mutableStateOf(prefs.getFloat("best060", 0f).takeIf { it > 0 }); private set
    var best0100 by mutableStateOf(prefs.getFloat("best0100", 0f).takeIf { it > 0 }); private set
    /** Dernier chrono mesuré (affiché un moment après une accélération). */
    var lastRun by mutableStateOf<String?>(null); private set

    private var lastLat = Double.NaN
    private var lastLon = 0.0
    private var lastTime = 0L
    private var stoppedSince = 0L
    private var launchAt = 0L
    private var got60 = false
    private var lastSave = 0L

    private fun load(k: String) = TripTotals.fromJson(prefs.getString(k, null)?.let { runCatching { JSONObject(it) }.getOrNull() })

    fun onFix(lat: Double, lon: Double, kmh: Double, accuracyM: Double, timeMs: Long, altitude: Double?) {
        altitude?.let { altitudeM = it.toInt() }
        val now = timeMs
        rollPeriods(now)
        if (lastLat.isNaN() || accuracyM > 50) { if (accuracyM <= 50) { lastLat = lat; lastLon = lon; lastTime = now }; return }
        val dt = ((now - lastTime) / 1000.0).coerceIn(0.0, 10.0)
        if (dt <= 0) return
        val moving = kmh >= 4
        // Distance comptée seulement en roulant (le GPS « bouge » un peu à l'arrêt).
        val d = if (moving) Geo.distanceMeters(lastLat, lastLon, lat, lon).takeIf { it < dt * 70 } ?: 0.0 else 0.0
        lastLat = lat; lastLon = lon; lastTime = now

        // Nouveau trajet après 30 min d'arrêt.
        if (!moving) { if (stoppedSince == 0L) stoppedSince = now } else {
            if (stoppedSince != 0L && now - stoppedSince > 30 * 60_000) trip = TripTotals(key = now.toString())
            stoppedSince = 0
        }
        trip = trip.add(d, dt, moving, kmh)
        day = day.add(d, dt, moving, kmh)
        week = week.add(d, dt, moving, kmh)
        month = month.add(d, dt, moving, kmh)
        total = total.add(d, dt, moving, kmh)
        chrono(kmh, now)
        if (now - lastSave > 15_000) save(now)
    }

    /** Chronos 0-60 et 0-100 : départ quand on quitte l'arrêt, arrivée à la vitesse visée. */
    private fun chrono(kmh: Double, now: Long) {
        if (kmh < 3) { launchAt = now; got60 = false; return }
        if (launchAt == 0L) return
        val s = (now - launchAt) / 1000f
        if (s > 40) { launchAt = 0; return }
        if (!got60 && kmh >= 60) {
            got60 = true
            lastRun = "0-60 km/h : %.1f s".format(java.util.Locale.FRANCE, s)
            if (best060 == null || s < best060!!) { best060 = s; prefs.edit().putFloat("best060", s).apply() }
        }
        if (kmh >= 100) {
            lastRun = "0-100 km/h : %.1f s".format(java.util.Locale.FRANCE, s)
            if (best0100 == null || s < best0100!!) { best0100 = s; prefs.edit().putFloat("best0100", s).apply() }
            launchAt = 0
        }
    }

    private fun rollPeriods(now: Long) {
        val c = Calendar.getInstance().apply { timeInMillis = now }
        val dayKey = "${c.get(Calendar.YEAR)}-${c.get(Calendar.DAY_OF_YEAR)}"
        val weekKey = "${c.get(Calendar.YEAR)}-w${c.get(Calendar.WEEK_OF_YEAR)}"
        val monthKey = "${c.get(Calendar.YEAR)}-${c.get(Calendar.MONTH)}"
        if (day.key != dayKey) day = TripTotals(key = dayKey)
        if (week.key != weekKey) week = TripTotals(key = weekKey)
        if (month.key != monthKey) month = TripTotals(key = monthKey)
    }

    fun reset(period: String) {
        when (period) {
            "trip" -> trip = TripTotals(key = System.currentTimeMillis().toString())
            "total" -> { total = TripTotals(); best060 = null; best0100 = null; prefs.edit().remove("best060").remove("best0100").apply() }
        }
        save(System.currentTimeMillis())
    }

    private fun save(now: Long) {
        lastSave = now
        prefs.edit().putString("trip", trip.toJson().toString()).putString("day", day.toJson().toString())
            .putString("week", week.toJson().toString()).putString("month", month.toJson().toString())
            .putString("total", total.toJson().toString()).apply()
    }

    companion object {
        fun duration(s: Double): String {
            val m = (s / 60).toInt()
            return if (m >= 60) "${m / 60} h ${"%02d".format(m % 60)}" else "$m min"
        }
        fun km(m: Double): String = if (m >= 100_000) "%.0f km".format(m / 1000) else "%.1f km".format(java.util.Locale.FRANCE, m / 1000)
    }
}
