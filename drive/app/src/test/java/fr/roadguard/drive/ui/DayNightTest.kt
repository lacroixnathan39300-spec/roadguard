package fr.roadguard.drive.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class DayNightTest {
    private fun utc(y: Int, m: Int, d: Int, h: Int, min: Int = 0): Long =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { clear(); set(y, m - 1, d, h, min) }.timeInMillis

    // Ney (Jura) : 46,72 N, 5,90 E.
    @Test fun noonIsDayMidnightIsNight() {
        assertFalse(DayNight.isNight(46.72, 5.90, utc(2026, 11, 4, 11)))
        assertTrue(DayNight.isNight(46.72, 5.90, utc(2026, 11, 4, 23)))
    }

    @Test fun novemberSunsetAroundFivePmLocal() {
        // Coucher vers 16 h 15 UTC (17 h 15 à l'heure de Paris) début novembre.
        val set = DayNight.sunEventUtc(308, 46.72, 5.90, rising = false)!!
        assertEquals(16.3, set, 0.4)
        assertFalse(DayNight.isNight(46.72, 5.90, utc(2026, 11, 4, 15, 30)))
        assertTrue(DayNight.isNight(46.72, 5.90, utc(2026, 11, 4, 17, 0)))
    }

    @Test fun juneSunriseEarly() {
        val rise = DayNight.sunEventUtc(172, 46.72, 5.90, rising = true)!!
        assertEquals(3.75, rise, 0.4) // ~5 h 45 heure d'été
    }
}
