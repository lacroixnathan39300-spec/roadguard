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
        val leg = trip.getJSONArray("legs").getJSONObject(0)
        val points = Polyline.decode(leg.getString("shape"), precision = 6)
        val list = leg.getJSONArray("maneuvers")
        val maneuvers = (0 until list.length()).map { i ->
            val m = list.getJSONObject(i)
            val instruction = m.optString("instruction")
            val streets = m.optJSONArray("street_names")
            Maneuver(
                type = m.optInt("type"),
                instruction = instruction,
                verbalAlert = m.optString("verbal_transition_alert_instruction").ifBlank { instruction },
                verbalNow = m.optString("verbal_pre_transition_instruction").ifBlank { instruction },
                shapeIndex = m.optInt("begin_shape_index").coerceIn(0, (points.size - 1).coerceAtLeast(0)),
                street = if (streets != null && streets.length() > 0) streets.optString(0) else "",
                roundaboutExit = m.optInt("roundabout_exit_count", 0).takeIf { it > 0 },
            )
        }
        val tollKm = (0 until list.length()).sumOf { i -> list.getJSONObject(i).let { m -> if (m.optBoolean("toll")) m.optDouble("length", 0.0) else 0.0 } }
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

    /** Requête Valhalla : voiture, kilomètres, consignes en français, départ dans le sens de roulage. */
    fun request(
        from: LatLon, headingDeg: Double?, to: LatLon,
        alternates: Int = 0, avoidTolls: Boolean = false, avoidHighways: Boolean = false,
    ): String {
        val start = JSONObject().put("lat", from.lat).put("lon", from.lon)
        if (headingDeg != null && !headingDeg.isNaN()) start.put("heading", headingDeg.toInt()).put("heading_tolerance", 60)
        val locations = org.json.JSONArray()
            .put(start)
            .put(JSONObject().put("lat", to.lat).put("lon", to.lon))
        val auto = JSONObject()
        if (avoidTolls) auto.put("use_tolls", 0.0)
        if (avoidHighways) auto.put("use_highways", 0.0)
        return JSONObject()
            .put("locations", locations)
            .put("costing", "auto")
            .apply { if (alternates > 0) put("alternates", alternates) }
            .apply { if (auto.length() > 0) put("costing_options", JSONObject().put("auto", auto)) }
            .put("directions_options", JSONObject().put("units", "kilometers").put("language", "fr-FR"))
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
