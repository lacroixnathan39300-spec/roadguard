package fr.roadguard.core.nav

import fr.roadguard.core.radar.Geo
import kotlin.math.cos

/** Ce que l'écran et la voix doivent montrer à un instant du trajet. */
data class GuidanceState(
    val next: Maneuver?,
    /** Distance jusqu'à la prochaine manœuvre, en mètres. */
    val toNextM: Int,
    val remainingM: Int,
    val remainingS: Int,
    val offRoute: Boolean,
    val arrived: Boolean,
    /** Phrase à prononcer maintenant (une seule fois), ou null. */
    val speak: String?,
    /** Manœuvre qui suit de près la prochaine (moins de 250 m) : « puis… ». */
    val then: Maneuver? = null,
    /** Nature de [speak] : [START] (départ), [EARLY] (à l'approche, « Dans 500 mètres… ») ou [NOW] (au moment de la manœuvre). */
    val speakKind: Int = 0,
) {
    companion object {
        const val START = 1
        const val EARLY = 2
        const val NOW = 3
    }
}

/**
 * Suit la progression sur le tracé : point le plus proche (recherche locale autour du dernier
 * connu), manœuvre suivante, distances restantes, sortie de route et annonces vocales
 * (à l'approche puis au moment de tourner).
 */
class Guidance(val route: Route) {
    private val cumulative = DoubleArray(route.points.size)
    private var lastIndex = 0
    private var offCount = 0
    private val spokenEarly = HashSet<Int>()
    private val spokenNow = HashSet<Int>()
    private var spokeStart = false

    init {
        for (i in 1 until route.points.size) {
            val a = route.points[i - 1]
            val b = route.points[i]
            cumulative[i] = cumulative[i - 1] + Geo.distanceMeters(a.lat, a.lon, b.lat, b.lon)
        }
    }

    fun update(lat: Double, lon: Double, speedKmh: Double): GuidanceState {
        val pts = route.points
        if (pts.size < 2) return GuidanceState(null, 0, 0, 0, offRoute = false, arrived = true, speak = null)

        // Point projeté sur le segment le plus proche, en cherchant autour de la dernière position.
        var bestDist = Double.MAX_VALUE
        var bestIndex = lastIndex
        var bestAlong = cumulative[lastIndex]
        val from = (lastIndex - 5).coerceAtLeast(0)
        val to = (lastIndex + 200).coerceAtMost(pts.size - 2)
        for (i in from..to) {
            val (d, t) = projection(lat, lon, pts[i], pts[i + 1])
            if (d < bestDist) {
                bestDist = d
                bestIndex = i
                bestAlong = cumulative[i] + t * (cumulative[i + 1] - cumulative[i])
            }
        }
        val onRoute = bestDist <= OFF_ROUTE_M
        if (onRoute) { lastIndex = bestIndex; offCount = 0 } else offCount++

        val total = cumulative.last()
        val remaining = (total - bestAlong).coerceAtLeast(0.0)
        val ratio = if (total > 0) remaining / total else 0.0
        val remainingS = (route.durationS * ratio).toInt()
        val arrived = remaining < ARRIVAL_M

        val next = route.maneuvers.firstOrNull { it.shapeIndex > lastIndex } ?: route.maneuvers.lastOrNull()
        val nextIndex = next?.let { route.maneuvers.indexOf(it) } ?: -1
        val toNext = next?.let { (cumulative[it.shapeIndex] - bestAlong).coerceAtLeast(0.0) } ?: remaining

        val following = if (nextIndex >= 0) route.maneuvers.getOrNull(nextIndex + 1) else null
        val then = following?.takeIf { f -> next != null && !next.isArrival &&
            cumulative[f.shapeIndex.coerceAtMost(cumulative.size - 1)] - cumulative[next.shapeIndex] < THEN_M }
        var speak: String? = null
        var speakKind = 0
        if (!spokeStart) {
            spokeStart = true
            speak = route.maneuvers.firstOrNull()?.verbalNow
            speakKind = GuidanceState.START
        } else if (next != null && onRoute) {
            val early = earlyDistance(speedKmh)
            if (toNext <= nowDistance(speedKmh) && spokenNow.add(nextIndex)) {
                spokenEarly.add(nextIndex)
                speak = then?.let { "${next.verbalNow.trimEnd('.', ' ')}, puis ${it.verbalAlert.replaceFirstChar { c -> c.lowercase() }}" }
                    ?: next.verbalNow
                speakKind = GuidanceState.NOW
            } else if (toNext <= early && toNext > nowDistance(speedKmh) * 2 && spokenEarly.add(nextIndex)) {
                speak = "Dans ${roundDistance(toNext)}, ${next.verbalAlert.replaceFirstChar { it.lowercase() }}"
                speakKind = GuidanceState.EARLY
            }
        }

        return GuidanceState(
            next = next,
            toNextM = toNext.toInt(),
            remainingM = remaining.toInt(),
            remainingS = remainingS,
            offRoute = offCount >= OFF_ROUTE_FIXES,
            arrived = arrived,
            speak = speak,
            then = then,
            speakKind = speakKind,
        )
    }

