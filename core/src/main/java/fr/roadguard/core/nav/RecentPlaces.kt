package fr.roadguard.core.nav

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Dernières destinations (8 au maximum), mémorisées sur le poste. */
class RecentPlaces(context: Context) {
    private val prefs = context.getSharedPreferences("navigation", Context.MODE_PRIVATE)

    fun load(): List<Place> = runCatching {
        val array = JSONArray(prefs.getString("recents", "[]"))
        (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            Place(o.getString("name"), o.optString("detail"), LatLon(o.getDouble("lat"), o.getDouble("lon")))
        }
    }.getOrDefault(emptyList())

    /** Adresses enregistrées « Maison » et « Travail » (clé : "home" ou "work"). */
    fun saved(slot: String): Place? = runCatching {
        val o = JSONObject(prefs.getString(slot, null) ?: return null)
        Place(o.getString("name"), o.optString("detail"), LatLon(o.getDouble("lat"), o.getDouble("lon")))
    }.getOrNull()

    fun save(slot: String, place: Place) {
        prefs.edit().putString(slot, JSONObject().put("name", place.name).put("detail", place.detail)
            .put("lat", place.point.lat).put("lon", place.point.lon).toString()).apply()
    }

    /** Favoris (sans limite), dans l'ordre d'ajout. */
    fun favorites(): List<Place> = read("favorites")

    /** Ajoute ou retire [place] des favoris ; renvoie la nouvelle liste. */
    fun toggleFavorite(place: Place): List<Place> {
        val current = favorites()
        val list = if (current.any { same(it, place) }) current.filterNot { same(it, place) } else current + place
        write("favorites", list)
        return list
    }

    private fun same(a: Place, b: Place) = a.name == b.name && Math.abs(a.point.lat - b.point.lat) < 1e-5 && Math.abs(a.point.lon - b.point.lon) < 1e-5

    private fun read(key: String): List<Place> = runCatching {
        val array = JSONArray(prefs.getString(key, "[]"))
        (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            Place(o.getString("name"), o.optString("detail"), LatLon(o.getDouble("lat"), o.getDouble("lon")))
        }
    }.getOrDefault(emptyList())

    private fun write(key: String, list: List<Place>) {
        val array = JSONArray()
        list.forEach { array.put(JSONObject().put("name", it.name).put("detail", it.detail).put("lat", it.point.lat).put("lon", it.point.lon)) }
        prefs.edit().putString(key, array.toString()).apply()
    }

    fun add(place: Place): List<Place> {
        val list = (listOf(place) + load().filterNot { it.name == place.name && it.detail == place.detail }).take(8)
        val array = JSONArray()
        list.forEach {
            array.put(JSONObject().put("name", it.name).put("detail", it.detail).put("lat", it.point.lat).put("lon", it.point.lon))
        }
        prefs.edit().putString("recents", array.toString()).apply()
        return list
    }
}
