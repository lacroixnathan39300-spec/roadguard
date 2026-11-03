package fr.roadguard.drive.radar

import android.content.res.AssetManager
import java.io.BufferedReader

/** Familles affichées sur la carte, chacune avec sa couleur. */
enum class RadarKind { FIXED, REDLIGHT, AVERAGE, SECTION, MOBILE, OTHER }

data class Radar(
    val id: String,
    val lat: Double,
    val lon: Double,
    val kind: RadarKind,
    val label: String,
    val speedKmh: Int?,
)

/**
 * Lit les bases radars embarquées (même format que l'appli GPS).
 * Deux formats coexistent : complet (12 colonnes, France/Suisse) et court (5 colonnes, MapAtlas).
 * Les zones « présence non confirmée » (historique, travaux) ne sont pas affichées sur la carte.
 */
object RadarCatalog {

    fun loadAll(assets: AssetManager): List<Radar> {
        val files = assets.list("radars").orEmpty().filter { it.startsWith("radars_embedded_") && it.endsWith(".csv") }
        return files.flatMap { name -> assets.open("radars/$name").bufferedReader().use { parse(it) } }
    }

    fun parse(reader: BufferedReader): List<Radar> {
        val header = reader.readLine()?.split(';') ?: return emptyList()
        val col = header.withIndex().associate { (i, n) -> n.trim() to i }
        val iId = col["id"] ?: return emptyList()
        val iType = col["type"] ?: return emptyList()
        val iLat = col["lat"] ?: return emptyList()
        val iLon = col["lon"] ?: return emptyList()
        val iCat = col["category"]
        val iStatus = col["status"]
        val iSpeed = col["speed_kmh"]

        val out = ArrayList<Radar>()
        reader.lineSequence().forEach { line ->
            val f = line.split(';')
            if (f.size <= maxOf(iId, iType, iLat, iLon)) return@forEach
            val lat = f[iLat].toDoubleOrNull() ?: return@forEach
            val lon = f[iLon].toDoubleOrNull() ?: return@forEach
            if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return@forEach
            val status = iStatus?.let { f.getOrNull(it) }.orEmpty()
            if (status == "historical") return@forEach
            val type = f[iType]
            val kind = kindOf(iCat?.let { f.getOrNull(it) }.orEmpty(), type) ?: return@forEach
            out += Radar(
                id = f[iId], lat = lat, lon = lon, kind = kind, label = type,
                speedKmh = iSpeed?.let { f.getOrNull(it)?.toIntOrNull() },
            )
        }
        return out
    }

    /** Catégorie du format complet, sinon déduite du libellé (format MapAtlas). */
    internal fun kindOf(category: String, type: String): RadarKind? = when (category) {
        "fixed", "turret", "discriminant", "urban", "tunnel", "variable", "carpool", "crossing" -> RadarKind.FIXED
        "redlight" -> RadarKind.REDLIGHT
        "average", "average_start", "average_end" -> RadarKind.AVERAGE
        "mobile" -> RadarKind.MOBILE
        "temporary", "works" -> null
        "" -> {
            val t = type.lowercase()
            when {
                "feu rouge" in t -> RadarKind.REDLIGHT
                "moyenne" in t -> RadarKind.AVERAGE
                "fixe" in t || "variable" in t -> RadarKind.FIXED
                else -> RadarKind.OTHER
            }
        }
        else -> RadarKind.OTHER
    }
}