    /** Point du tracé situé [m] mètres devant la dernière position suivie, ou null au-delà de l'arrivée. */
    fun pointAhead(m: Double): LatLon? {
        if (route.points.isEmpty()) return null
        val target = cumulative[lastIndex] + m
        val i = cumulative.indexOfFirst { it >= target }
        return if (i < 0) null else route.points[i]
    }

    /** Distance du point au segment [a,b] (mètres) et position relative t ∈ [0,1] sur le segment. */
    private fun projection(lat: Double, lon: Double, a: LatLon, b: LatLon): Pair<Double, Double> {
        val k = cos(Math.toRadians(lat)) // projection locale plane, suffisante sur quelques km
        val ax = a.lon * k; val ay = a.lat
        val bx = b.lon * k; val by = b.lat
        val px = lon * k; val py = lat
        val dx = bx - ax; val dy = by - ay
        val len2 = dx * dx + dy * dy
        val t = if (len2 == 0.0) 0.0 else (((px - ax) * dx + (py - ay) * dy) / len2).coerceIn(0.0, 1.0)
        val qLat = ay + t * dy
        val qLon = (ax + t * dx) / k
        return Geo.distanceMeters(lat, lon, qLat, qLon) to t
    }

    companion object {
        const val OFF_ROUTE_M = 45.0
        const val OFF_ROUTE_FIXES = 3
        const val ARRIVAL_M = 25.0
        const val THEN_M = 250.0

        /** Annonce anticipée : ~500 m en ville, ~1 km sur route rapide, ~2 km sur autoroute. */
        fun earlyDistance(speedKmh: Double): Double = when {
            speedKmh >= 100 -> 2000.0
            speedKmh >= 70 -> 1000.0
            else -> 500.0
        }

        /** Annonce « maintenant » : quelques secondes avant la manœuvre. */
        fun nowDistance(speedKmh: Double): Double = (speedKmh / 3.6 * 6.0).coerceIn(40.0, 250.0)

        fun roundDistance(m: Double): String = when {
            m >= 1000 -> {
                val km = Math.round(m / 100.0) / 10.0
                val text = if (km % 1.0 == 0.0) km.toInt().toString() else km.toString().replace('.', ',')
                if (km < 2) "$text kilomètre" else "$text kilomètres"
            }
            m >= 100 -> "${(Math.round(m / 50.0) * 50).toInt()} mètres"
            else -> "${(Math.round(m / 10.0) * 10).toInt()} mètres"
        }

        /** Distance courte pour l'écran : « 350 m », « 1,2 km ». */
        fun shortDistance(m: Int): String = when {
            m >= 10_000 -> "${m / 1000} km"
            m >= 1000 -> String.format(java.util.Locale.FRANCE, "%.1f km", m / 1000.0)
            m >= 100 -> "${(m + 25) / 50 * 50} m"
            else -> "${(m + 5) / 10 * 10} m"
        }
    }
}
