package fr.roadguard.core

import org.junit.Assert.assertEquals
import org.junit.Test

class SpeedMathTest {
    @Test fun convertsMetersPerSecondToKmh() {
        assertEquals(90, SpeedMath.toFix(true, 25f, 5f, 0L).kmh)
    }

    @Test fun gpsNoiseAtStandstillShowsZero() {
        assertEquals(0, SpeedMath.toFix(true, 0.7f, 20f, 0L).kmh)
    }

    @Test fun missingOrInvalidSpeedShowsZero() {
        assertEquals(0, SpeedMath.toFix(false, 30f, 5f, 0L).kmh)
        assertEquals(0, SpeedMath.toFix(true, Float.NaN, 5f, 0L).kmh)
    }

    @Test fun needleIsClampedToTheArc() {
        assertEquals(0f, SpeedMath.needleSweep(-5), 0f)
        assertEquals(120f, SpeedMath.needleSweep(100), 0.01f)
        assertEquals(240f, SpeedMath.needleSweep(350), 0f)
    }
}
