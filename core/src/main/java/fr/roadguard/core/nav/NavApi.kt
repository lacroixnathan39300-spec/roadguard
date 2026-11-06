package fr.roadguard.core.nav

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder

/** Un résultat de recherche d'adresse ou de lieu. */
data class Place(val name: String, val detail: String, val point: LatLon)

/**
 * Services en ligne libres et sans clé :
 * - recherche : Photon (komoot, données OpenStreetMap) ;
 * - itinéraire : Valhalla de FOSSGIS (OpenStreetMap), consignes en français.
 * Usage personnel raisonnable, attribution OpenStreetMap affichée sur la carte.
 */
object NavApi {
    private const val USER_AGENT = "ROADGUARD-DRIVE/2.0 (usage personnel)"
    private const val SEARCH_URL = "https://photon.komoot.io/api/"
    private const val ROUTE_URL = "https://valhalla1.openstreetmap.de/route"

    suspend fun search(query: String, near: LatLon?): List<Place> = withContext(Dispatchers.IO) {
        val q = URLEncoder.encode(query.trim(), "UTF-8")
        val bias = near?.let { "&lat=${it.lat}&lon=${it.lon}" }.orEmpty()
        PlaceParser.parse(get("$SEARCH_URL?q=$q&lang=fr&limit=10$bias"))
    }

    suspend fun route(from: LatLon, headingDeg: Double?, to: LatLon): Route = routes(from, headingDeg, to).first()

    /** Trajet le plus rapide puis jusqu'à 2 variantes ; options péages / autoroutes. */
    suspend fun routes(
        from: LatLon, headingDeg: Double?, to: LatLon,
        alternates: Int = 0, avoidTolls: Boolean = false, avoidHighways: Boolean = false,
        vias: List<LatLon> = emptyList(), optimize: Boolean = false, narrow: Boolean = false, eco: Boolean = false,
        language: String = "fr-FR",
    ): List<Route> = withContext(Dispatchers.IO) {
        val body = URLEncoder.encode(ValhallaParser.request(from, headingDeg, to, alternates, avoidTolls, avoidHighways, vias, narrow, eco, language), "UTF-8")
        // Plusieurs étapes, « dans l'ordre le plus rapide » : Valhalla réordonne les étapes (départ et arrivée fixes).
        val url = if (optimize && vias.size >= 2) ROUTE_URL.replace(Regex("/route$"), "/optimized_route") else ROUTE_URL
        ValhallaParser.parseAll(get("$url?json=$body"), to)
    }

    private fun get(url: String): String {
        val r = fr.roadguard.core.net.Net.fetch(url)
        if (r.code !in 200..299) {
            val reason = runCatching { JSONObject(r.text).optString("error") }.getOrNull().orEmpty()
            throw IOException(if (reason.isNotBlank()) reason else "Service indisponible (${r.code})")
        }
        return r.text
    }
}

object PlaceParser {
    fun parse(json: String): List<Place> {
        val features = JSONObject(json).optJSONArray("features") ?: return emptyList()
        return (0 until features.length()).mapNotNull { i ->
            val f = features.getJSONObject(i)
            val coords = f.optJSONObject("geometry")?.optJSONArray("coordinates") ?: return@mapNotNull null
            val p = f.optJSONObject("properties") ?: JSONObject()
            val street = listOf(p.optString("housenumber"), p.optString("street")).filter { it.isNotBlank() }.joinToString(" ")
            val name = p.optString("name").ifBlank { street }.ifBlank { p.optString("city") }
            if (name.isBlank()) return@mapNotNull null
            val city = listOf(p.optString("postcode"), p.optString("city")).filter { it.isNotBlank() }.joinToString(" ")
            val detail = listOf(if (street != name) street else "", city, p.optString("country"))
                .filter { it.isNotBlank() }.joinToString(", ")
            Place(name, detail, LatLon(coords.getDouble(1), coords.getDouble(0)))
        }
    }
}
