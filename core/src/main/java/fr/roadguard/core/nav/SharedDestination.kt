package fr.roadguard.core.nav

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.net.URLDecoder

/**
 * Transforme un texte partagé depuis le téléphone (lien Google Maps, Waze, geo:, adresse d'un contact…)
 * en destination : coordonnées si le texte en contient, sinon recherche de l'adresse.
 */
object SharedDestination {

    suspend fun resolve(text: String, near: LatLon?): Place? {
        coordinates(text)?.let { return Place(label(text) ?: "Destination partagée", "", it) }
        // Lien court (maps.app.goo.gl…) : suivre les redirections pour trouver les coordonnées.
        firstUrl(text)?.let { url ->
            val expanded = withContext(Dispatchers.IO) { runCatching { expand(url) }.getOrNull() }
            expanded?.let { coordinates(it) }?.let { return Place(label(text) ?: "Destination partagée", "", it) }
            expanded?.let { queryFromUrl(it) }?.let { q -> NavApi.search(q, near).firstOrNull()?.let { return it } }
        }
        val query = label(text) ?: return null
        return NavApi.search(query, near).firstOrNull()
    }

    private val patterns = listOf(
        Regex("""!3d(-?\d{1,2}\.\d+)!4d(-?\d{1,3}\.\d+)"""),          // Google Maps (lieu précis)
        Regex("""@(-?\d{1,2}\.\d+),(-?\d{1,3}\.\d+)"""),                // Google Maps (centre de carte)
        Regex("""[?&](?:q|ll|daddr|destination|query)=(-?\d{1,2}\.\d+)(?:,|%2C)\s*(-?\d{1,3}\.\d+)"""),
        Regex("""geo:(-?\d{1,2}\.\d+),(-?\d{1,3}\.\d+)"""),
        Regex("""ll\.(-?\d{1,2}\.\d+)(?:,|%2C)(-?\d{1,3}\.\d+)"""),       // Waze
        Regex("""(?<![\d.])(-?\d{1,2}\.\d{3,}),\s*(-?\d{1,3}\.\d{3,})"""),  // « 46.7187, 5.8335 »
    )

    /** Coordonnées trouvées dans le texte, ou null. */
    fun coordinates(text: String): LatLon? {
        for (p in patterns) {
            val m = p.find(text) ?: continue
            val lat = m.groupValues[1].toDoubleOrNull() ?: continue
            val lon = m.groupValues[2].toDoubleOrNull() ?: continue
            if (lat in -90.0..90.0 && lon in -180.0..180.0 && !(lat == 0.0 && lon == 0.0)) return LatLon(lat, lon)
        }
        return null
    }

    /** Première ligne utile (nom du lieu ou adresse), sans les liens. */
    fun label(text: String): String? = text.lines()
        .map { it.replace(Regex("""https?://\S+"""), "").trim() }
        .firstOrNull { it.isNotEmpty() && coordinatesOnly(it).not() }

    private fun coordinatesOnly(line: String) = line.matches(Regex("""-?\d+\.\d+\s*,\s*-?\d+\.\d+"""))

    fun firstUrl(text: String): String? = Regex("""https?://\S+""").find(text)?.value

    /** Lieu cherché dans un lien du type …/maps/search/Champagnole ou …?q=Champagnole. */
    fun queryFromUrl(url: String): String? {
        Regex("""/maps/(?:search|place)/([^/@?]+)""").find(url)?.let { return decode(it.groupValues[1]) }
        Regex("""[?&]q=([^&]+)""").find(url)?.let { return decode(it.groupValues[1]) }
        return null
    }

    private fun decode(s: String) = URLDecoder.decode(s.replace('+', ' '), "UTF-8").trim().ifEmpty { null }

    private fun expand(start: String): String {
        var url = start
        repeat(5) {
            val r = fr.roadguard.core.net.Net.fetch(url, followRedirects = false)
            val location = if (r.code in 300..399) r.text.ifBlank { null } else null
            if (location == null) return url
            url = URL(URL(url), location).toString()
        }
        return url
    }
}
