package fr.roadguard.core.radar

import android.content.Context
import fr.roadguard.core.net.Downloader
import org.json.JSONObject
import java.io.File
import java.io.IOException

/**
 * Pays proposés en plus des bases embarquées (France, Suisse, Belgique, Allemagne, Italie, Espagne, Luxembourg).
 * Chaque téléchargement réunit deux bases ouvertes : MapAtlas Camera Index (CC BY 4.0, 106 pays)
 * et OpenStreetMap (© contributeurs OpenStreetMap, ODbL), sans doublons.
 */
object RadarCountries {
    val available: List<Pair<String, String>> = listOf(
        "ZA" to "Afrique du Sud",
        "AL" to "Albanie",
        "DZ" to "Algérie",
        "AD" to "Andorre",
        "SA" to "Arabie saoudite",
        "AR" to "Argentine",
        "AM" to "Arménie",
        "AU" to "Australie",
        "AT" to "Autriche",
        "AZ" to "Azerbaïdjan",
        "BH" to "Bahreïn",
        "BY" to "Biélorussie",
        "BA" to "Bosnie-Herzégovine",
        "BR" to "Brésil",
        "BG" to "Bulgarie",
        "CA" to "Canada",
        "CL" to "Chili",
        "CY" to "Chypre",
        "CO" to "Colombie",
        "KR" to "Corée du Sud",
        "HR" to "Croatie",
        "DK" to "Danemark",
        "EG" to "Égypte",
        "AE" to "Émirats arabes unis",
        "EC" to "Équateur",
        "EE" to "Estonie",
        "US" to "États-Unis",
        "FJ" to "Fidji",
        "FI" to "Finlande",
        "GI" to "Gibraltar",
        "GR" to "Grèce",
        "GP" to "Guadeloupe",
        "GT" to "Guatemala",
        "GF" to "Guyane",
        "GE" to "Géorgie",
        "HK" to "Hong Kong",
        "HU" to "Hongrie",
        "IN" to "Inde",
        "IQ" to "Irak",
        "IR" to "Iran",
        "IE" to "Irlande",
        "IS" to "Islande",
        "IL" to "Israël",
        "JP" to "Japon",
        "JO" to "Jordanie",
        "KZ" to "Kazakhstan",
        "KG" to "Kirghizistan",
        "XK" to "Kosovo",
        "KW" to "Koweït",
        "RE" to "La Réunion",
        "LV" to "Lettonie",
        "LB" to "Liban",
        "LI" to "Liechtenstein",
        "LT" to "Lituanie",
        "MK" to "Macédoine du Nord",
        "MY" to "Malaisie",
        "MT" to "Malte",
        "MA" to "Maroc",
        "MQ" to "Martinique",
        "MU" to "Maurice",
        "MX" to "Mexique",
        "MD" to "Moldavie",
        "MC" to "Monaco",
        "MN" to "Mongolie",
        "ME" to "Monténégro",
        "NA" to "Namibie",
        "NO" to "Norvège",
        "NC" to "Nouvelle-Calédonie",
        "NZ" to "Nouvelle-Zélande",
        "OM" to "Oman",
        "UZ" to "Ouzbékistan",
        "PK" to "Pakistan",
        "PA" to "Panama",
        "PY" to "Paraguay",
        "NL" to "Pays-Bas",
        "PH" to "Philippines",
        "PL" to "Pologne",
        "PF" to "Polynésie française",
        "PT" to "Portugal",
        "PE" to "Pérou",
        "QA" to "Qatar",
        "RO" to "Roumanie",
        "GB" to "Royaume-Uni",
        "RU" to "Russie",
        "RW" to "Rwanda",
        "SM" to "Saint-Marin",
        "RS" to "Serbie",
        "SG" to "Singapour",
        "SK" to "Slovaquie",
        "SI" to "Slovénie",
        "SE" to "Suède",
        "TW" to "Taïwan",
        "CZ" to "Tchéquie",
        "TH" to "Thaïlande",
        "TT" to "Trinité-et-Tobago",
        "TN" to "Tunisie",
        "TR" to "Turquie",
        "UA" to "Ukraine",
        "UY" to "Uruguay",
        "VN" to "Viêt Nam",
        "ZM" to "Zambie",
        "ZW" to "Zimbabwe",
    )

    fun file(context: Context, code: String) = File(RadarRepository.extraDir(context), "radars_extra_${code.lowercase()}.csv")
    fun installed(context: Context, code: String) = file(context, code).isFile

