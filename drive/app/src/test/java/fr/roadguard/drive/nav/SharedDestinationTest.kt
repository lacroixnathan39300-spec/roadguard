package fr.roadguard.core.nav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SharedDestinationTest {
    private fun assertCoords(lat: Double, lon: Double, text: String) {
        val c = SharedDestination.coordinates(text)!!
        assertEquals(lat, c.lat, 1e-6); assertEquals(lon, c.lon, 1e-6)
    }

    @Test fun readsCoordinatesFromCommonLinks() {
        assertCoords(46.7444, 5.9101, "https://www.google.com/maps/place/Champagnole/@46.7, 5.9,13z/data=!3d46.7444!4d5.9101")
        assertCoords(46.71, 5.83, "https://www.google.com/maps/@46.71,5.83,15z")
        assertCoords(46.72, 5.9, "https://maps.google.com/?q=46.72,5.9")
        assertCoords(46.72, 5.9, "geo:46.72,5.9?q=Ney")
        assertCoords(46.73, 5.91, "https://waze.com/ul?ll=46.73%2C5.91&navigate=yes")
        assertCoords(46.7187, 5.8335, "Rendez-vous ici : 46.7187, 5.8335")
    }

    @Test fun keepsPlaceNameAndIgnoresLinks() {
        assertEquals("Mairie de Champagnole", SharedDestination.label("Mairie de Champagnole\nhttps://maps.app.goo.gl/abc"))
        assertEquals("7 rue de la Paix, 39300 Champagnole", SharedDestination.label("7 rue de la Paix, 39300 Champagnole"))
        assertNull(SharedDestination.coordinates("Mairie de Champagnole"))
        assertEquals("https://maps.app.goo.gl/abc", SharedDestination.firstUrl("Mairie\nhttps://maps.app.goo.gl/abc"))
    }

    @Test fun extractsQueryFromSearchLinks() {
        assertEquals("Lac de Chalain", SharedDestination.queryFromUrl("https://www.google.com/maps/search/Lac+de+Chalain/"))
        assertEquals("Ney 39300", SharedDestination.queryFromUrl("https://maps.google.com/?q=Ney+39300&hl=fr"))
    }
}
