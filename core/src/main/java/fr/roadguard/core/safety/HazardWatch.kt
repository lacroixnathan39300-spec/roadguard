package fr.roadguard.core.safety

import fr.roadguard.core.net.Net
import fr.roadguard.core.radar.Fix
import fr.roadguard.core.radar.Geo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Dangers de la route d'après OpenStreetMap : passages à niveau, virages dangereux (panneaux A1),
 * écoles (aux heures d'entrée et de sortie). Chargés par zones de 3 km, annoncés une fois quand ils sont devant.
 */
class HazardWatch(private val scope: CoroutineScope) {
    enum class Kind { CROSSING, CURVE, SCHOOL }
    data class Item(val id: String, val kind: Kind, val lat: Double, val lon: Double)

    @Volatile private var items: List<Item> = emptyList()
    private var loadedLat = Double.NaN; private var loadedLon = Double.NaN; private var loadedAt = 0L
    @Volatile private var busy = false
    private val announced = HashMap<String, Long>()

    /** Renvoie la phrase à dire pour un danger devant, ou null. */
    fun onFix(f: Fix, now: Long = System.currentTimeMillis()): String? {
        if (f.bearing.isNaN() || f.speedKmh < 15) return null
        if (!busy && (loadedLat.isNaN() || Geo.distanceMeters(f.lat, f.lon, loadedLat, loadedLon) > 2_000 || now - loadedAt > 20 * 60_000)) load(f.lat, f.lon, now)
        announced.entries.removeAll { now - it.value > 30 * 60_000 }
        for (it in items) {
            if (it.id in announced) continue
            val d = Geo.distanceMeters(f.lat, f.lon, it.lat, it.lon)
            val range = when (it.kind) { Kind.CROSSING -> 350.0; Kind.CURVE -> 300.0; Kind.SCHOOL -> 250.0 }
            if (d > range || d < 30) continue
            val off = Geo.headingDelta(f.bearing, Geo.bearing(f.lat, f.lon, it.lat, it.lon))
            if (off > 50 || d * kotlin.math.sin(Math.toRadians(off)) > 40) continue
            if (it.kind == Kind.CURVE && f.speedKmh < 50) continue
            if (it.kind == Kind.SCHOOL && !schoolTime(now)) continue
            announced[it.id] = now
            val m = ((d + 25) / 50).toInt() * 50
            return when (it.kind) {
                Kind.CROSSING -> "Passage à niveau à $m mètres"
                Kind.CURVE -> "Virage dangereux à $m mètres, ralentis"
                Kind.SCHOOL -> "École à proximité, attention aux enfants"
            }
        }
        return null
    }

    private fun load(lat: Double, lon: Double, now: Long) {
        busy = true; loadedLat = lat; loadedLon = lon; loadedAt = now
        scope.launch(Dispatchers.IO) {
            runCatching {
                val q = "[out:json][timeout:25];(" +
                    "node[\"railway\"=\"level_crossing\"](around:3000,$lat,$lon);" +
                    "node[\"hazard\"~\"curve\"](around:3000,$lat,$lon);" +
                    "node[\"traffic_sign\"~\"FR:A1\"](around:3000,$lat,$lon);" +
                    "node[\"amenity\"=\"school\"](around:3000,$lat,$lon);way[\"amenity\"=\"school\"](around:3000,$lat,$lon););out center;"
                val r = Net.fetch("https://overpass-api.de/api/interpreter", "POST",
                    ("data=" + java.net.URLEncoder.encode(q, "UTF-8")).toByteArray(), "application/x-www-form-urlencoded")
                if (r.code != 200) return@runCatching
                val els = JSONObject(String(r.body)).optJSONArray("elements") ?: return@runCatching
                items = (0 until els.length()).mapNotNull { i ->
                    val e = els.getJSONObject(i)
                    val t = e.optJSONObject("tags") ?: JSONObject()
                    val c = e.optJSONObject("center")
                    val la = c?.optDouble("lat") ?: e.optDouble("lat", Double.NaN)
                    val lo = c?.optDouble("lon") ?: e.optDouble("lon", Double.NaN)
                    if (!la.isFinite() || !lo.isFinite()) return@mapNotNull null
                    val kind = when {
                        t.optString("railway") == "level_crossing" -> Kind.CROSSING
                        t.optString("amenity") == "school" -> Kind.SCHOOL
                        else -> Kind.CURVE
                    }
                    Item("${e.optString("type")}${e.optLong("id")}", kind, la, lo)
                }
            }
            busy = false
        }
    }

    companion object {
        /** Heures d'école (lundi, mardi, jeudi, vendredi ; mercredi matin) : entrée, midi, sortie. */
        fun schoolTime(now: Long): Boolean {
            val c = java.util.Calendar.getInstance().apply { timeInMillis = now }
            val day = c.get(java.util.Calendar.DAY_OF_WEEK)
            if (day == java.util.Calendar.SATURDAY || day == java.util.Calendar.SUNDAY) return false
            val m = c.get(java.util.Calendar.HOUR_OF_DAY) * 60 + c.get(java.util.Calendar.MINUTE)
            val morning = m in (7 * 60 + 45)..(8 * 60 + 45)
            if (day == java.util.Calendar.WEDNESDAY) return morning || m in (11 * 60 + 15)..(12 * 60 + 30)
            return morning || m in (11 * 60 + 15)..(13 * 60 + 45) || m in (16 * 60)..(17 * 60 + 30)
        }
    }
}
