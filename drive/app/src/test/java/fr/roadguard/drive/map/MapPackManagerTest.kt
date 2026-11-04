package fr.roadguard.core.map

import fr.roadguard.core.radar.RadarCountries
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MapPackManagerTest {
    @Test fun parsesCatalogAndFlagsBrokenSources() {
        val packs = MapPackManager.parseCatalog("""{"schema":1,"packs":[
            {"id":"ch","name":"Suisse","url":"https://x/ch.pmtiles","sha256":"ab","size":575,"country_code":"CH"},
            {"id":"fr_ne","name":"France NE","availability":"invalid_source","unavailable_reason":"En-tête nul","url":"https://x","size":1,"country_code":"FR"}]}""")
        assertEquals(2, packs.size)
        assertNull(packs[0].unavailable)
        assertEquals("En-tête nul", packs[1].unavailable)
        assertEquals(575L, packs[0].size)
    }

    @Test fun convertsMapAtlasCountry() {
        val (csv, n) = RadarCountries.convert("""{"type":"FeatureCollection","features":[
            {"type":"Feature","geometry":{"type":"Point","coordinates":[4.9,52.37]},"properties":{"class":"spot","ref":"A1","maxspeed":"80"}},
            {"type":"Feature","geometry":{"type":"Point","coordinates":[4.8,52.3]},"properties":{"class":"signal"}},
            {"type":"Feature","geometry":{"type":"Point","coordinates":[4.8,52.3]},"properties":{"class":"unknown"}},
            {"type":"Feature","geometry":{"type":"LineString","coordinates":[[4,52],[5,53]]},"properties":{"class":"corridor"}}]}""", "NL")
        assertEquals(2, n)
        val radars = fr.roadguard.core.radar.RadarCatalog.parse(java.io.BufferedReader(java.io.StringReader(csv)))
        assertEquals(2, radars.size)
        assertEquals(80, radars[0].speedKmh)
        assertTrue(radars[1].label.startsWith("Radar feu rouge"))
    }
}
