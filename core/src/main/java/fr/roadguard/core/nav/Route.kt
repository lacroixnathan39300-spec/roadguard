package fr.roadguard.core.nav

import org.json.JSONObject

data class LatLon(val lat: Double, val lon: Double)

/** Une manœuvre du trajet (tourner, rond-point, arrivée…). */
data class Maneuver(
    val type: Int,
    val instruction: String,
    /** Phrase courte annoncée à l'approche (précédée de « Dans 500 mètres, »). */
    val verbalAlert: String,
    /** Phrase à dire au moment de la manœuvre. */
    val verbalNow: String,
    /** Index du premier point du tracé concerné par la manœuvre. */
    val shapeIndex: Int,
    val street: String,
    val roundaboutExit: Int?,
) {
    val isArrival: Boolean get() = type in 4..6
}

data class Route(
    val points: List<LatLon>,
    val maneuvers: List<Maneuver>,
    val lengthM: Double,
    val durationS: Double,
    val destination: LatLon,
    /** Kilomètres sur routes à péage (estimation du coût : ~0,10 €/km pour une voiture en France). */
    val tollKm: Double = 0.0,
) {
    /** Coût estimé des péages en euros (null s'il n'y en a pas). */
    val tollEuros: Int? get() = if (tollKm < 1) null else Math.round(tollKm * 0.10).toInt().coerceAtLeast(1)
}

/** Lecture de la réponse du calculateur d'itinéraire Valhalla (consignes en français). */
object ValhallaParser {
    fun parse(json: String, destination: LatLon): Route = parseTrip(JSONObject(json).getJSONObject("trip"), destination)

    /** Trajet principal puis variantes proposées par Valhalla (« alternates »). */
    fun parseAll(json: String, destination: LatLon): List<Route> {
        val root = JSONObject(json)
        val out = mutableListOf(parseTrip(root.getJSONObject("trip"), destination))
        root.optJSONArray("alternates")?.let { alts ->
            for (i in 0 until alts.length()) {
                alts.optJSONObject(i)?.optJSONObject("trip")?.let { t -> runCatching { out += parseTrip(t, destination) } }
            }
        }
        return out
    }

    private fun parseTrip(trip: JSONObject, destination: LatLon): Route {
        // Plusieurs étapes = plusieurs « legs » : on les met bout à bout (une seule polyligne, une seule liste de consignes).
        val legs = trip.getJSONArray("legs")
        val points = ArrayList<LatLon>()
        val maneuvers = ArrayList<Maneuver>()
        var tollKm = 0.0
        for (li in 0 until legs.length()) {
            val leg = legs.getJSONObject(li)
            val legPoints = Polyline.decode(leg.getString("shape"), precision = 6)
            // Le premier point d'une étape répète le dernier de la précédente.
            val skip = if (points.isNotEmpty() && legPoints.isNotEmpty()) 1 else 0
            val offset = points.size - skip
            points.addAll(legPoints.drop(skip))
            val list = leg.getJSONArray("maneuvers")
            val last = li == legs.length() - 1
            for (i in 0 until list.length()) {
                val m = list.getJSONObject(i)
                var type = m.optInt("type")
                var instruction = m.optString("instruction")
                // Départ d'une étape intermédiaire : inutile. Arrivée à une étape : on continue.
                if (li > 0 && type in 1..3) continue
                if (!last && type in 4..6) { type = 8; instruction = "Étape atteinte : on continue." }
                if (m.optBoolean("toll")) tollKm += m.optDouble("length", 0.0)
                val streets = m.optJSONArray("street_names")
                maneuvers += Maneuver(
                    type = type,
                    instruction = instruction,
                    verbalAlert = if (!last && m.optInt("type") in 4..6) "Étape dans quelques instants." else m.optString("verbal_transition_alert_instruction").ifBlank { instruction },
                    verbalNow = if (!last && m.optInt("type") in 4..6) "Étape atteinte. On continue." else m.optString("verbal_pre_transition_instruction").ifBlank { instruction },
                    shapeIndex = (offset + m.optInt("begin_shape_index")).coerceIn(0, (offset + legPoints.size - 1).coerceAtLeast(0)),
                    street = if (streets != null && streets.length() > 0) streets.optString(0) else "",
                    roundaboutExit = m.optInt("roundabout_exit_count", 0).takeIf { it > 0 },
                )
            }
        }
        val summary = trip.getJSONObject("summary")
        return Route(
            points = points,
            maneuvers = maneuvers,
            lengthM = summary.optDouble("length") * 1000.0,
            durationS = summary.optDouble("time"),
            destination = destination,
            tollKm = tollKm,
        )
    }

    /**
     * Requête Valhalla : voiture, kilomètres, consignes dans la langue choisie, départ dans le sens de roulage.
     * [vias] : étapes dans l'ordre ; [narrow] : éviter routes étroites, chemins et centres ; [eco] : plus économe (moins vite).
     */
    fun request(
        from: LatLon, headingDeg: Double?, to: LatLon,
        alternates: Int = 0, avoidTolls: Boolean = false, avoidHighways: Boolean = false,
        vias: List<LatLon> = emptyList(), narrow: Boolean = false, eco: Boolean = false, language: String = "fr-FR",
    ): String {
        val start = JSONObject().put("lat", from.lat).put("lon", from.lon)
        if (headingDeg != null && !headingDeg.isNaN()) start.put("heading", headingDeg.toInt()).put("heading_tolerance", 60)
        val locations = org.json.JSONArray().put(start)
        vias.forEach { locations.put(JSONObject().put("lat", it.lat).put("lon", it.lon).put("type", "break")) }
        locations.put(JSONObject().put("lat", to.lat).put("lon", to.lon))
        val auto = JSONObject()
        if (avoidTolls) auto.put("use_tolls", 0.0)
        if (avoidHighways) auto.put("use_highways", 0.0)
        if (narrow) auto.put("use_living_streets", 0.0).put("use_tracks", 0.0).put("exclude_unpaved", true).put("service_penalty", 300).put("service_factor", 4.0)
        // Plus économe : 110 km/h au plus (bien moins de carburant que 130), autoroutes un peu moins prisées.
        if (eco) auto.put("top_speed", 110).put("use_highways", if (avoidHighways) 0.0 else 0.6)
        return JSONObject()
            .put("locations", locations)
            .put("costing", "auto")
            .apply { if (alternates > 0 && vias.isEmpty()) put("alternates", alternates) }
            .apply { if (auto.length() > 0) put("costing_options", JSONObject().put("auto", auto)) }
            .put("directions_options", JSONObject().put("units", "kilometers").put("language", language))
            .toString()
    }
}

/** Décodage du format « polyline » (précision 5 ou 6 décimales). */
object Polyline {
    fun decode(encoded: String, precision: Int = 6): List<LatLon> {
        val factor = Math.pow(10.0, precision.toDouble())
        val out = ArrayList<LatLon>()
        var index = 0
        var lat = 0L
        var lon = 0L
        while (index < encoded.length) {
            var result = 0L
            var shift = 0
            var b: Int
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f).toLong() shl shift)
                shift += 5
            } while (b >= 0x20 && index < encoded.length)
            lat += if (result and 1L != 0L) (result shr 1).inv() else (result shr 1)
            result = 0L
            shift = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f).toLong() shl shift)
                shift += 5
            } while (b >= 0x20 && index < encoded.length)
            lon += if (result and 1L != 0L) (result shr 1).inv() else (result shr 1)
            out += LatLon(lat / factor, lon / factor)
        }
        return out
    }
}
