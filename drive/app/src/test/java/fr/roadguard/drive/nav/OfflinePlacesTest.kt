package fr.roadguard.drive.nav

import fr.roadguard.core.nav.OfflinePlaces
import org.junit.Assert.assertEquals
import org.junit.Test

class OfflinePlacesTest {
    @Test fun normalizes() {
        assertEquals("lons le saunier", OfflinePlaces.normalize("Lons-le-Saunier"))
        assertEquals("saint claude", OfflinePlaces.normalize("St-Claude"))
        assertEquals("sainte agnes", OfflinePlaces.normalize("Ste Agnès"))
    }
}
