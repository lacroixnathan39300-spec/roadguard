package fr.roadguard.core.nav

import android.content.Context
import java.text.Normalizer
import java.util.zip.GZIPInputStream

/**
 * Recherche sans Internet : communes de France embarquées (nom, code postal, centre, population ;
 * source geo.api.gouv.fr, Licence Ouverte). Accents et tirets ignorés, les plus peuplées d'abord.
 */
object OfflinePlaces {
    private class Entry(val key: String, val name: String, val cp: String, val lat: Double, val lon: Double)

    @Volatile private var cache: List<Entry>? = null

    private fun load(context: Context): List<Entry> = cache ?: synchronized(this) {
        cache ?: runCatching {
            val assets = context.applicationContext.assets
            // DRIVE : copie sous « places/ » ; appli GPS : même chemin.
            GZIPInputStream(assets.open("places/communes_fr.csv.gz")).bufferedReader().useLines { lines ->
                lines.drop(1).mapNotNull { l ->
                    val f = l.split(';')
                    if (f.size < 4) null else Entry(normalize(f[0]), f[0], f[1], f[2].toDoubleOrNull() ?: return@mapNotNull null,
                        f[3].toDoubleOrNull() ?: return@mapNotNull null)
                }.toList()
            }
        }.getOrDefault(emptyList()).also { cache = it }
    }

    fun available(context: Context) = load(context).isNotEmpty()

    fun search(context: Context, query: String, limit: Int = 10): List<Place> {
        val q = normalize(query)
        if (q.length < 2) return emptyList()
        val digits = query.filter { it.isDigit() }
        val all = load(context)
        val matches = if (digits.length == 5) all.filter { it.cp == digits }
        else {
            val words = q.split(' ').filter { it.isNotBlank() }
            // Début du nom d'abord, puis le nom contenant tous les mots ; ordre de population conservé.
            all.filter { it.key.startsWith(q) } + all.filter { e -> !e.key.startsWith(q) && words.all { w -> w in e.key } }
        }
        return matches.take(limit).map { Place(it.name, "${it.cp} · hors ligne".trim(), LatLon(it.lat, it.lon)) }
    }

    fun normalize(s: String): String = Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "").replace(Regex("[-'’]"), " ").replace(Regex("\\bst\\b"), "saint")
        .replace(Regex("\\bste\\b"), "sainte").replace(Regex("\\s+"), " ").trim()
}
