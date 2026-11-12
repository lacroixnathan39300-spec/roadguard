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
 * Choisit LE radar à annoncer, celui qui est vraiment sur ta route :
 *  - avec un itinéraire : seuls les radars à moins de 30 m du tracé, devant, comptent (distance mesurée le long du tracé) ;
 *  - sans itinéraire : radar dans un couloir étroit autour de l'axe suivi (et non plus un cône qui s'élargit),
 *    qui se rapproche réellement à peu près à ta vitesse (sinon il est sur une route voisine), et dans le bon sens s'il est connu.
 * Une fois annoncé, on reste sur ce radar jusqu'à l'avoir passé (pas de va-et-vient avec un radar voisin).
 * Alerte à 1 km. Chaque radar n'est annoncé qu'une fois par passage.
 */
class AlertEngine(private val index: RadarIndex) {
    private val announced = LinkedHashMap<String, Long>()

    /** Radars déjà franchis (plus annoncés pendant 10 min). */
    private val passedIds = HashMap<String, Long>()

    /** Radars signalés par la communauté autour de soi : annoncés et bipés comme ceux des bases. */
    @Volatile var extra: List<Radar> = emptyList()

    /** Tracé de l'itinéraire en cours (null sans guidage). */
    var route: List<fr.roadguard.core.nav.LatLon>? = null
        set(v) { if (v !== field) { field = v; cum = null; routeIdx = 0 } }
    private var cum: DoubleArray? = null
    private var routeIdx = 0

    /** Contrôle « il se rapproche vraiment » : dernier échantillon (temps, distance) par radar candidat. */
    private val samples = HashMap<String, Pair<Long, Double>>()
    private val confirmed = HashSet<String>()
    private var locked: String? = null

    fun update(fix: Fix): RadarAlert? {
        if (fix.accuracyM > 60 || fix.speedKmh < 10 || fix.bearing.isNaN()) return null
        // Le GPS a environ 1 s de retard : la voiture est déjà plus loin que la position reçue.
        // On avance donc la position de ce retard, pour que l'alerte s'arrête pile au radar (et pas après).
        val lag = lagMeters(fix.speedKmh)
        val range = warningDistance(fix.speedKmh)
        val onRoute = routeAlongFix(fix)
        val v = fix.speedKmh / 3.6
        val candidates = HashMap<String, Pair<Radar, Double>>()
        val signaled = extra
        for (r in if (signaled.isEmpty()) index.near(fix.lat, fix.lon) else index.near(fix.lat, fix.lon) + signaled) {
            if (r.kind == RadarKind.OTHER || r.id in passedIds) continue
            val straight = Geo.distanceMeters(fix.lat, fix.lon, r.lat, r.lon)
            if (straight - lag > range + 200) continue
            val off = Geo.headingDelta(fix.bearing, Geo.bearing(fix.lat, fix.lon, r.lat, r.lon))
            // Sens du radar connu : ignorer ceux qui contrôlent l'autre sens.
            val dir = r.directionDeg
            if (dir != null && Geo.headingDelta(fix.bearing, dir.toDouble()) > 45) continue
            if (onRoute != null) {
                // Itinéraire : distance le long du tracé, radar à moins de 30 m de la route.
                val along = alongRoute(r, onRoute, range + lag) ?: continue
                val d = along - lag
                if (d <= 0) { if (d > -60) passedIds[r.id] = fix.timeMs; continue }
                if (d <= range) candidates[r.id] = r to d
                continue
            }
            val d = straight - lag
            if (off > 90 || d <= 0) { if (straight < 60) passedIds[r.id] = fix.timeMs; continue }  // franchi (ou sur le point de l'être)
            if (d > range) continue
            // Couloir : écart latéral à l'axe suivi (un peu plus large au loin pour les courbes).
            val lateral = straight * kotlin.math.sin(Math.toRadians(off))
            if (d > 40 && lateral > 45 + 0.06 * d) continue
            candidates[r.id] = r to d
        }
        // Sans itinéraire : le radar doit se rapprocher presque aussi vite que tu roules (sinon : route voisine).
        if (onRoute == null) {
            for ((id, c) in candidates) {
                val prev = samples[id]
                if (prev == null) { samples[id] = fix.timeMs to c.second; continue }
                val dt = (fix.timeMs - prev.first) / 1000.0
                if (dt < 1.5) continue
                val rate = (prev.second - c.second) / dt
                if (rate >= 0.6 * v) confirmed.add(id) else if (rate < 0.3 * v) confirmed.remove(id)
                samples[id] = fix.timeMs to c.second
            }
            samples.keys.retainAll(candidates.keys); confirmed.retainAll(candidates.keys)
        }
        fun ok(id: String) = onRoute != null || id in confirmed
        val keep = locked?.let { id -> candidates[id]?.takeIf { ok(id) } }
        val chosen = keep ?: candidates.filter { ok(it.key) }.values.minByOrNull { it.second }
        locked = chosen?.first?.id
        forgetOld(fix.timeMs)
        val (radar, dist) = chosen ?: return null
        val isNew = announced.put(radar.id, fix.timeMs) == null
        return RadarAlert(radar, dist.toInt().coerceAtLeast(0), isNew)
    }

