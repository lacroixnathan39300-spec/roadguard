package fr.roadguard.core

import android.content.Context
import fr.roadguard.core.radar.Geo
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Trace des trajets (un fichier par jour, 30 jours gardés), exportable en GPX.
 * Un point toutes les 5 s ou tous les 30 m en roulant ; rien à l'arrêt.
 */
class TrackRecorder(context: Context) {
    private val dir = File(context.applicationContext.filesDir, "tracks").apply { mkdirs() }
    private var lastLat = Double.NaN
    private var lastLon = 0.0
    private var lastAt = 0L
    private val day = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun onFix(lat: Double, lon: Double, kmh: Double, altitude: Double?, timeMs: Long) {
        if (kmh < 4) return
        val moved = if (lastLat.isNaN()) Double.MAX_VALUE else Geo.distanceMeters(lastLat, lastLon, lat, lon)
        if (moved < 30 && timeMs - lastAt < 5_000) return
        lastLat = lat; lastLon = lon; lastAt = timeMs
        val f = File(dir, day.format(Date(timeMs)) + ".csv")
        runCatching {
            f.appendText("%.6f;%.6f;%d;%s;%.0f\n".format(Locale.US, lat, lon, timeMs, altitude?.let { "%.0f".format(Locale.US, it) } ?: "", kmh))
        }
        cleanup(timeMs)
    }

    private var cleanedAt = 0L
    private fun cleanup(now: Long) {
        if (now - cleanedAt < 3_600_000) return
        cleanedAt = now
        dir.listFiles()?.filter { now - it.lastModified() > 30L * 86_400_000 }?.forEach { it.delete() }
    }

    /** Jours disponibles (les plus récents d'abord). */
    fun days(): List<String> = dir.listFiles { f -> f.name.endsWith(".csv") }.orEmpty().map { it.name.removeSuffix(".csv") }.sortedDescending()

    /** GPX des jours demandés (un segment par jour, coupé après 10 min sans point). */
    fun gpx(daysWanted: List<String>): String {
        val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<gpx version=\"1.1\" creator=\"ROADGUARD\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n")
        for (d in daysWanted.sorted()) {
            val lines = File(dir, "$d.csv").takeIf { it.isFile }?.readLines().orEmpty()
            if (lines.isEmpty()) continue
            sb.append("  <trk><name>ROADGUARD $d</name>\n    <trkseg>\n")
            var prev = 0L
            for (l in lines) {
                val p = l.split(';')
                val t = p.getOrNull(2)?.toLongOrNull() ?: continue
                if (prev != 0L && t - prev > 600_000) sb.append("    </trkseg>\n    <trkseg>\n")
                prev = t
                sb.append("      <trkpt lat=\"${p[0]}\" lon=\"${p[1]}\">")
                p.getOrNull(3)?.takeIf { it.isNotBlank() }?.let { sb.append("<ele>$it</ele>") }
                sb.append("<time>${iso.format(Date(t))}</time></trkpt>\n")
            }
            sb.append("    </trkseg>\n  </trk>\n")
        }
        sb.append("</gpx>\n")
        return sb.toString()
    }
}
