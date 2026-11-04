package fr.roadguard.drive.nav

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
