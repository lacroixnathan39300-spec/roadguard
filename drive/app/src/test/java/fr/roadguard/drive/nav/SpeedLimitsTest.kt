package fr.roadguard.core.nav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeedLimitsTest {
    @Test fun takesLastKnownLimit() {
        assertEquals(50, SpeedLimits.parse("""{"edges":[{"speed_limit":80},{"speed_limit":50}]}"""))
        // Dernier tronçon sans limite : « inconnue » (0), pas la limite du tronçon précédent.
        assertEquals(0, SpeedLimits.parse("""{"edges":[{"speed_limit":50},{}]}"""))
        assertEquals(0, SpeedLimits.parse("""{"edges":[{"speed_limit":0}]}"""))
        assertNull(SpeedLimits.parse("""{"edges":[]}"""))
    }

    @Test fun requestContainsTrailAndFilter() {
        val body = SpeedLimits.request(listOf(LatLon(46.0, 5.0), LatLon(46.001, 5.0)))
        assertTrue(body.contains("\"shape_match\":\"map_snap\""))
        assertTrue(body.contains("edge.speed_limit"))
    }

    @Test fun overspeedUsesTolerance() {
        assertFalse(SpeedLimitTracker.isOver(55, 50))
        assertTrue(SpeedLimitTracker.isOver(56, 50))
        assertFalse(SpeedLimitTracker.isOver(136, 130))
        assertTrue(SpeedLimitTracker.isOver(137, 130))
        assertFalse(SpeedLimitTracker.isOver(200, null))
    }

    @Test fun asksOnlyWhileMovingAndNotTooOften() {
        val t = SpeedLimitTracker()
        assertNull(t.add(46.0, 5.0, 0, 50.0))
        assertNull(t.add(46.0005, 5.0, 1000, 50.0))
        val first = t.add(46.001, 5.0, 3_500, 50.0)
        assertEquals(3, first!!.size)
        assertNull(t.add(46.0015, 5.0, 5_000, 50.0))
        assertEquals(5, t.add(46.002, 5.0, 7_000, 50.0)!!.size)
        assertNull(t.add(46.0025, 5.0, 30_000, 3.0))
    }

    @Test fun readsWayOfCurrentRoad() {
        assertEquals("123456", SpeedLimits.wayOf("""{"edges":[{"way_id":1},{"way_id":123456,"speed_limit":50}]}"""))
        assertNull(SpeedLimits.wayOf("""{"edges":[{"speed_limit":50}]}"""))
        assertNull(SpeedLimits.wayOf("""{"edges":[]}"""))
    }
}