    /**
     * Télécharge les radars du pays : MapAtlas Camera Index (si le pays y figure) + OpenStreetMap,
     * fusionnés (un point OSM à moins de 50 m d'un radar MapAtlas est un doublon). Renvoie le nombre de radars.
     */
    suspend fun download(context: Context, code: String, onProgress: (Long) -> Unit): Int {
        val raw = File(RadarRepository.extraDir(context), "mapatlas_${code.lowercase()}.geojson")
        val atlas = runCatching {
            Downloader.download("https://mapatlas.eu/data/camera-index/${code.lowercase()}.geojson", raw, 0, null) { d, _ -> onProgress(d) }
            convert(raw.readText(), code)
        }.getOrNull().also { raw.delete() }
        val osm = runCatching { fromOsm(code) }.getOrNull()
        val rows = ArrayList<String>()
        val taken = HashSet<Long>()
        fun cell(lat: Double, lon: Double) = (Math.floor(lat / 0.0005).toLong() shl 32) or (Math.floor(lon / 0.0005).toLong() and 0xffffffffL)
        fun near(lat: Double, lon: Double): Boolean {
            val cy = Math.floor(lat / 0.0005).toLong(); val cx = Math.floor(lon / 0.0005).toLong()
            for (dy in -1L..1L) for (dx in -1L..1L) if (((cy + dy) shl 32) or ((cx + dx) and 0xffffffffL) in taken) return true
            return false
        }
        atlas?.first?.lineSequence()?.drop(1)?.filter { it.isNotBlank() }?.forEach { line ->
            val f = line.split(';'); rows += line
            f.getOrNull(2)?.toDoubleOrNull()?.let { lat -> f.getOrNull(3)?.toDoubleOrNull()?.let { lon -> taken += cell(lat, lon) } }
        }
        osm?.forEach { (line, lat, lon) -> if (!near(lat, lon)) { rows += line; taken += cell(lat, lon) } }
        if (rows.isEmpty()) {
            if (atlas == null && osm == null) throw IOException("Téléchargement impossible : vérifie la connexion")
            throw IOException("Aucun radar connu pour ce pays")
        }
        file(context, code).writeText(HEADER + rows.joinToString("\n", postfix = "\n"))
        RadarRepository.invalidate()
        return rows.size
    }

    private const val HEADER = "id;type;lat;lon;country;category;speed_kmh;direction_deg;status;sources;source_ids;speed_conflicts\n"

    /** Radars OpenStreetMap du pays (highway=speed_camera) via l'API Overpass : (ligne CSV, lat, lon). */
    private suspend fun fromOsm(code: String): List<Triple<String, Double, Double>> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val q = "[out:json][timeout:180];area[\"ISO3166-1\"=\"$code\"]->.a;node[\"highway\"=\"speed_camera\"](area.a);out body;"
        val c = java.net.URL("https://overpass-api.de/api/interpreter").openConnection() as java.net.HttpURLConnection
        c.connectTimeout = 20_000; c.readTimeout = 200_000; c.requestMethod = "POST"; c.doOutput = true
        c.setRequestProperty("User-Agent", "Pilote/1 (radars)")
        c.outputStream.use { it.write(("data=" + java.net.URLEncoder.encode(q, "UTF-8")).toByteArray()) }
        if (c.responseCode != 200) throw IOException("OpenStreetMap : erreur ${c.responseCode}")
        val els = JSONObject(c.inputStream.bufferedReader().use { it.readText() }).optJSONArray("elements") ?: return@withContext emptyList()
        (0 until els.length()).mapNotNull { i ->
            val e = els.optJSONObject(i) ?: return@mapNotNull null
            val lat = e.optDouble("lat", Double.NaN); val lon = e.optDouble("lon", Double.NaN)
            if (!lat.isFinite() || !lon.isFinite()) return@mapNotNull null
            val t = e.optJSONObject("tags") ?: JSONObject()
            val speed = t.optString("maxspeed").filter(Char::isDigit).toIntOrNull()?.takeIf { it in 5..150 }?.toString().orEmpty()
            val dir = t.optString("direction").toIntOrNull()?.takeIf { it in 0..360 }?.toString().orEmpty()
            val id = e.optLong("id")
            Triple("osm-${code.lowercase()}-$id;Radar fixe (OpenStreetMap);$lat;$lon;$code;fixed;$speed;$dir;inventory;osm;osm:$id;", lat, lon)
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
