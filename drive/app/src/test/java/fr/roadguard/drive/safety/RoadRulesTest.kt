package fr.roadguard.drive.safety

import fr.roadguard.core.safety.RoadRules
import org.junit.Assert.assertEquals
import org.junit.Test

class RoadRulesTest {
    @Test fun rainLowersFrenchLimits() {
        assertEquals(110, RoadRules.adjust(130, "FR", rain = true, young = false, personalMax = 0))
        assertEquals(100, RoadRules.adjust(110, "FR", rain = true, young = false, personalMax = 0))
        assertEquals(80, RoadRules.adjust(90, "FR", rain = true, young = false, personalMax = 0))
        assertEquals(80, RoadRules.adjust(80, "FR", rain = true, young = false, personalMax = 0))
        assertEquals(130, RoadRules.adjust(130, "FR", rain = false, young = false, personalMax = 0))
    }

    @Test fun youngDriverAndPersonalMax() {
        assertEquals(110, RoadRules.adjust(130, "FR", rain = false, young = true, personalMax = 0))
        assertEquals(90, RoadRules.adjust(130, "FR", rain = false, young = false, personalMax = 90))
        assertEquals(50, RoadRules.adjust(50, "FR", rain = false, young = false, personalMax = 90))
        assertEquals(130, RoadRules.adjust(130, "DE", rain = true, young = true, personalMax = 0))
    }
}
