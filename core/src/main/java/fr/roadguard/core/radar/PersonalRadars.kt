package fr.roadguard.core.radar

import android.content.Context
import java.io.File
import java.util.Locale

/**
 * Radars perso : ajoutés à la main (« radar ici ») ou importés d'un fichier CSV (iGO, Garmin, tableur…).
 * Rangés comme un pays de plus (radars_extra_perso.csv), donc affichés et annoncés comme les autres.
 */
object PersonalRadars {
    const val HEADER = "id;type;lat;lon;country;category;speed_kmh;direction_deg;status;sources;source_ids;speed_conflicts"

    fun file(context: Context) = File(RadarRepository.extraDir(context), "radars_extra_perso.csv")

    fun list(context: Context): List<Radar> =
        file(context).takeIf { it.isFile }?.bufferedReader()?.use { RadarCatalog.parse(it) }.orEmpty()

    fun add(context: Context, lat: Double, lon: Double, kind: RadarKind, speedKmh: Int?, directionDeg: Int?): Radar {
        val r = Radar("perso-${System.currentTimeMillis()}", lat, lon, kind, label(kind), speedKmh, directionDeg, category(kind))
        save(context, list(context) + r)
        return r
    }

    fun remove(context: Context, id: String) = save(context, list(context).filterNot { it.id == id })

    fun clear(context: Context) { file(context).delete(); RadarRepository.invalidate() }

    /** Ajoute les radars lus dans [text] ; renvoie (ajoutés, lignes ignorées). Les doublons (< 15 m) sont écartés. */
    fun import(context: Context, text: String): Pair<Int, Int> {
        val (parsed, skipped) = parseCsv(text)
        val existing = list(context)
        val fresh = parsed.filter { p -> existing.none { Geo.distanceMeters(it.lat, it.lon, p.lat, p.lon) < 15 } }
            .distinctBy { "%.4f,%.4f".format(Locale.US, it.lat, it.lon) }
        if (fresh.isNotEmpty()) save(context, existing + fresh)
        return fresh.size to skipped + (parsed.size - fresh.size)
    }

    private fun save(context: Context, radars: List<Radar>) {
        val f = file(context)
        f.parentFile?.mkdirs()
        if (radars.isEmpty()) f.delete() else f.writeText(toCsv(radars))
        RadarRepository.invalidate()
    }

    fun toCsv(radars: List<Radar>): String = buildString {
        append(HEADER).append('\n')
        radars.forEach { r ->
            append(r.id).append(';').append(r.label.replace(';', ',')).append(';')
            append("%.6f".format(Locale.US, r.lat)).append(';').append("%.6f".format(Locale.US, r.lon)).append(";;")
            append(category(r.kind)).append(';').append(r.speedKmh ?: "").append(';').append(r.directionDeg ?: "")
            append(";confirmed;perso;;\n")
        }
    }

    fun label(kind: RadarKind) = when (kind) {
        RadarKind.REDLIGHT -> "Radar feu rouge (perso)"
        RadarKind.AVERAGE, RadarKind.SECTION -> "Vitesse moyenne (perso)"
        RadarKind.MOBILE -> "Contrôle mobile (perso)"
        else -> "Radar fixe (perso)"
    }

    private fun category(kind: RadarKind) = when (kind) {
        RadarKind.REDLIGHT -> "redlight"
        RadarKind.AVERAGE, RadarKind.SECTION -> "average"
        RadarKind.MOBILE -> "mobile"
        else -> "fixed"
    }

    /**
     * Lecture souple : séparateur , ; tabulation ou |, en-tête facultatif (lat/latitude/y, lon/lng/longitude/x,
     * vitesse/speed, type, direction/cap). Format iGO (X,Y,TYPE,SPEED,DIRTYPE,DIRECTION) reconnu.
     * Sans en-tête : latitude, longitude, vitesse (inversées si la latitude est hors limites).
     */
    fun parseCsv(text: String): Pair<List<Radar>, Int> {
        val lines = text.removePrefix(BOM).lines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }
        if (lines.isEmpty()) return emptyList<Radar>() to 0
        val sep = listOf('\t', ';', '|', ',').maxByOrNull { c -> lines.take(5).sumOf { l -> l.count { it == c } } } ?: ','
        fun split(l: String) = l.split(sep).map { it.trim().trim('"').trim() }
        val first = split(lines[0]).map { it.lowercase(Locale.ROOT) }
        val hasHeader = first.any { it.toDoubleOrNull() == null && num(it) == null } &&
            first.any { it in LAT || it in LON }
        val iLat = if (hasHeader) first.indexOfFirst { it in LAT } else 0
        val iLon = if (hasHeader) first.indexOfFirst { it in LON } else 1
        val iSpeed = if (hasHeader) first.indexOfFirst { it in SPEED } else 2
        val iType = if (hasHeader) first.indexOfFirst { it in TYPE } else -1
        val iDir = if (hasHeader) first.indexOfFirst { it in DIR } else -1
        if (iLat < 0 || iLon < 0) return emptyList<Radar>() to lines.size
        val out = ArrayList<Radar>()
        var skipped = 0
        val stamp = System.currentTimeMillis()
        for ((n, line) in lines.drop(if (hasHeader) 1 else 0).withIndex()) {
            val f = split(line)
            var lat = num(f.getOrNull(iLat)); var lon = num(f.getOrNull(iLon))
            if (lat == null || lon == null) { skipped++; continue }
            if (!hasHeader && lat !in -90.0..90.0 && lon in -90.0..90.0) { val t = lat; lat = lon; lon = t }
            if (lat !in -90.0..90.0 || lon !in -180.0..180.0 || (lat == 0.0 && lon == 0.0)) { skipped++; continue }
            val speed = num(f.getOrNull(iSpeed))?.toInt()?.takeIf { it in 5..200 }
            val kind = kindFrom(f.getOrNull(iType).orEmpty())
            val dir = num(f.getOrNull(iDir))?.toInt()?.takeIf { it in 0..360 }
            out += Radar("perso-$stamp-$n", lat, lon, kind, label(kind), speed, dir, category(kind))
        }
        return out to skipped
    }

    private fun num(s: String?): Double? = s?.trim()?.replace(',', '.')?.toDoubleOrNull()?.takeIf { it.isFinite() }

    /** Types iGO (1 fixe, 2 feu+vitesse, 3 feu, 4 moyenne, 5 mobile) ou mots usuels. */
    internal fun kindFrom(type: String): RadarKind {
        val t = type.lowercase(Locale.ROOT)
        return when {
            t == "2" || t == "3" || "feu" in t || "red" in t -> RadarKind.REDLIGHT
            t == "4" || "moyen" in t || "section" in t || "average" in t || "tronçon" in t -> RadarKind.AVERAGE
            t == "5" || "mobile" in t -> RadarKind.MOBILE
            else -> RadarKind.FIXED
        }
    }

    /** Marque d'encodage UTF-8 laissée par Excel en tête de fichier. */
    private val BOM = 0xFEFF.toChar().toString()
    private val LAT = setOf("lat", "latitude", "y")
    private val LON = setOf("lon", "lng", "long", "longitude", "x")
    private val SPEED = setOf("speed", "vitesse", "speed_kmh", "limit", "limite", "vmax", "kmh")
    private val TYPE = setOf("type", "category", "categorie", "catégorie", "kind")
    private val DIR = setOf("direction", "direction_deg", "dir", "cap", "heading", "bearing")
}
