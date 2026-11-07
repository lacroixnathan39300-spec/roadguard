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

    /** Portion de route (identifiant OpenStreetMap) de la dernière lecture : sert aux limitations corrigées par la communauté. */
    @Volatile var lastWay: String? = null
        private set

    /** Limite du tronçon où l'on roule ; 0 si ce tronçon n'a pas de limite connue ; null si la requête a échoué. */
    suspend fun current(trail: List<LatLon>): Int? = withContext(Dispatchers.IO) {
        if (trail.size < 2) return@withContext null
        runCatching { val json = post(request(trail)); lastWay = wayOf(json); parse(json) }.getOrNull()
    }

    /** Identifiant OpenStreetMap de la portion de route où l'on roule (dernier tronçon), ou null. */
    fun wayOf(json: String): String? {
        val edges = JSONObject(json).optJSONArray("edges") ?: return null
        if (edges.length() == 0) return null
        return edges.getJSONObject(edges.length() - 1).optLong("way_id", 0L).takeIf { it > 0 }?.toString()
    }

    fun request(trail: List<LatLon>): String {
        val shape = JSONArray()
        trail.forEach { shape.put(JSONObject().put("lat", it.lat).put("lon", it.lon)) }
        return JSONObject()
            .put("shape", shape)
            .put("costing", "auto")
            .put("shape_match", "map_snap")
            .put("filters", JSONObject().put("attributes", JSONArray().put("edge.speed_limit").put("edge.road_class").put("edge.way_id")).put("action", "include"))
            .toString()
    }

    /**
     * Limite du DERNIER tronçon (celui où l'on roule). S'il n'en porte pas, on renvoie 0 (« inconnue ») au lieu de
     * reprendre celle d'un tronçon précédent : en sortant d'un village, l'ancien 50 restait affiché sur la route à 80 / 90.
     */
    fun parse(json: String): Int? {
        val edges = JSONObject(json).optJSONArray("edges") ?: return null
        if (edges.length() == 0) return null
        val last = edges.getJSONObject(edges.length() - 1)
        val limit = last.optInt("speed_limit", 0)
        if (limit in 5..150) return limit
        // Autoroute sans limite renseignée : 130 (règle générale).
        return if (last.optString("road_class") == "motorway") 130 else 0
    }

    private fun post(body: String): String {
        val r = fr.roadguard.core.net.Net.fetch(URL_TRACE, "POST", body.toByteArray(), "application/json")
        if (r.code !in 200..299) error("HTTP ${r.code}")
        return r.text
    }
}

/**
 * Garde une courte trace des dernières positions (points espacés d'au moins 15 m)
 * et décide quand redemander la limite : toutes les 3 s en roulant, pour suivre les changements de zone sans retard.
 */
class SpeedLimitTracker {
    private val trail = ArrayDeque<LatLon>()
    private var lastAskAt = 0L

    fun add(lat: Double, lon: Double, timeMs: Long, speedKmh: Double, intervalMs: Long = 3_000): List<LatLon>? {
        val last = trail.lastOrNull()
        if (last == null || fr.roadguard.core.radar.Geo.distanceMeters(last.lat, last.lon, lat, lon) >= 15) {
            trail.addLast(LatLon(lat, lon))
            while (trail.size > 6) trail.removeFirst()
        }
        if (speedKmh < 8 || trail.size < 3 || timeMs - lastAskAt < intervalMs) return null
        lastAskAt = timeMs
        return trail.toList()
    }

    companion object {
        /** Dépassement signalé au-delà de la tolérance habituelle (5 km/h sous 100, 5 % au-dessus). */
        fun isOver(speedKmh: Int, limit: Int?, margin: Int = -1): Boolean {
            if (limit == null) return false
            val tolerance = if (margin >= 0) margin else if (limit < 100) 5 else (limit * 0.05).toInt()
            return speedKmh > limit + tolerance
        }
    }
}
