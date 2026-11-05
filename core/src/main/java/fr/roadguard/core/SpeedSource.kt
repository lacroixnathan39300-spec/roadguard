package fr.roadguard.core

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import fr.roadguard.core.radar.Fix
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** Une mesure de vitesse issue du GPS du poste. */
data class SpeedFix(val kmh: Int, val accuracyMeters: Float, val timeMillis: Long)

/**
 * Positions lues directement sur le GPS de l'autoradio (LocationManager, sans Google Play Services,
 * absent sur beaucoup de postes). L'appelant vérifie la permission avant de collecter.
 */
class SpeedSource(private val context: Context) {

    @SuppressLint("MissingPermission")
    fun locations(): Flow<Location> = callbackFlow {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var lastGpsAt = 0L
        // GPS en priorité ; position réseau (Wi-Fi, antennes) seulement en attendant le GPS ou s'il ne capte
        // plus (intérieur, parking) : la carte et le compteur ne restent pas bloqués sur « GPS… ».
        val gps = object : LocationListener {
            override fun onLocationChanged(location: Location) { lastGpsAt = android.os.SystemClock.elapsedRealtime(); trySend(location) }
            @Deprecated("Requis pour API < 29")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
            override fun onProviderEnabled(provider: String) = Unit
            override fun onProviderDisabled(provider: String) = Unit
        }
        val network = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (android.os.SystemClock.elapsedRealtime() - lastGpsAt > 5_000) trySend(location)
            }
            @Deprecated("Requis pour API < 29")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
            override fun onProviderEnabled(provider: String) = Unit
            override fun onProviderDisabled(provider: String) = Unit
        }
        // Dernière position connue (moins de 10 min) : affichée tout de suite au démarrage.
        listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
            .filter { System.currentTimeMillis() - it.time < 10 * 60_000 }
            .maxByOrNull { it.time }?.let { trySend(it) }
        // Inscription même si le GPS est coupé : les positions arrivent dès qu'on le rallume.
        runCatching { manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, gps, Looper.getMainLooper()) }
        if (manager.allProviders.contains(LocationManager.NETWORK_PROVIDER)) {
            runCatching { manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 3000L, 0f, network, Looper.getMainLooper()) }
        }
        awaitClose { manager.removeUpdates(gps); manager.removeUpdates(network) }
    }
}

/** Calculs purs, testables sans appareil. */
object SpeedMath {
    /** En dessous de ce seuil, le bruit GPS à l'arrêt est affiché comme 0. */
    const val STANDSTILL_KMH = 3

    fun toFix(hasSpeed: Boolean, metersPerSecond: Float, accuracy: Float, time: Long): SpeedFix {
        val raw = if (hasSpeed && metersPerSecond.isFinite() && metersPerSecond > 0f) metersPerSecond * 3.6f else 0f
        // Seuil appliqué avant l'arrondi : 2,5 km/h de bruit ne doit pas s'afficher « 3 ».
        val kmh = if (raw < STANDSTILL_KMH) 0 else Math.round(raw)
        return SpeedFix(kmh, accuracy, time)
    }

    /** Angle de l'aiguille du compteur : arc de 240° de -210° à +30°, plein à [maxKmh]. */
    fun needleSweep(kmh: Int, maxKmh: Int = 200): Float =
        240f * (kmh.coerceIn(0, maxKmh).toFloat() / maxKmh)
}

fun Location.toSpeedFix(): SpeedFix = SpeedMath.toFix(hasSpeed(), speed, accuracy, time)

fun Location.toAlertFix(): Fix = Fix(
    lat = latitude,
    lon = longitude,
    speedKmh = if (hasSpeed()) speed * 3.6 else 0.0,
    bearing = if (hasBearing() && hasSpeed() && speed > 2f) bearing.toDouble() else Double.NaN,
    accuracyM = if (hasAccuracy()) accuracy.toDouble() else 99.0,
    timeMs = time,
)
