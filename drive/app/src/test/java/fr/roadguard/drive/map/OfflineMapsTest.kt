package fr.roadguard.core.map

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class OfflineMapsTest {
    private fun header(minLon: Double, minLat: Double, maxLon: Double, maxLat: Double, version: Int = 3): ByteArray {
        val b = ByteBuffer.allocate(127).order(ByteOrder.LITTLE_ENDIAN)
        b.put("PMTiles".toByteArray()); b.put(version.toByte())
        b.putInt(102, (minLon * 1e7).toInt()); b.putInt(106, (minLat * 1e7).toInt())
        b.putInt(110, (maxLon * 1e7).toInt()); b.putInt(114, (maxLat * 1e7).toInt())
        return b.array()
    }

    @Test fun readsPackBoundsFromHeader() {
        val p = OfflineMaps.parseHeader(File("ch.pmtiles"), header(5.9, 45.8, 10.5, 47.9))!!
        assertEquals(45.8, p.minLat, 1e-6); assertEquals(10.5, p.maxLon, 1e-6)
        assertTrue(p.contains(46.5, 6.6)); assertFalse(p.contains(46.7, 5.5))
        assertNull(OfflineMaps.parseHeader(File("x"), header(5.9, 45.8, 10.5, 47.9, version = 2)))
        assertNull(OfflineMaps.parseHeader(File("x"), ByteArray(127)))
    }

    @Test fun picksThePackCoveringThePosition() {
        val ch = OfflineMaps.parseHeader(File("ch"), header(5.9, 45.8, 10.5, 47.9))!!
        val fr = OfflineMaps.parseHeader(File("fr"), header(-5.0, 42.0, 8.0, 51.0))!!
        assertEquals("fr", OfflineMaps.packFor(listOf(ch, fr), 46.72, 5.9)?.file?.name)
        assertEquals("ch", OfflineMaps.packFor(listOf(ch, fr), 46.5, 9.0)?.file?.name)
        assertNull(OfflineMaps.packFor(listOf(ch), 40.0, 0.0))
    }

    @Test fun styleUsesLocalPackFontsAndColours() {
        val template = """{"version":8,"glyphs":"@glyphs","sources":{
            "roads":{"type":"vector","tiles":["@vector"]},"osm-online":{"type":"raster","tiles":["@raster"]}},
            "layers":[{"id":"bg","type":"background","paint":{"background-color":"@background"}},
            {"id":"osm","type":"raster","source":"osm-online"},
            {"id":"water","type":"fill","source":"roads","source-layer":"water","paint":{"fill-color":"@water"}}]}"""
        val s = JSONObject(OfflineMaps.buildStyle(template, "/data/x/ch.pmtiles", nightMode = true))
        assertEquals("pmtiles://file:///data/x/ch.pmtiles", s.getJSONObject("sources").getJSONObject("roads").getString("url"))
        assertFalse(s.getJSONObject("sources").has("osm-online"))
        assertEquals(2, s.getJSONArray("layers").length())
        assertEquals("#10151C", s.getJSONArray("layers").getJSONObject(0).getJSONObject("paint").getString("background-color"))
        assertTrue(s.getString("glyphs").startsWith("asset://glyphs/"))
    }
}
