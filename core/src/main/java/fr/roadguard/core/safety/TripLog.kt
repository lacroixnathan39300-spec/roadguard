package fr.roadguard.core.safety

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Un trajet terminé (historique, journal kilométrique, éco-conduite). */
data class TripRecord(
    val start: Long, val end: Long, val km: Double, val maxKmh: Int, val avgKmh: Int,
    val overSeconds: Int, val hardBrakes: Int, val hardAccels: Int, val radars: Int,
    val from: String?, val to: String?, val pro: Boolean, val ecoScore: Int,
) {
    fun toJson(): JSONObject = JSONObject().put("start", start).put("end", end).put("km", km).put("max", maxKmh).put("avg", avgKmh)
        .put("over", overSeconds).put("brakes", hardBrakes).put("accels", hardAccels).put("radars", radars)
        .put("from", from ?: "").put("to", to ?: "").put("pro", pro).put("eco", ecoScore)

    companion object {
        fun from(o: JSONObject) = TripRecord(o.optLong("start"), o.optLong("end"), o.optDouble("km"), o.optInt("max"), o.optInt("avg"),
            o.optInt("over"), o.optInt("brakes"), o.optInt("accels"), o.optInt("radars"),
            o.optString("from").ifBlank { null }, o.optString("to").ifBlank { null }, o.optBoolean("pro"), o.optInt("eco"))

        /**
         * Note d'éco-conduite sur 100 : on retire des points pour chaque freinage ou accélération brusque
         * (par 10 km), et pour le temps passé au-dessus des limitations.
         */
        fun eco(km: Double, brakes: Int, accels: Int, overSeconds: Int, minutes: Double): Int {
            val per10 = if (km > 0.5) 10.0 / km else 1.0
            val penalty = (brakes * 6 + accels * 4) * per10 + (overSeconds / 60.0) / minutes.coerceAtLeast(1.0) * 100 * 0.6
            return (100 - penalty).toInt().coerceIn(0, 100)
        }
    }
}

/** Plein de carburant (carnet). */
data class FuelEntry(val at: Long, val liters: Double, val price: Double, val odometer: Int, val full: Boolean) {
    fun toJson(): JSONObject = JSONObject().put("at", at).put("l", liters).put("p", price).put("km", odometer).put("full", full)
    companion object { fun from(o: JSONObject) = FuelEntry(o.optLong("at"), o.optDouble("l"), o.optDouble("p"), o.optInt("km"), o.optBoolean("full", true)) }
}

/** Voiture garée (position, heure, adresse) et minuteur de stationnement. */
data class Parking(val lat: Double, val lon: Double, val at: Long, val address: String?, val timerEnd: Long)

/** Historique des trajets, compteur kilométrique, carnet de carburant et stationnement. */
class TripLog(context: Context) {
    private val dir = File(context.filesDir, "triplog").apply { mkdirs() }
    private val prefs = context.getSharedPreferences("triplog", Context.MODE_PRIVATE)

    private fun readArray(name: String): JSONArray = runCatching { JSONArray(File(dir, name).readText()) }.getOrDefault(JSONArray())
    private fun writeArray(name: String, a: JSONArray) { File(dir, "$name.tmp").apply { writeText(a.toString()); renameTo(File(dir, name)) } }

    fun trips(): List<TripRecord> = readArray("trips.json").let { a -> (0 until a.length()).map { TripRecord.from(a.getJSONObject(it)) } }.sortedByDescending { it.start }
    fun addTrip(t: TripRecord) {
        val a = readArray("trips.json"); a.put(t.toJson())
        // Au plus 1 000 trajets (les plus anciens partent).
        val keep = if (a.length() > 1000) JSONArray((a.length() - 1000 until a.length()).map { a.get(it) }) else a
        writeArray("trips.json", keep)
        odometerKm += t.km
    }
    fun setPro(start: Long, pro: Boolean) {
        val a = readArray("trips.json")
        for (i in 0 until a.length()) { val o = a.getJSONObject(i); if (o.optLong("start") == start) o.put("pro", pro) }
        writeArray("trips.json", a)
    }
    fun deleteTrip(start: Long) {
        val a = readArray("trips.json")
        writeArray("trips.json", JSONArray((0 until a.length()).map { a.getJSONObject(it) }.filter { it.optLong("start") != start }))
    }

    /** Kilomètres parcourus avec Pilote (pour les rappels d'entretien). */
    var odometerKm: Double
        get() = prefs.getFloat("odometer", 0f).toDouble()
        set(v) { prefs.edit().putFloat("odometer", v.toFloat()).apply() }

    fun fuel(): List<FuelEntry> = readArray("fuel.json").let { a -> (0 until a.length()).map { FuelEntry.from(a.getJSONObject(it)) } }.sortedBy { it.at }
    fun addFuel(e: FuelEntry) { val a = readArray("fuel.json"); a.put(e.toJson()); writeArray("fuel.json", a) }
    fun deleteFuel(at: Long) {
        val a = readArray("fuel.json")
        writeArray("fuel.json", JSONArray((0 until a.length()).map { a.getJSONObject(it) }.filter { it.optLong("at") != at }))
    }

