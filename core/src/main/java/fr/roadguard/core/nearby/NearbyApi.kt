package fr.roadguard.core.nearby

import fr.roadguard.core.net.Net
import fr.roadguard.core.radar.Geo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Lieu utile à proximité (station, borne, parking, aire, toilettes). */
data class NearbyPlace(val name: String, val detail: String, val lat: Double, val lon: Double, val distanceM: Double, val price: Double? = null)

enum class Fuel(val label: String, val field: String) {
    GAZOLE("Gazole", "gazole_prix"), E10("SP95-E10", "e10_prix"), SP95("SP95", "sp95_prix"),
    SP98("SP98", "sp98_prix"), E85("E85", "e85_prix"), GPLC("GPLc", "gplc_prix"),
}

enum class PoiKind(val label: String, val emoji: String, val filter: String) {
    CHARGING("Bornes de recharge", "🔌", "node[\"amenity\"=\"charging_station\"]"),
    PARKING("Parkings", "🅿", "nwr[\"amenity\"=\"parking\"][\"access\"!=\"private\"]"),
    REST("Aires de repos", "🌳", "nwr[\"highway\"~\"rest_area|services\"]"),
    TOILETS("Toilettes", "🚻", "node[\"amenity\"=\"toilets\"]"),
}

/**
 * Prix des carburants en France (données publiques du ministère de l'Économie, mises à jour en continu)
 * et lieux utiles d'OpenStreetMap, autour d'une position.
 */
object NearbyApi {
    suspend fun fuel(lat: Double, lon: Double, fuel: Fuel, radiusKm: Int = 10): List<NearbyPlace> = withContext(Dispatchers.IO) {
        val where = java.net.URLEncoder.encode("within_distance(geom, geom'POINT($lon $lat)', ${radiusKm}km) and ${fuel.field} is not null", "UTF-8")
        val url = "https://data.economie.gouv.fr/api/explore/v2.1/catalog/datasets/prix-des-carburants-en-france-flux-instantane-v2/records" +
            "?where=$where&order_by=${fuel.field}&limit=40"
        val r = Net.fetch(url)
        if (r.code != 200) throw java.io.IOException("Prix indisponibles (${r.code})")
        val a = JSONObject(String(r.body)).optJSONArray("results") ?: return@withContext emptyList()
        (0 until a.length()).mapNotNull { i ->
            val o = a.getJSONObject(i)
            val g = o.optJSONObject("geom")
            val la = g?.optDouble("lat") ?: o.optString("latitude").toDoubleOrNull()?.div(100_000.0) ?: return@mapNotNull null
            val lo = g?.optDouble("lon") ?: o.optString("longitude").toDoubleOrNull()?.div(100_000.0) ?: return@mapNotNull null
            val price = o.optDouble(fuel.field, Double.NaN).takeIf { it.isFinite() } ?: return@mapNotNull null
            val addr = listOf(o.optString("adresse"), o.optString("ville")).filter { it.isNotBlank() && it != "null" }.joinToString(", ")
            NearbyPlace(addr.ifBlank { "Station" }, fuel.label, la, lo, Geo.distanceMeters(lat, lon, la, lo), price)
        }.sortedBy { it.price }
    }

    suspend fun poi(lat: Double, lon: Double, kind: PoiKind, radiusM: Int = 8000): List<NearbyPlace> = withContext(Dispatchers.IO) {
        val q = "[out:json][timeout:25];${kind.filter}(around:$radiusM,$lat,$lon);out center 60;"
        val r = Net.fetch("https://overpass-api.de/api/interpreter", "POST",
            ("data=" + java.net.URLEncoder.encode(q, "UTF-8")).toByteArray(), "application/x-www-form-urlencoded")
        if (r.code != 200) throw java.io.IOException("Recherche indisponible (${r.code})")
        val els = JSONObject(String(r.body)).optJSONArray("elements") ?: return@withContext emptyList()
        (0 until els.length()).mapNotNull { i ->
            val e = els.getJSONObject(i)
            val c = e.optJSONObject("center")
            val la = c?.optDouble("lat") ?: e.optDouble("lat", Double.NaN)
            val lo = c?.optDouble("lon") ?: e.optDouble("lon", Double.NaN)
            if (!la.isFinite() || !lo.isFinite()) return@mapNotNull null
            val t = e.optJSONObject("tags") ?: JSONObject()
            val name = t.optString("name").ifBlank { t.optString("operator").ifBlank { kind.label.removeSuffix("s") } }
            val detail = buildList {
                t.optString("capacity").takeIf { it.isNotBlank() }?.let { add(if (kind == PoiKind.CHARGING) "$it points" else "$it places") }
                t.optString("fee").takeIf { it == "yes" }?.let { add("payant") }
                t.optString("fee").takeIf { it == "no" }?.let { add("gratuit") }
                t.optString("opening_hours").takeIf { it.isNotBlank() }?.let { add(it) }
                t.optString("socket:type2").takeIf { it.isNotBlank() }?.let { add("Type 2") }
                t.optString("socket:type2_combo").takeIf { it.isNotBlank() }?.let { add("CCS") }
            }.joinToString(" · ")
            NearbyPlace(name, detail, la, lo, Geo.distanceMeters(lat, lon, la, lo))
        }.sortedBy { it.distanceM }.take(40)
    }
}
