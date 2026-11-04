package fr.roadguard.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fr.roadguard.core.net.Net
import fr.roadguard.core.radar.Geo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

data class Weather(val tempC: Int, val emoji: String, val label: String)

/**
 * Météo (Open-Meteo, gratuit, sans compte) et adresse actuelle (Photon, OpenStreetMap).
 * Mises à jour rares : météo toutes les 20 min ou après 15 km, adresse après 300 m (au plus toutes les 30 s).
 */
class LocalInfo(private val scope: CoroutineScope) {
    var weather by mutableStateOf<Weather?>(null); private set
    var address by mutableStateOf<String?>(null); private set

    private var weatherAt = 0L; private var weatherLat = 0.0; private var weatherLon = 0.0
    private var addressAt = 0L; private var addressLat = 0.0; private var addressLon = 0.0
    private var busy = false

    fun onFix(lat: Double, lon: Double, now: Long) {
        if (busy) return
        val weatherDue = weather == null && now - weatherAt > 60_000 || now - weatherAt > 20 * 60_000 ||
            Geo.distanceMeters(lat, lon, weatherLat, weatherLon) > 15_000 && now - weatherAt > 60_000
        val addressDue = now - addressAt > 30_000 && (address == null || Geo.distanceMeters(lat, lon, addressLat, addressLon) > 300)
        if (!weatherDue && !addressDue) return
        busy = true
        scope.launch(Dispatchers.IO) {
            if (weatherDue) {
                weatherAt = now; weatherLat = lat; weatherLon = lon
                runCatching {
                    val r = Net.fetch("https://api.open-meteo.com/v1/forecast?latitude=%.3f&longitude=%.3f&current=temperature_2m,weather_code,is_day"
                        .format(java.util.Locale.US, lat, lon))
                    val c = JSONObject(String(r.body)).getJSONObject("current")
                    weather = describe(c.getDouble("temperature_2m"), c.getInt("weather_code"), c.optInt("is_day", 1) == 1)
                }
            }
            if (addressDue) {
                addressAt = now; addressLat = lat; addressLon = lon
                runCatching {
                    val r = Net.fetch("https://photon.komoot.io/reverse?lat=%.5f&lon=%.5f&lang=fr&limit=1".format(java.util.Locale.US, lat, lon))
                    val p = JSONObject(String(r.body)).getJSONArray("features").getJSONObject(0).getJSONObject("properties")
                    val street = p.optString("street").ifBlank { p.optString("name") }
                    val city = p.optString("city").ifBlank { p.optString("town").ifBlank { p.optString("village") } }
                    address = listOf(street, city).filter { it.isNotBlank() }.distinct().joinToString(", ").ifBlank { null }
                }
            }
            busy = false
        }
    }

    companion object {
        /** Codes météo WMO → symbole et libellé. */
        fun describe(temp: Double, code: Int, day: Boolean): Weather {
            val (emoji, label) = when (code) {
                0 -> (if (day) "☀️" else "🌙") to "Dégagé"
                1, 2 -> (if (day) "🌤" else "🌙") to "Peu nuageux"
                3 -> "☁️" to "Couvert"
                45, 48 -> "🌫" to "Brouillard"
                in 51..57 -> "🌦" to "Bruine"
                in 61..67, in 80..82 -> "🌧" to "Pluie"
                in 71..77, 85, 86 -> "❄️" to "Neige"
                in 95..99 -> "⛈" to "Orage"
                else -> "🌡" to ""
            }
            return Weather(Math.round(temp).toInt(), emoji, label)
        }
    }
}