    var parking: Parking?
        get() = if (!prefs.contains("p_lat")) null else Parking(prefs.getFloat("p_lat", 0f).toDouble(), prefs.getFloat("p_lon", 0f).toDouble(),
            prefs.getLong("p_at", 0), prefs.getString("p_addr", null), prefs.getLong("p_timer", 0))
        set(v) {
            val e = prefs.edit()
            if (v == null) e.remove("p_lat").remove("p_lon").remove("p_at").remove("p_addr").remove("p_timer")
            else e.putFloat("p_lat", v.lat.toFloat()).putFloat("p_lon", v.lon.toFloat()).putLong("p_at", v.at).putString("p_addr", v.address).putLong("p_timer", v.timerEnd)
            e.apply()
        }

    /** Journal kilométrique au format CSV (tableur), pour les frais réels ou la facturation. */
    fun csv(trips: List<TripRecord>): String {
        val df = java.text.SimpleDateFormat("dd/MM/yyyy;HH:mm", java.util.Locale.FRANCE)
        val sb = StringBuilder("Date;Départ;Arrivée heure;Type;Km;Durée (min);Vitesse moyenne;De;Vers\n")
        trips.sortedBy { it.start }.forEach { t ->
            sb.append(df.format(java.util.Date(t.start))).append(';')
                .append(java.text.SimpleDateFormat("HH:mm", java.util.Locale.FRANCE).format(java.util.Date(t.end))).append(';')
                .append(if (t.pro) "Pro" else "Perso").append(';')
                .append(String.format(java.util.Locale.FRANCE, "%.1f", t.km)).append(';')
                .append((t.end - t.start) / 60_000).append(';').append(t.avgKmh).append(';')
                .append((t.from ?: "").replace(';', ',')).append(';').append((t.to ?: "").replace(';', ',')).append('\n')
        }
        return sb.toString()
    }

    /** Consommation moyenne (L/100 km) entre pleins complets, et coût au km. */
    fun consumption(): Pair<Double?, Double?> {
        val f = fuel().filter { it.odometer > 0 }
        val fulls = f.filter { it.full }
        if (fulls.size < 2) return null to null
        val first = fulls.first(); val last = fulls.last()
        val km = (last.odometer - first.odometer).toDouble()
        if (km <= 0) return null to null
        val between = f.filter { it.at > first.at && it.at <= last.at }
        val liters = between.sumOf { it.liters }; val cost = between.sumOf { it.price }
        return liters / km * 100 to cost / km
    }
}

/** Mesures du trajet en cours (distance, vitesses, conduite brusque, excès). */
class TripMeter {
    var start = 0L; private set
    var km = 0.0; private set
    var maxKmh = 0; private set
    var overSeconds = 0; private set
    var hardBrakes = 0; private set
    var hardAccels = 0; private set
    var radars = 0
    var from: String? = null
    private var lastLat = Double.NaN; private var lastLon = 0.0; private var lastT = 0L; private var lastKmh = 0.0
    private var lastHardAt = 0L

    val active get() = start != 0L

    fun onFix(lat: Double, lon: Double, kmh: Double, over: Boolean, t: Long, address: String?) {
        if (start == 0L) { if (kmh < 15) return; start = t; from = address }
        if (!lastLat.isNaN()) {
            val dt = (t - lastT) / 1000.0
            if (dt in 0.3..10.0) {
                val d = fr.roadguard.core.radar.Geo.distanceMeters(lastLat, lastLon, lat, lon)
                if (d / dt < 70) km += d / 1000.0
                val dv = (kmh - lastKmh) / dt
                // Une manœuvre brusque n'est comptée qu'une fois (3 s d'écart au moins).
                if (t - lastHardAt > 3_000) {
                    if (dv < -12) { hardBrakes++; lastHardAt = t } else if (dv > 10) { hardAccels++; lastHardAt = t }
                }
                if (over) overSeconds += dt.toInt().coerceAtLeast(1)
            }
        }
        lastLat = lat; lastLon = lon; lastT = t; lastKmh = kmh
        if (kmh > maxKmh) maxKmh = kmh.toInt()
    }

    fun finish(end: Long, to: String?, pro: Boolean): TripRecord? {
        if (start == 0L) return null
        val minutes = (end - start) / 60_000.0
        val r = if (km < 0.5 || minutes < 1) null else TripRecord(start, end, km, maxKmh, (km / (minutes / 60.0)).toInt(), overSeconds,
            hardBrakes, hardAccels, radars, from, to, pro, TripRecord.eco(km, hardBrakes, hardAccels, overSeconds, minutes))
        start = 0; km = 0.0; maxKmh = 0; overSeconds = 0; hardBrakes = 0; hardAccels = 0; radars = 0; from = null; lastLat = Double.NaN
        return r
    }
}
