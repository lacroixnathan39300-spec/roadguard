package fr.roadguard.drive.nav

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
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

    suspend fun route(from: LatLon, headingDeg: Double?, to: LatLon): Route = withContext(Dispatchers.IO) {
        val body = URLEncoder.encode(ValhallaParser.request(from, headingDeg, to), "UTF-8")
        ValhallaParser.parse(get("$ROUTE_URL?json=$body"), to)
    }

    private fun get(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 10_000
        c.readTimeout = 20_000
        c.setRequestProperty("User-Agent", USER_AGENT)
        c.setRequestProperty("Accept", "application/json")
        try {
            val code = c.responseCode
            val stream = if (code in 200..299) c.inputStream else c.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                val reason = runCatching { JSONObject(text).optString("error") }.getOrNull().orEmpty()
                throw IOException(if (reason.isNotBlank()) reason else "Service indisponible ($code)")
            }
            return text
        } finally {
            c.disconnect()
        }
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
