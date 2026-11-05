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

    @Test fun announcesRadarAheadOnceThenKeepsShowingIt() {
        val engine = AlertEngine(RadarIndex(listOf(radar("r", 46.005))))
        val first = engine.update(fix(46.0, 0))
        assertNotNull(first); assertTrue(first!!.isNew)
        assertEquals(556.0, first.distanceM.toDouble(), 5.0)
        val second = engine.update(fix(46.001, 4000))
        assertFalse(second!!.isNew)
    }

    @Test fun keepsAlertingTwentyMetresAfterTheRadar() {
        val engine = AlertEngine(RadarIndex(listOf(radar("r", 46.001))))
        assertNotNull(engine.update(fix(46.0, 0)))
        val past10 = engine.update(fix(46.00109, 1000)) // ~10 m après
        assertTrue(past10!!.passed); assertEquals(10.0, past10.remainingM.toDouble(), 2.0)
        assertNull(engine.update(fix(46.00127, 2000))) // ~30 m après : fini
    }

    @Test fun ignoresRadarBehindOrOffAxis() {
        val engine = AlertEngine(RadarIndex(listOf(radar("behind", 45.996), radar("side", 46.0, lon = 5.006))))
        assertNull(engine.update(fix(46.0, 0)))
    }

    @Test fun ignoresRadarControllingTheOtherDirection() {
        val engine = AlertEngine(RadarIndex(listOf(radar("south", 46.004, dir = 180))))
        assertNull(engine.update(fix(46.0, 0)))
    }

    @Test fun rangeGrowsWithSpeed() {
        val engine = AlertEngine(RadarIndex(listOf(radar("r", 46.0075)))) // ~834 m
        assertNull(engine.update(fix(46.0, 0, speed = 50.0)))
        assertNotNull(engine.update(fix(46.0, 1000, speed = 130.0)))
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
