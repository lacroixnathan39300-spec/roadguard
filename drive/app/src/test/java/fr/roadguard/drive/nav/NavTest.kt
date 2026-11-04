package fr.roadguard.core.nav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NavTest {

    @Test fun decodesReferencePolyline() {
        // Exemple de référence du format (précision 5).
        val pts = Polyline.decode("_p~iF~ps|U_ulLnnqC_mqNvxq`@", precision = 5)
        assertEquals(3, pts.size)
        assertEquals(38.5, pts[0].lat, 1e-6); assertEquals(-120.2, pts[0].lon, 1e-6)
        assertEquals(40.7, pts[1].lat, 1e-6); assertEquals(-120.95, pts[1].lon, 1e-6)
        assertEquals(43.252, pts[2].lat, 1e-6); assertEquals(-126.453, pts[2].lon, 1e-6)
    }

    @Test fun parsesPhotonResults() {
        val json = """{"features":[
            {"geometry":{"coordinates":[5.909,46.744]},"properties":{"name":"Champagnole","postcode":"39300","country":"France"}},
            {"geometry":{"coordinates":[5.9,46.7]},"properties":{"housenumber":"7","street":"Rue X","city":"Ney","postcode":"39300"}},
            {"properties":{"name":"sans géométrie"}}
        ]}"""
        val places = PlaceParser.parse(json)
        assertEquals(2, places.size)
        assertEquals("Champagnole", places[0].name)
        assertEquals(46.744, places[0].point.lat, 1e-9)
        assertEquals("7 Rue X", places[1].name)
        assertEquals("39300 Ney", places[1].detail)
    }

    @Test fun parsesValhallaRoute() {
        val shape = encode6(listOf(LatLon(46.0, 5.0), LatLon(46.001, 5.0), LatLon(46.002, 5.0))).replace("\\", "\\\\")
        val json = """{"trip":{"legs":[{"shape":"$shape","maneuvers":[
            {"type":1,"instruction":"Roulez vers le nord.","verbal_pre_transition_instruction":"Roulez vers le nord.","begin_shape_index":0},
            {"type":10,"instruction":"Tournez à droite sur Rue A.","verbal_transition_alert_instruction":"Tournez à droite sur Rue A.","verbal_pre_transition_instruction":"Tournez à droite sur Rue A.","begin_shape_index":1,"street_names":["Rue A"]},
            {"type":4,"instruction":"Vous êtes arrivé.","begin_shape_index":2}
        ]}],"summary":{"length":0.222,"time":30}}}"""
        val route = ValhallaParser.parse(json, LatLon(46.002, 5.0))
        assertEquals(3, route.points.size)
        assertEquals(46.001, route.points[1].lat, 1e-6)
        assertEquals(3, route.maneuvers.size)
        assertEquals("Rue A", route.maneuvers[1].street)
        assertTrue(route.maneuvers[2].isArrival)
        assertEquals(222.0, route.lengthM, 0.5)
    }

    private fun straightRoute(): Route {
        // Route plein nord sur ~3,3 km avec un virage à droite à mi-parcours.
        val pts = (0..30).map { LatLon(46.0 + it * 0.001, 5.0) }
        val maneuvers = listOf(
            Maneuver(1, "Départ", "Départ", "Roulez vers le nord.", 0, "", null),
            Maneuver(10, "Tournez à droite.", "Tournez à droite.", "Tournez à droite maintenant.", 15, "", null),
            Maneuver(4, "Arrivée", "Arrivée", "Vous êtes arrivé.", 30, "", null),
        )
        return Route(pts, maneuvers, 3330.0, 300.0, pts.last())
    }

    @Test fun guidanceTracksProgressAndSpeaksOnce() {
        val g = Guidance(straightRoute())
        assertEquals("Roulez vers le nord.", g.update(46.0, 5.0, 50.0).speak)
        // ~1,1 km avant le virage : rien à dire encore à 50 km/h (annonce à 500 m).
        val far = g.update(46.004, 5.0, 50.0)
        assertNull(far.speak)
        assertEquals(10, far.next!!.type)
        assertEquals(1223.0, far.toNextM.toDouble(), 15.0)
        // ~440 m avant : annonce anticipée.
        val early = g.update(46.011, 5.0, 50.0)
        assertNotNull(early.speak); assertTrue(early.speak!!.startsWith("Dans 450 mètres"))
        assertNull(g.update(46.0112, 5.0, 50.0).speak)
        // ~55 m avant : annonce immédiate, une seule fois.
        assertEquals("Tournez à droite maintenant.", g.update(46.01450, 5.0, 50.0).speak)
        assertNull(g.update(46.0146, 5.0, 50.0).speak)
    }

    @Test fun detectsOffRouteAfterThreeFixesAndArrival() {
        val g = Guidance(straightRoute())
        g.update(46.0, 5.0, 50.0)
        repeat(2) { assertFalse(g.update(46.005, 5.002, 50.0).offRoute) } // ~150 m à l'est
        assertTrue(g.update(46.005, 5.002, 50.0).offRoute)
        val g2 = Guidance(straightRoute())
        var state = g2.update(46.0, 5.0, 50.0)
        var lat = 46.0
        while (lat < 46.03) { lat += 0.001; state = g2.update(lat, 5.0, 50.0) }
        assertTrue(state.arrived)
    }

    @Test fun distancesAreRoundedForDisplayAndVoice() {
        assertEquals("1,2 km", Guidance.shortDistance(1234))
        assertEquals("350 m", Guidance.shortDistance(347))
        assertEquals("450 mètres", Guidance.roundDistance(441.0))
        assertEquals("1,5 kilomètre", Guidance.roundDistance(1480.0))
        assertEquals("2 kilomètres", Guidance.roundDistance(1980.0))
    }

    /** Encodeur minimal (précision 6) pour fabriquer des réponses de test. */
    private fun encode6(points: List<LatLon>): String {
        val sb = StringBuilder()
        var pLat = 0L; var pLon = 0L
        for (p in points) {
            val lat = Math.round(p.lat * 1e6); val lon = Math.round(p.lon * 1e6)
            enc(lat - pLat, sb); enc(lon - pLon, sb)
            pLat = lat; pLon = lon
        }
        return sb.toString()
    }

    private fun enc(v: Long, sb: StringBuilder) {
        var x = if (v < 0) (v shl 1).inv() else (v shl 1)
        while (x >= 0x20) { sb.append(((0x20 or (x and 0x1f).toInt()) + 63).toChar()); x = x shr 5 }
        sb.append((x + 63).toInt().toChar())
    }

    @Test fun parsesAlternatesAndOptions() {
        val shape = encode6(listOf(LatLon(46.0, 5.0), LatLon(46.002, 5.0))).replace("\\", "\\\\")
        fun trip(t: Int) = """{"legs":[{"shape":"$shape","maneuvers":[{"type":4,"instruction":"Arrivé","begin_shape_index":1}]}],"summary":{"length":1.5,"time":$t}}"""
        val json = """{"trip":${trip(60)},"alternates":[{"trip":${trip(90)}},{"trip":${trip(120)}}]}"""
        val all = ValhallaParser.parseAll(json, LatLon(46.002, 5.0))
        assertEquals(3, all.size)
        assertEquals(120.0, all[2].durationS, 1e-9)
        val req = org.json.JSONObject(ValhallaParser.request(LatLon(46.0, 5.0), null, LatLon(46.1, 5.1), 2, avoidTolls = true))
        assertEquals(2, req.getInt("alternates"))
        assertEquals(0.0, req.getJSONObject("costing_options").getJSONObject("auto").getDouble("use_tolls"), 1e-9)
        val plain = org.json.JSONObject(ValhallaParser.request(LatLon(46.0, 5.0), null, LatLon(46.1, 5.1)))
        assertEquals(false, plain.has("alternates") || plain.has("costing_options"))
    }

    @Test fun announcesCloseFollowingManeuver() {
        val pts = (0..30).map { LatLon(46.0 + it * 0.001, 5.0) }
        val maneuvers = listOf(
            Maneuver(1, "Départ", "Départ", "Roulez vers le nord.", 0, "", null),
            Maneuver(10, "Tournez à droite.", "Tournez à droite.", "Tournez à droite maintenant.", 15, "", null),
            Maneuver(15, "Tournez à gauche.", "Tournez à gauche.", "Tournez à gauche maintenant.", 17, "", null),
            Maneuver(4, "Arrivée", "Arrivée", "Vous êtes arrivé.", 30, "", null),
        )
        val g = Guidance(Route(pts, maneuvers, 3330.0, 300.0, pts.last()))
        g.update(46.0, 5.0, 50.0)
        val near = g.update(46.01450, 5.0, 50.0)
        assertEquals(15, near.then?.type)
        assertEquals("Tournez à droite maintenant, puis tournez à gauche.", near.speak)
    }
}
