package fr.roadguard.core.radar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertEngineTest {
    // Un degré de latitude ≈ 111 km : 0,001° ≈ 111 m. Route plein nord à partir de (46.0, 5.0).
    private fun radar(id: String, lat: Double, lon: Double = 5.0, kind: RadarKind = RadarKind.FIXED,
                      dir: Int? = null, category: String = "fixed", speed: Int? = null) =
        Radar(id, lat, lon, kind, "Radar", speed, dir, category)

    private fun fix(lat: Double, t: Long, speed: Double = 90.0, bearing: Double = 0.0, lon: Double = 5.0) =
        Fix(lat, lon, speed, bearing, 8.0, t)

    // Sans itinéraire, l'alerte vient après ~2 s de rapprochement réel (90 km/h = 25 m/s ≈ 0,000225°/s).
    private fun approach(engine: AlertEngine, lat0: Double, speed: Double = 90.0, bearing: Double = 0.0, lon: Double = 5.0): RadarAlert? {
        val step = speed / 3.6 / 111_000.0 * 2 * (if (bearing == 180.0) -1 else 1)
        engine.update(fix(lat0, 0, speed, bearing, lon))
        return engine.update(fix(lat0 + step, 2000, speed, bearing, lon))
    }

    @Test fun announcesRadarAheadOnceThenKeepsShowingIt() {
        val engine = AlertEngine(RadarIndex(listOf(radar("r", 46.005))))
        val first = approach(engine, 46.0)
        assertNotNull(first); assertTrue(first!!.isNew)
        assertEquals(481.0, first.distanceM.toDouble(), 6.0) // 556 - 50 m roulés - 25 m de retard GPS
        val second = engine.update(fix(46.001, 4000))
        assertFalse(second!!.isNew)
    }

    @Test fun alertStopsAtTheRadarNotAfter() {
        val engine = AlertEngine(RadarIndex(listOf(radar("r", 46.002)))) // ~222 m devant
        assertNotNull(approach(engine, 46.0))
        assertNull(engine.update(fix(46.0018, 4000))) // ~22 m avant le radar : la voiture y est déjà (retard GPS)
        assertNull(engine.update(fix(46.0022, 5000))) // après : plus rien
        assertNull(engine.update(fix(46.0019, 6000))) // pas de nouvelle alerte pour ce radar
    }

    @Test fun ignoresRadarBehindOrOffAxis() {
        val engine = AlertEngine(RadarIndex(listOf(radar("behind", 45.996), radar("side", 46.0, lon = 5.006))))
        assertNull(approach(engine, 46.0))
    }

    @Test fun ignoresRadarOnParallelRoad() {
        // Radar à 600 m devant mais 200 m à côté (route parallèle) : plus annoncé.
        val engine = AlertEngine(RadarIndex(listOf(radar("p", 46.0054, lon = 5.0026))))
        assertNull(approach(engine, 46.0))
    }

    @Test fun ignoresRadarControllingTheOtherDirection() {
        val engine = AlertEngine(RadarIndex(listOf(radar("south", 46.004, dir = 180))))
        assertNull(approach(engine, 46.0))
    }

    @Test fun warnsAtOneKilometre() {
        val engine = AlertEngine(RadarIndex(listOf(radar("r", 46.0088)))) // ~977 m
        assertNotNull(approach(engine, 46.0, speed = 50.0))
    }

    @Test fun withRouteOnlyRadarsOnTheRouteCount() {
        val engine = AlertEngine(RadarIndex(listOf(radar("on", 46.004), radar("off", 46.003, lon = 5.001))))
        engine.route = listOf(fr.roadguard.core.nav.LatLon(46.0, 5.0), fr.roadguard.core.nav.LatLon(46.02, 5.0))
        val a = engine.update(fix(46.0, 0))
        assertEquals("on", a!!.radar.id) // le radar à 77 m de la route est ignoré, même plus proche
    }

    @Test fun averageSectionMeasuredFromStartToEnd() {
        val index = RadarIndex(listOf(
            radar("s", 46.0, kind = RadarKind.AVERAGE, category = "average_start"),
            radar("lim", 46.0005, kind = RadarKind.AVERAGE, category = "average", speed = 80, dir = 0),
            radar("e", 46.02, kind = RadarKind.AVERAGE, category = "average_end"),
        ))
        val tracker = AverageSpeedTracker(index)
        // 0,0005° toutes les 2 s ≈ 55,6 m / 2 s = 100 km/h, sur ~2,2 km.
        var status: AverageStatus? = null
        var t = 0L
        var lat = 46.0
        while (lat <= 46.0201) {
            status = tracker.update(fix(lat, t, speed = 100.0))
            if (status?.finished == true) break
            lat += 0.0005; t += 2000
        }
        assertNotNull(status)
        assertTrue(status!!.finished)
        assertEquals(80, status.limitKmh)
        assertEquals(100.0, status.averageKmh.toDouble(), 3.0)
        assertTrue(status.over)
    }

    @Test fun averageNotStartedAgainstTheControlledDirection() {
        val index = RadarIndex(listOf(
            radar("s", 46.0, kind = RadarKind.AVERAGE, category = "average_start"),
            radar("lim", 46.0005, kind = RadarKind.AVERAGE, category = "average", speed = 80, dir = 0),
        ))
        assertNull(AverageSpeedTracker(index).update(fix(46.0, 0, bearing = 180.0)))
    }
}
