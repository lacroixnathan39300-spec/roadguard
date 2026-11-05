package fr.roadguard.core.radar

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
    /** Inutilisé (ancienne prolongation après le radar) : l'alerte s'arrête désormais pile au radar. */
    val passed: Boolean = false,
    val remainingM: Int = 0,
)

/**
 * Choisit le radar à annoncer : devant (±35° du cap), dans le bon sens s'il est connu,
 * à une distance qui grandit avec la vitesse. Chaque radar n'est annoncé qu'une fois par passage.
 */
class AlertEngine(private val index: RadarIndex) {
    private val announced = LinkedHashMap<String, Long>()

    /** Radars déjà franchis (plus annoncés pendant 10 min). */
    private val passedIds = HashMap<String, Long>()

    fun update(fix: Fix): RadarAlert? {
        if (fix.accuracyM > 60 || fix.speedKmh < 10 || fix.bearing.isNaN()) return null
        // Le GPS a environ 1 s de retard : la voiture est déjà plus loin que la position reçue.
        // On avance donc la position de ce retard, pour que l'alerte s'arrête pile au radar (et pas après).
        val lag = lagMeters(fix.speedKmh)
        val range = warningDistance(fix.speedKmh)
        var best: Radar? = null
        var bestDist = Double.MAX_VALUE
        for (r in index.near(fix.lat, fix.lon)) {
            if (r.kind == RadarKind.OTHER || r.id in passedIds) continue
            val d = Geo.distanceMeters(fix.lat, fix.lon, r.lat, r.lon) - lag
            if (d > range || d >= bestDist) continue
            // Devant soi : le radar doit être dans l'axe de la route suivie.
            val off = Geo.headingDelta(fix.bearing, Geo.bearing(fix.lat, fix.lon, r.lat, r.lon))
            if (off > 90 || d <= 0) { if (d < 30) passedIds[r.id] = fix.timeMs; continue }  // franchi (ou sur le point de l'être)
            if (d > 40 && off > 35) continue
            // Sens du radar connu : ignorer ceux qui contrôlent l'autre sens.
            val dir = r.directionDeg
            if (dir != null && Geo.headingDelta(fix.bearing, dir.toDouble()) > 60) continue
            best = r
            bestDist = d
        }
        forgetOld(fix.timeMs)
        val radar = best ?: return null
        val isNew = announced.put(radar.id, fix.timeMs) == null
        return RadarAlert(radar, bestDist.toInt().coerceAtLeast(0), isNew)
    }

    private fun forgetOld(now: Long) {
        // Après 10 minutes, un radar peut être annoncé de nouveau (nouveau passage).
        val it = announced.entries.iterator()
        while (it.hasNext()) if (now - it.next().value > 600_000) it.remove()
        passedIds.entries.removeAll { now - it.value > 600_000 }
    }

    companion object {
        /** Retard moyen du GPS (~1 s) converti en mètres : 20 m à 70 km/h, 36 m à 130 km/h. */
        fun lagMeters(speedKmh: Double): Double = speedKmh / 3.6 * 1.0

        /** Environ 30 s de trajet : 500 m en ville, 750 m à 90 km/h, ~1 080 m à 130 km/h (max 1 200 m). */
        fun warningDistance(speedKmh: Double): Double = (speedKmh / 3.6 * 30.0).coerceIn(500.0, 1200.0)
    }
}
