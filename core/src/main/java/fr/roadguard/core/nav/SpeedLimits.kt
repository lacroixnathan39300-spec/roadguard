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

    /**
     * Avance des limitations (pas des radars) : on lit la limite du tronçon situé 50 m devant la voiture,
     * pour qu'un changement de limitation soit pris 50 m plus tôt qu'avant (demande de Nathan).
     */
    const val AHEAD_M = 50.0

    /** Portion de route (identifiant OpenStreetMap) où roule la voiture : sert aux corrections de limitation signalées. */
    @Volatile var lastWay: String? = null
        private set
    /** Portion de route 50 m devant (celle dont la limite est lue). */
    @Volatile var aheadWay: String? = null
        private set

    /**
     * Limite du tronçon où l'on sera dans 50 m ([ahead], point devant la voiture) ou, sans ce point, du tronçon où l'on roule ;
     * 0 si ce tronçon n'a pas de limite connue ; null si la requête a échoué.
     */
    suspend fun current(trail: List<LatLon>, ahead: LatLon? = null): Int? = withContext(Dispatchers.IO) {
        if (trail.size < 2) return@withContext null
        runCatching {
            val json = post(request(if (ahead != null) trail + ahead else trail))
            lastWay = wayOf(json, trail.size - 1)
            aheadWay = wayOf(json)
            parse(json)
        }.getOrNull()
    }

    /**
     * Identifiant OpenStreetMap de la portion de route où se trouve le point [index] de la trace (la voiture),
     * ou du dernier tronçon (index -1, ou point non recalé sur une route) ; null si inconnu.
     */
    fun wayOf(json: String, index: Int = -1): String? {
        val o = JSONObject(json)
        val edges = o.optJSONArray("edges") ?: return null
        if (edges.length() == 0) return null
        val pts = o.optJSONArray("matched_points")
        val ei = if (index >= 0 && pts != null && index < pts.length()) pts.optJSONObject(index)?.takeIf { it.optString("type") != "unmatched" }?.optLong("edge_index", -1L) ?: -1L else -1L
        val e = edges.optJSONObject(if (ei in 0L until edges.length().toLong()) ei.toInt() else edges.length() - 1)
        return e?.optLong("way_id", 0L)?.takeIf { it > 0 }?.toString()
    }

    fun request(trail: List<LatLon>): String {
        val shape = JSONArray()
        trail.forEach { shape.put(JSONObject().put("lat", it.lat).put("lon", it.lon)) }
        return JSONObject()
            .put("shape", shape)
            .put("costing", "auto")
            .put("shape_match", "map_snap")
            .put("filters", JSONObject().put("attributes", JSONArray().put("edge.speed_limit").put("edge.road_class").put("edge.way_id")
                .put("matched.type").put("matched.edge_index")).put("action", "include"))
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
