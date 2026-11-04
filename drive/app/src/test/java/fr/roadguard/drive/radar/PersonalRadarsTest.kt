package fr.roadguard.drive.radar

import fr.roadguard.core.radar.PersonalRadars
import fr.roadguard.core.radar.RadarCatalog
import fr.roadguard.core.radar.RadarKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PersonalRadarsTest {
    @Test fun simpleWithoutHeader() {
        val (r, skipped) = PersonalRadars.parseCsv("46.714,5.5848,70\n46.64106,5.50103\nnimporte quoi\n")
        assertEquals(2, r.size); assertEquals(1, skipped)
        assertEquals(46.714, r[0].lat, 1e-9); assertEquals(70, r[0].speedKmh); assertNull(r[1].speedKmh)
    }

    @Test fun semicolonDecimalComma() {
        val (r, _) = PersonalRadars.parseCsv("latitude;longitude;vitesse;type\n46,714;5,5848;70;feu rouge\n")
        assertEquals(1, r.size); assertEquals(5.5848, r[0].lon, 1e-9)
        assertEquals(RadarKind.REDLIGHT, r[0].kind)
    }

    @Test fun igoFormat() {
        val (r, _) = PersonalRadars.parseCsv("X,Y,TYPE,SPEED,DIRTYPE,DIRECTION\n5.5848,46.714,1,70,1,205\n5.50103,46.64106,4,90,0,0\n")
        assertEquals(2, r.size)
        assertEquals(46.714, r[0].lat, 1e-9); assertEquals(205, r[0].directionDeg)
        assertEquals(RadarKind.AVERAGE, r[1].kind)
    }

    @Test fun swappedWithoutHeaderAndBadSpeed() {
        val (r, _) = PersonalRadars.parseCsv("150.5,-33.8,999\n")
        assertEquals(-33.8, r[0].lat, 1e-9); assertNull(r[0].speedKmh)
    }

    @Test fun roundTripThroughCatalog() {
        val (r, _) = PersonalRadars.parseCsv("lat,lon,speed,type\n46.714,5.5848,70,mobile\n46.7,5.5,50,\n")
        val back = RadarCatalog.parse(PersonalRadars.toCsv(r).reader().buffered())
        assertEquals(2, back.size)
        assertEquals(RadarKind.MOBILE, back[0].kind); assertEquals(RadarKind.FIXED, back[1].kind)
        assertEquals(50, back[1].speedKmh)
    }

    @Test fun ascFormat() {
        val (r, skipped) = PersonalRadars.parseAsc("5.5848,46.714,\"Radar fixe 70\"\n; commentaire\n5.50103,46.64106,\"Feu rouge\"\nbad line\n")
        assertEquals(2, r.size); assertEquals(1, skipped)
        assertEquals(46.714, r[0].lat, 1e-9); assertEquals(70, r[0].speedKmh)
        assertEquals(RadarKind.REDLIGHT, r[1].kind)
    }
}
