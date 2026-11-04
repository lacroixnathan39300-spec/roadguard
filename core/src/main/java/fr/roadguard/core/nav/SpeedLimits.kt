package fr.roadguard.core.nav

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Limite de vitesse de la route empruntée, d'après OpenStreetMap (Valhalla « trace_attributes »).
 * On envoie les dernières positions, Valhalla les recale sur la route et rend la limite des tronçons.
 */
object SpeedLimits {
    private const val URL_TRACE = "https://valhalla1.openstreetmap.de/trace_attributes"

    /** Limite du dernier tronçon parcouru, ou null si inconnue. */
    suspend fun current(trail: List<LatLon>): Int? = withContext(Dispatchers.IO) {
        if (trail.size < 2) return@withContext null
        runCatching { parse(post(request(trail))) }.getOrNull()
    }

    fun request(trail: List<LatLon>): String {
        val shape = JSONArray()
        trail.forEach { shape.put(JSONObject().put("lat", it.lat).put("lon", it.lon)) }
        return JSONObject()
            .put("shape", shape)
            .put("costing", "auto")
            .put("shape_match", "map_snap")
            .put("filters", JSONObject().put("attributes", JSONArray().put("edge.speed_limit")).put("action", "include"))
            .toString()
    }

    /** Dernier tronçon portant une limite plausible (5 à 150 km/h). */
    fun parse(json: String): Int? {
        val edges = JSONObject(json).optJSONArray("edges") ?: return null
        for (i in edges.length() - 1 downTo 0) {
            val limit = edges.getJSONObject(i).optInt("speed_limit", 0)
            if (limit in 5..150) return limit
        }
        return null
    }

    private fun post(body: String): String {
        val r = fr.roadguard.core.net.Net.fetch(URL_TRACE, "POST", body.toByteArray(), "application/json")
        if (r.code !in 200..299) error("HTTP ${r.code}")
        return r.text
    }
}

/**
 * Garde une courte trace des dernières positions (points espacés d'au moins 25 m)
 * et décide quand redemander la limite : toutes les 12 s en roulant.
 */
class SpeedLimitTracker {
    private val trail = ArrayDeque<LatLon>()
    private var lastAskAt = 0L

    fun add(lat: Double, lon: Double, timeMs: Long, speedKmh: Double): List<LatLon>? {
        val last = trail.lastOrNull()
        if (last == null || fr.roadguard.core.radar.Geo.distanceMeters(last.lat, last.lon, lat, lon) >= 25) {
            trail.addLast(LatLon(lat, lon))
            while (trail.size > 6) trail.removeFirst()
        }
        if (speedKmh < 8 || trail.size < 3 || timeMs - lastAskAt < 12_000) return null
        lastAskAt = timeMs
        return trail.toList()
    }

    companion object {
        /** Dépassement signalé au-delà de la tolérance habituelle (5 km/h sous 100, 5 % au-dessus). */
        fun isOver(speedKmh: Int, limit: Int?): Boolean {
            if (limit == null) return false
            val tolerance = if (limit < 100) 5 else (limit * 0.05).toInt()
            return speedKmh > limit + tolerance
        }
    }
}
