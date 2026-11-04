package fr.roadguard.core.radar

import android.content.Context
import fr.roadguard.core.net.Downloader
import org.json.JSONObject
import java.io.File
import java.io.IOException

/** Pays proposés en plus des bases embarquées (France, Suisse, Belgique, Allemagne, Italie, Espagne, Luxembourg). */
object RadarCountries {
    val available: List<Pair<String, String>> = listOf(
        "NL" to "Pays-Bas", "AT" to "Autriche", "PT" to "Portugal", "GB" to "Royaume-Uni", "IE" to "Irlande",
        "PL" to "Pologne", "CZ" to "Tchéquie", "DK" to "Danemark", "SE" to "Suède", "NO" to "Norvège",
        "FI" to "Finlande", "HR" to "Croatie", "SI" to "Slovénie", "HU" to "Hongrie", "SK" to "Slovaquie",
        "GR" to "Grèce", "RO" to "Roumanie", "US" to "États-Unis", "CA" to "Canada", "MA" to "Maroc",
    )

    fun file(context: Context, code: String) = File(RadarRepository.extraDir(context), "radars_extra_${code.lowercase()}.csv")
    fun installed(context: Context, code: String) = file(context, code).isFile

    /**
     * Base communautaire MapAtlas Camera Index du pays (licence CC BY 4.0, © MapAtlas et contributeurs),
     * convertie au format ROADGUARD. Renvoie le nombre de radars.
     */
    suspend fun download(context: Context, code: String, onProgress: (Long) -> Unit): Int {
        val raw = File(RadarRepository.extraDir(context), "mapatlas_${code.lowercase()}.geojson")
        Downloader.download("https://mapatlas.eu/data/camera-index/${code.lowercase()}.geojson", raw, 0, null) { d, _ -> onProgress(d) }
        try {
            val csv = convert(raw.readText(), code)
            if (csv.second == 0) throw IOException("Aucun radar dans la base de ce pays")
            file(context, code).writeText(csv.first)
            RadarRepository.invalidate()
            return csv.second
        } finally {
            raw.delete()
        }
    }

    fun remove(context: Context, code: String) {
        file(context, code).delete()
        RadarRepository.invalidate()
    }

    /** GeoJSON MapAtlas → CSV ROADGUARD (format complet). Renvoie (texte, nombre de points). */
    fun convert(geojson: String, code: String): Pair<String, Int> {
        val root = JSONObject(geojson)
        if (root.optString("type") != "FeatureCollection") throw IOException("Format MapAtlas invalide")
        val features = root.optJSONArray("features") ?: throw IOException("Format MapAtlas invalide")
        val sb = StringBuilder("id;type;lat;lon;country;category;speed_kmh;direction_deg;status;sources;source_ids;speed_conflicts\n")
        var count = 0
        for (i in 0 until features.length()) {
            val f = features.optJSONObject(i) ?: continue
            val g = f.optJSONObject("geometry") ?: continue
            if (g.optString("type") != "Point") continue
            val c = g.optJSONArray("coordinates") ?: continue
            val lon = c.optDouble(0, Double.NaN)
            val lat = c.optDouble(1, Double.NaN)
            if (!lat.isFinite() || !lon.isFinite() || lat !in -90.0..90.0 || lon !in -180.0..180.0) continue
            val p = f.optJSONObject("properties") ?: JSONObject()
            val (category, label) = when (p.optString("class")) {
                "spot" -> "fixed" to "Radar fixe"
                "signal" -> "redlight" to "Radar feu rouge"
                "corridor" -> "average" to "Radar vitesse moyenne"
                "adaptive" -> "variable" to "Radar à vitesse variable"
                else -> continue
            }
            val speed = p.optString("maxspeed").filter(Char::isDigit).toIntOrNull()?.takeIf { it in 5..150 }?.toString().orEmpty()
            val ref = p.optString("ref").ifBlank { "$i" }.replace(Regex("[^A-Za-z0-9_-]"), "_")
            sb.append("mapatlas-${code.lowercase()}-$ref;$label (MapAtlas);$lat;$lon;$code;$category;$speed;;inventory;mapatlas;mapatlas:$ref;\n")
            count++
        }
        return sb.toString() to count
    }
}