    /** Position sur l'itinéraire (mètres depuis le départ), ou null si pas d'itinéraire ou si on s'en est écarté (> 80 m). */
    private fun routeAlongFix(fix: Fix): Double? {
        val pts = route ?: return null
        if (pts.size < 2) return null
        val c = cum ?: DoubleArray(pts.size).also { a ->
            for (i in 1 until pts.size) a[i] = a[i - 1] + Geo.distanceMeters(pts[i - 1].lat, pts[i - 1].lon, pts[i].lat, pts[i].lon)
            cum = a
        }
        var best = Double.MAX_VALUE; var bestAlong = 0.0; var bestI = routeIdx
        val from = (routeIdx - 5).coerceAtLeast(0)
        val to = (routeIdx + 400).coerceAtMost(pts.size - 1)
        for (i in from until to) {
            val (dist, t) = project(fix.lat, fix.lon, pts[i], pts[i + 1])
            if (dist < best) { best = dist; bestAlong = c[i] + t * (c[i + 1] - c[i]); bestI = i }
        }
        if (best > 80) { // perdu : recherche sur tout le tracé (détour, recalcul)
            for (i in 0 until pts.size - 1) {
                val (dist, t) = project(fix.lat, fix.lon, pts[i], pts[i + 1])
                if (dist < best) { best = dist; bestAlong = c[i] + t * (c[i + 1] - c[i]); bestI = i }
            }
        }
        if (best > 80) return null
        routeIdx = bestI
        return bestAlong
    }

    /** Distance le long du tracé entre la voiture et le radar, si le radar est à moins de 30 m de la route devant. */
    private fun alongRoute(r: Radar, fixAlong: Double, maxAhead: Double): Double? {
        val pts = route ?: return null
        val c = cum ?: return null
        var i = routeIdx
        while (i < pts.size - 1 && c[i] - fixAlong <= maxAhead) {
            val (dist, t) = project(r.lat, r.lon, pts[i], pts[i + 1])
            if (dist <= 30) {
                val along = c[i] + t * (c[i + 1] - c[i])
                if (along >= fixAlong - 60) return along - fixAlong
            }
            i++
        }
        return null
    }

    /** Distance (m) d'un point à un segment, et position (0–1) de sa projection sur le segment. */
    private fun project(lat: Double, lon: Double, a: fr.roadguard.core.nav.LatLon, b: fr.roadguard.core.nav.LatLon): Pair<Double, Double> {
        val k = Math.cos(Math.toRadians(lat)) * 111_320.0
        val ax = (a.lon - lon) * k; val ay = (a.lat - lat) * 110_540.0
        val bx = (b.lon - lon) * k; val by = (b.lat - lat) * 110_540.0
        val dx = bx - ax; val dy = by - ay
        val len2 = dx * dx + dy * dy
        val t = if (len2 == 0.0) 0.0 else (-(ax * dx + ay * dy) / len2).coerceIn(0.0, 1.0)
        val px = ax + t * dx; val py = ay + t * dy
        return Math.sqrt(px * px + py * py) to t
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

        /** Alertes à 1 km, quelle que soit la vitesse. */
        @Suppress("UNUSED_PARAMETER")
        fun warningDistance(speedKmh: Double): Double = 1000.0
    }
}
