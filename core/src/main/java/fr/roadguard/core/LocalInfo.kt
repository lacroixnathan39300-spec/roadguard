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
    /** Pays actuel (code ISO, ex. FR, CH), d'après l'adresse. */
    var country by mutableStateOf<String?>(null); private set
    /** Il pleut (limitations abaissées en France). */
    var raining by mutableStateOf(false); private set
    /** Danger météo ici (« Risque de verglas »…), ou null. */
    var hazard by mutableStateOf<String?>(null); private set
    /** Danger météo sur l'itinéraire, à ~20 km devant, ou null. */
    var hazardAhead by mutableStateOf<String?>(null); private set
    private var aheadAt = 0L

    private var weatherAt = 0L; private var weatherLat = 0.0; private var weatherLon = 0.0
    private var addressAt = 0L; private var addressLat = 0.0; private var addressLon = 0.0
    private var busy = false

    fun onFix(lat: Double, lon: Double, now: Long) {
        if (busy) return
        val weatherDue = weather == null && now - weatherAt > 60_000 || now - weatherAt > 10 * 60_000 ||
            Geo.distanceMeters(lat, lon, weatherLat, weatherLon) > 15_000 && now - weatherAt > 60_000
        val addressDue = now - addressAt > 30_000 && (address == null || Geo.distanceMeters(lat, lon, addressLat, addressLon) > 300)
        if (!weatherDue && !addressDue) return
        busy = true
        scope.launch(Dispatchers.IO) {
            if (weatherDue) {
                weatherAt = now; weatherLat = lat; weatherLon = lon
                runCatching {
                    val c = current(lat, lon)
                    weather = describe(c.getDouble("temperature_2m"), c.getInt("weather_code"), c.optInt("is_day", 1) == 1)
                    raining = isRaining(c)
                    hazard = hazardOf(c)
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
                    p.optString("countrycode").takeIf { it.length == 2 }?.let { country = it.uppercase() }
                }
            }
            busy = false
        }
    }

    /** Météo à un point de l'itinéraire, ~20 km devant (toutes les 15 min au plus). */
    fun checkAhead(lat: Double, lon: Double, now: Long) {
        if (now - aheadAt < 15 * 60_000) return
        aheadAt = now
        scope.launch(Dispatchers.IO) { runCatching { hazardAhead = hazardOf(current(lat, lon)) } }
    }

    private fun current(lat: Double, lon: Double): JSONObject {
        val r = Net.fetch("https://api.open-meteo.com/v1/forecast?latitude=%.3f&longitude=%.3f&current=temperature_2m,weather_code,is_day,precipitation"
            .format(java.util.Locale.US, lat, lon))
        return JSONObject(String(r.body)).getJSONObject("current")
    }

    companion object {
        fun isRaining(c: JSONObject): Boolean {
            val code = c.optInt("weather_code"); val mm = c.optDouble("precipitation", 0.0)
            return mm >= 0.2 || code in 51..67 || code in 80..82 || code in 95..99
        }

        /** Danger météo pour la conduite, ou null. */
        fun hazardOf(c: JSONObject): String? {
            val code = c.optInt("weather_code"); val t = c.optDouble("temperature_2m", 10.0); val mm = c.optDouble("precipitation", 0.0)
            return when {
                code in listOf(56, 57, 66, 67) -> "Pluie verglaçante"
                t <= 1.0 && (mm > 0 || code in 51..86) -> "Risque de verglas"
                code in 71..77 || code == 85 || code == 86 -> "Neige"
                code == 45 || code == 48 -> "Brouillard"
                code in 95..99 -> "Orage"
                code == 65 || code == 82 || mm >= 4 -> "Fortes pluies"
                else -> null
            }
        }

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
