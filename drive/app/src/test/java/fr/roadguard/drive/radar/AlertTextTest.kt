package fr.roadguard.core.radar

import org.junit.Assert.assertEquals
import org.junit.Test

class AlertTextTest {
    private val radar = Radar("r", 46.0, 5.0, RadarKind.FIXED, "Radar", 80)

    @Test fun bannerRoundsDistanceAndShowsLimit() {
        assertEquals("Radar fixe · 80 km/h · 450 m", AlertText.banner(RadarAlert(radar, 437, true)))
    }

    @Test fun spokenSentenceIsFrench() {
        assertEquals("Radar fixe dans 500 mètres, limité à 80", AlertText.spoken(RadarAlert(radar, 512, true)))
    }

    @Test fun averageVerdicts() {
        assertEquals("Moyenne 78/80 km/h · OK", AlertText.average(AverageStatus(78, 80, 900, false)))
        assertEquals("Tronçon terminé · moyenne 86/80 km/h · trop vite", AlertText.average(AverageStatus(86, 80, 4000, true)))
        assertEquals("Moyenne 70 km/h", AlertText.average(AverageStatus(70, null, 900, false)))
    }

    @Test fun proximityBeepsSpeedUp() {
        assertEquals(1500L, BeepCadence.intervalMs(800))
        assertEquals(160L, BeepCadence.intervalMs(20))
        assertEquals(true, BeepCadence.intervalMs(100) < BeepCadence.intervalMs(300))
        assertEquals("Radar fixe, limité à 80", AlertText.spokenShort(RadarAlert(radar, 512, true)))
    }
}
