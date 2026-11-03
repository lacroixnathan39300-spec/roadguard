package fr.roadguard.drive

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** Une mesure de vitesse issue du GPS du poste. */
data class SpeedFix(val kmh: Int, val accuracyMeters: Float, val timeMillis: Long)

/**
 * Vitesse lue directement sur le GPS de l'autoradio (LocationManager, sans Google Play Services,
 * absent sur beaucoup de postes chinois). L'appelant vérifie la permission avant de collecter.
 */
class SpeedSource(private val context: Context) {

    @SuppressLint("MissingPermission")
    fun fixes(): Flow<SpeedFix> = callbackFlow {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                trySend(SpeedMath.toFix(location.hasSpeed(), location.speed, location.accuracy, location.time))
            }
            @Deprecated("Requis pour API < 29")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
            override fun onProviderEnabled(provider: String) = Unit
            override fun onProviderDisabled(provider: String) = Unit
        }
        if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500L, 0f, listener, Looper.getMainLooper())
        }
        awaitClose { manager.removeUpdates(listener) }
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
