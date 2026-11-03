package fr.roadguard.drive.radar

import kotlin.math.max
import kotlin.math.roundToInt

/** État affiché pendant et juste après un tronçon de vitesse moyenne. */
data class AverageStatus(
    val averageKmh: Int,
    /** Limite du tronçon, null si la base ne la donne pas. */
    val limitKmh: Int?,
    val meters: Int,
    val finished: Boolean,
) {
    /** Au-dessus de la limite (false si la limite est inconnue). */
    val over: Boolean get() = limitKmh != null && averageKmh > limitKmh
}

/**
 * Mesure la vitesse moyenne réelle sur un tronçon contrôlé.
 *
 * Les bases françaises ne relient pas les points « début » et « fin » d'un même tronçon :
 * le tronçon est donc reconnu en roulant. Il commence au passage d'un point de début
 * (dans le sens de circulation si un radar voisin indique son sens) et se termine au premier
 * point de fin franchi après au moins [MIN_SECTION_M] mètres. La moyenne = distance parcourue
 * (somme des positions GPS) / temps écoulé, comme le calcul du radar.
 */
class AverageSpeedTracker(private val index: RadarIndex) {

    private var active: Active? = null
    private var lastResult: AverageStatus? = null
    private var lastResultAt = 0L

    private class Active(
        val startId: String,
        val limitKmh: Int?,
        val startedAt: Long,
        var lastLat: Double,
        var lastLon: Double,
        var lastAt: Long,
        var meters: Double = 0.0,
    )

    fun update(fix: Fix): AverageStatus? {
        if (fix.accuracyM > 40) return cancel()
        val zone = active ?: return tryStart(fix)

        val dt = fix.timeMs - zone.lastAt
        if (dt <= 0) return status(zone, fix.timeMs, finished = false)
        // Trou GPS trop long ou saut impossible : la mesure n'est plus fiable.
        val step = Geo.distanceMeters(zone.lastLat, zone.lastLon, fix.lat, fix.lon)
        if (dt > 20_000 || step > max(80.0, dt / 1000.0 * 70.0)) return cancel()
        zone.meters += step
        zone.lastLat = fix.lat
        zone.lastLon = fix.lon
        zone.lastAt = fix.timeMs

        if (zone.meters > MAX_SECTION_M) return cancel()
        if (zone.meters >= MIN_SECTION_M && passing(fix, "average_end")) {
            val result = status(zone, fix.timeMs, finished = true)
            active = null
            lastResult = result
            lastResultAt = fix.timeMs
            return result
        }
        return status(zone, fix.timeMs, finished = false)
    }

    private fun tryStart(fix: Fix): AverageStatus? {
        // Le résultat final reste affiché 10 s après la fin du tronçon.
        lastResult?.let { if (fix.timeMs - lastResultAt in 0..RESULT_SHOW_MS) return it }
        lastResult = null
        if (fix.speedKmh < 10 || fix.bearing.isNaN()) return null
        val start = nearest(fix, "average_start", PASS_RADIUS_M) ?: return null
        val nearby = index.near(start.lat, start.lon).filter {
            it.kind == RadarKind.AVERAGE && Geo.distanceMeters(start.lat, start.lon, it.lat, it.lon) < 400
        }
        // Si un radar voisin indique son sens, on ne démarre que dans ce sens.
        val dir = nearby.firstNotNullOfOrNull { it.directionDeg }
        if (dir != null && Geo.headingDelta(fix.bearing, dir.toDouble()) > 60) return null
        val limit = nearby.firstNotNullOfOrNull { it.speedKmh?.takeIf { s -> s in 20..150 } }
        active = Active(start.id, limit, fix.timeMs, fix.lat, fix.lon, fix.timeMs)
        return status(active!!, fix.timeMs, finished = false)
    }

    private fun passing(fix: Fix, category: String) = nearest(fix, category, PASS_RADIUS_M) != null

    private fun nearest(fix: Fix, category: String, radius: Double): Radar? =
        index.near(fix.lat, fix.lon)
            .filter { it.category == category }
            .minByOrNull { Geo.distanceMeters(fix.lat, fix.lon, it.lat, it.lon) }
            ?.takeIf { Geo.distanceMeters(fix.lat, fix.lon, it.lat, it.lon) <= radius }

    private fun status(zone: Active, now: Long, finished: Boolean): AverageStatus {
        val seconds = max((now - zone.startedAt) / 1000.0, 1.0)
        return AverageStatus((zone.meters * 3.6 / seconds).roundToInt(), zone.limitKmh, zone.meters.roundToInt(), finished)
    }

    private fun cancel(): AverageStatus? {
        active = null
        return null
    }

    companion object {
        const val PASS_RADIUS_M = 60.0
        const val MIN_SECTION_M = 300.0
        const val MAX_SECTION_M = 30_000.0
        const val RESULT_SHOW_MS = 10_000L
    }
}
