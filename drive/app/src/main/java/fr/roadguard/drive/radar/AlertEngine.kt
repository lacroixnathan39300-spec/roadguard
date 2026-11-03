package fr.roadguard.drive.radar

/** Une position GPS utile aux alertes. [bearing] est NaN si inconnu (arrêt). */
data class Fix(
    val lat: Double,
    val lon: Double,
    val speedKmh: Double,
    val bearing: Double,
    val accuracyM: Double,
    val timeMs: Long,
)

/** Alerte à afficher : le radar devant soi le plus proche. */
data class RadarAlert(
    val radar: Radar,
    val distanceM: Int,
    /** true la première fois que ce radar est annoncé (bip + voix). */
    val isNew: Boolean,
)

/**
 * Choisit le radar à annoncer : devant (±35° du cap), dans le bon sens s'il est connu,
 * à une distance qui grandit avec la vitesse. Chaque radar n'est annoncé qu'une fois par passage.
 */
class AlertEngine(private val index: RadarIndex) {
    private val announced = LinkedHashMap<String, Long>()

    fun update(fix: Fix): RadarAlert? {
        if (fix.accuracyM > 60 || fix.speedKmh < 10 || fix.bearing.isNaN()) return null
        val range = warningDistance(fix.speedKmh)
        var best: Radar? = null
        var bestDist = Double.MAX_VALUE
        for (r in index.near(fix.lat, fix.lon)) {
            if (r.kind == RadarKind.OTHER) continue
            val d = Geo.distanceMeters(fix.lat, fix.lon, r.lat, r.lon)
            if (d > range || d >= bestDist) continue
            // Devant soi : le radar doit être dans l'axe de la route suivie.
            if (d > 40 && Geo.headingDelta(fix.bearing, Geo.bearing(fix.lat, fix.lon, r.lat, r.lon)) > 35) continue
            // Sens du radar connu : ignorer ceux qui contrôlent l'autre sens.
            val dir = r.directionDeg
            if (dir != null && Geo.headingDelta(fix.bearing, dir.toDouble()) > 60) continue
            best = r
            bestDist = d
        }
        forgetOld(fix.timeMs)
        val radar = best ?: return null
        val isNew = announced.put(radar.id, fix.timeMs) == null
        return RadarAlert(radar, bestDist.toInt(), isNew)
    }

    private fun forgetOld(now: Long) {
        // Après 10 minutes, un radar peut être annoncé de nouveau (nouveau passage).
        val it = announced.entries.iterator()
        while (it.hasNext()) if (now - it.next().value > 600_000) it.remove()
    }

    companion object {
        /** Environ 25 s de trajet : 400 m en ville, ~625 m à 90 km/h, ~900 m à 130 km/h (max 1 000 m). */
        fun warningDistance(speedKmh: Double): Double = (speedKmh / 3.6 * 25.0).coerceIn(400.0, 1000.0)
    }
}
