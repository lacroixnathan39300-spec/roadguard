package fr.roadguard.core.map

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Pack de carte hors ligne installé (fichier PMTiles) et la zone qu'il couvre. */
data class OfflinePack(val file: File, val minLat: Double, val minLon: Double, val maxLat: Double, val maxLon: Double) {
    fun contains(lat: Double, lon: Double) = lat in minLat..maxLat && lon in minLon..maxLon
}

/**
 * Carte sans Internet : packs PMTiles téléchargés par l'appli, style ROADGUARD et polices embarqués.
 * Le style reprend celui de l'ancienne carte hors ligne (compatible schémas Protomaps et OpenMapTiles).
 */
object OfflineMaps {

    /** Packs valides du dossier (en-tête PMTiles v3 lu pour connaître la zone couverte). */
    fun packs(dir: File): List<OfflinePack> =
        dir.listFiles { f -> f.isFile && f.name.endsWith(".pmtiles") }.orEmpty().mapNotNull(::readHeader)

    fun packFor(packs: List<OfflinePack>, lat: Double?, lon: Double?): OfflinePack? =
        if (lat == null || lon == null) packs.firstOrNull() else packs.firstOrNull { it.contains(lat, lon) }

    /** En-tête PMTiles v3 : « PMTiles », version 3, puis limites en degrés × 10⁷ aux octets 102 à 117. */
    fun readHeader(file: File): OfflinePack? = runCatching {
        val bytes = ByteArray(127)
        RandomAccessFile(file, "r").use { it.readFully(bytes) }
        parseHeader(file, bytes)
    }.getOrNull()

    fun parseHeader(file: File, bytes: ByteArray): OfflinePack? {
        if (bytes.size < 127 || String(bytes, 0, 7, Charsets.US_ASCII) != "PMTiles" || bytes[7].toInt() != 3) return null
        val b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val minLon = b.getInt(102) / 1e7
        val minLat = b.getInt(106) / 1e7
        val maxLon = b.getInt(110) / 1e7
        val maxLat = b.getInt(114) / 1e7
        if (minLat >= maxLat || minLon >= maxLon) return null
        return OfflinePack(file, minLat, minLon, maxLat, maxLon)
    }

    private val day = mapOf(
        "background" to "#EEF1F4", "water" to "#A9CBE6", "landcover" to "#D7E8CC", "building" to "#DCD8D2",
        "road" to "#FFFFFF", "roadOutline" to "#B8BEC6", "motorway" to "#F6B25B", "rail" to "#9AA3AD",
        "text" to "#26303C", "halo" to "#FFFFFF",
    )
    private val night = mapOf(
        "background" to "#10151C", "water" to "#1B3A55", "landcover" to "#17241A", "building" to "#232A33",
        "road" to "#3A4554", "roadOutline" to "#0A0E13", "motorway" to "#B06D1E", "rail" to "#4A5562",
        "text" to "#D7E2EC", "halo" to "#0A0E13",
    )

    /** Style MapLibre complet pour un pack, couleurs jour ou nuit. */
    fun style(context: Context, pack: OfflinePack, nightMode: Boolean): String {
        val template = context.assets.open("maps/local_style.json").bufferedReader().use { it.readText() }
        return buildStyle(template, pack.file.absolutePath, nightMode)
    }

    fun buildStyle(template: String, packPath: String, nightMode: Boolean): String {
        var text = template
        (if (nightMode) night else day).forEach { (k, v) -> text = text.replace("\"@$k\"", "\"$v\"") }
        val style = JSONObject(text)
        style.put("glyphs", "asset://glyphs/{fontstack}/{range}.pbf")
        val sources = style.getJSONObject("sources")
        sources.remove("osm-online")
        sources.put("roads", JSONObject()
            .put("type", "vector")
            .put("url", "pmtiles://file://$packPath")
            .put("attribution", "© Protomaps © OpenStreetMap"))
        val layers = style.getJSONArray("layers")
        val kept = org.json.JSONArray()
        for (i in 0 until layers.length()) {
            val l = layers.getJSONObject(i)
            if (l.optString("source") != "osm-online") kept.put(l)
        }
        style.put("layers", kept)
        return style.toString()
    }
}
