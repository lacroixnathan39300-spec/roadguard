package fr.roadguard.drive.radar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.BufferedReader
import java.io.StringReader

class RadarCatalogTest {
    private fun read(text: String) = RadarCatalog.parse(BufferedReader(StringReader(text)))

    @Test fun fullFormatKeepsInventoryAndDropsUnconfirmed() {
        val radars = read(
            """
            id;type;lat;lon;country;category;speed_kmh;direction_deg;status;sources;source_ids;speed_conflicts
            a;Radar fixe;46.5;5.5;FR;fixed;80;;inventory;gouv;x;
            b;Zone possible;46.6;5.6;FR;temporary;;;historical;lufop;y;
            c;Feu rouge;46.7;5.7;FR;redlight;;;inventory;gouv;z;
            """.trimIndent()
        )
        assertEquals(listOf("a", "c"), radars.map { it.id })
        assertEquals(80, radars[0].speedKmh)
        assertEquals(RadarKind.REDLIGHT, radars[1].kind)
    }

    @Test fun shortMapAtlasFormatUsesTheLabel() {
        val radars = read(
            """
            id;type;lat;lon;country
            m1;Radar fixe (MapAtlas);43.6;11.1;IT
            m2;Radar vitesse moyenne (MapAtlas);43.7;11.2;IT
            m3;Radar feu rouge (MapAtlas);43.8;11.3;IT
            """.trimIndent()
        )
        assertEquals(listOf(RadarKind.FIXED, RadarKind.AVERAGE, RadarKind.REDLIGHT), radars.map { it.kind })
    }

    @Test fun malformedLinesAreSkipped() {
        val radars = read("id;type;lat;lon\nx;Radar fixe;abc;5\ny;Radar fixe;95;5\nz;Radar fixe;46;5\n")
        assertEquals(listOf("z"), radars.map { it.id })
    }

    @Test fun worksAreNotShown() {
        assertNull(RadarCatalog.kindOf("works", "Travaux"))
    }
}
