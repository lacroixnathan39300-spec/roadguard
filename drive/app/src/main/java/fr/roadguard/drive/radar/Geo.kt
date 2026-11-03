package fr.roadguard.drive.radar

import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** Calculs géographiques purs (sphère terrestre, précision largement suffisante pour les alertes). */
object Geo {
    private const val EARTH_DIAMETER_M = 12_742_000.0

    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val h = sin(dLat / 2).let { it * it } +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).let { it * it }
        return EARTH_DIAMETER_M * asin(min(1.0, sqrt(h)))
    }

    /** Cap de 1 vers 2, en degrés 0–360 (0 = nord). */
    fun bearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.toRadians(lat1)
        val b = Math.toRadians(lat2)
        val y = sin(dLon) * cos(b)
        val x = cos(a) * sin(b) - sin(a) * cos(b) * cos(dLon)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }

    /** Écart absolu entre deux caps, 0–180°. */
    fun headingDelta(a: Double, b: Double): Double {
        val d = abs((a - b + 360.0) % 360.0)
        return min(d, 360.0 - d)
    }
}

/** Index par cellules de 0,05° (~5 km) pour ne tester que les radars proches. */
class RadarIndex(radars: List<Radar>) {
    private val cells: Map<Long, List<Radar>> = radars.groupBy { key(cell(it.lat), cell(it.lon)) }

    val size: Int = radars.size

    fun near(lat: Double, lon: Double): List<Radar> {
        val cy = cell(lat)
        val cx = cell(lon)
        val out = ArrayList<Radar>()
        for (dy in -1..1) for (dx in -1..1) cells[key(cy + dy, cx + dx)]?.let(out::addAll)
        return out
    }

    private fun cell(v: Double) = Math.floor(v / 0.05).toInt()
    private fun key(y: Int, x: Int) = (y.toLong() shl 32) or (x.toLong() and 0xffffffffL)
}
