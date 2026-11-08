package fr.roadguard.app.next

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import fr.roadguard.core.DriveSettings

/**
 * Démarrage automatique de Pilote en roulant (façon Radarbot / RadarDroid) : sans ouvrir l'appli,
 * dès 20 km/h, le service de conduite démarre (alertes, bips) et la bulle s'affiche par-dessus Waze.
 *
 * Sources de position, très économes :
 *  - « passive » : les positions que d'autres applis (Waze, Google Maps…) demandent déjà, avec la vitesse ;
 *  - réseau toutes les 2 min : la vitesse se déduit du chemin parcouru entre deux positions.
 * Nécessite la position « Toujours autoriser » (sinon Android ne transmet rien en arrière-plan).
 */
object AutoStart {
    const val TRIGGER_KMH = 20.0
    private const val ACTION = "fr.roadguard.app.next.AUTO_LOCATION"

    fun hasBackgroundLocation(context: Context) = Build.VERSION.SDK_INT < 29 ||
        context.checkSelfPermission(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun hasLocation(context: Context) =
        context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun pending(context: Context): android.app.PendingIntent {
        val i = Intent(context, AutoStartReceiver::class.java).setAction(ACTION)
        // Modifiable : Android y ajoute la position reçue.
        val flags = android.app.PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 31) android.app.PendingIntent.FLAG_MUTABLE else 0)
        return android.app.PendingIntent.getBroadcast(context, 31, i, flags)
    }

    /** Active ou coupe l'écoute selon le réglage et les autorisations. À appeler à l'ouverture et au démarrage du téléphone. */
    @SuppressLint("MissingPermission")
    fun sync(context: Context) {
        val app = context.applicationContext
        val lm = app.getSystemService(LocationManager::class.java) ?: return
        val pi = pending(app)
        runCatching { lm.removeUpdates(pi) }
        if (!DriveSettings(app).autoDrive || !hasLocation(app) || !hasBackgroundLocation(app)) return
        runCatching { lm.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 3_000L, 0f, pi) }
        if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            runCatching { lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 120_000L, 150f, pi) }
        }
    }

    private fun state(context: Context) = context.applicationContext.getSharedPreferences("autostart", Context.MODE_PRIVATE)

    /**
     * Pilote fermé volontairement : plus rien ne le relance tout seul (vitesse, Bluetooth de la voiture, redémarrage
     * du service par Android) tant qu'il n'est pas rouvert.
     */
    fun pause(context: Context) { state(context).edit().putBoolean("closed", true).commit() }
    fun resume(context: Context) { state(context).edit().putBoolean("closed", false).apply() }
    fun paused(context: Context) = state(context).getBoolean("closed", false)

    /** Dernier état du service, pour savoir quoi faire s'il est relancé par Android après un arrêt brutal. */
    fun remember(context: Context, auto: Boolean, navigating: Boolean) {
        state(context).edit().putBoolean("was_auto", auto).putBoolean("was_navigating", navigating).apply()
    }
    fun wasAuto(context: Context) = state(context).getBoolean("was_auto", false)
    fun wasNavigating(context: Context) = state(context).getBoolean("was_navigating", false)

    /** Vitesse estimée en km/h : celle du GPS si fournie, sinon déduite de la position précédente. */
    fun estimateKmh(context: Context, loc: Location): Double? {
        val prefs = context.getSharedPreferences("autostart", Context.MODE_PRIVATE)
        val lastLat = prefs.getFloat("lat", Float.NaN); val lastLon = prefs.getFloat("lon", Float.NaN); val lastT = prefs.getLong("t", 0)
        prefs.edit().putFloat("lat", loc.latitude.toFloat()).putFloat("lon", loc.longitude.toFloat()).putLong("t", loc.time).apply()
        if (loc.hasSpeed() && loc.speed > 0) return loc.speed * 3.6
        if (lastLat.isNaN() || lastT == 0L) return null
        val dt = (loc.time - lastT) / 1000.0
        if (dt < 20 || dt > 600) return null
        val d = fr.roadguard.core.radar.Geo.distanceMeters(lastLat.toDouble(), lastLon.toDouble(), loc.latitude, loc.longitude)
        // Imprécision du réseau : on exige un vrai déplacement (plus de 400 m).
        return if (d > 400) d / dt * 3.6 else 0.0
    }

    /**
     * Démarrage depuis l'arrière-plan : Android l'accepte pour une appli autorisée à s'afficher
     * par-dessus les autres, à condition qu'une fenêtre à elle soit visible ; on pose donc d'abord
     * un point invisible de 1 pixel, retiré quelques secondes plus tard.
     */
    fun launch(context: Context) {
        val app = context.applicationContext
        if (DriveService.session != null) return
        if (paused(app)) return // fermé volontairement : pas de relance tant que Pilote n'est pas rouvert
        if (!fr.roadguard.core.account.AccountManager.get(app).allowed) return
        var anchor: View? = null
        val wm = app.getSystemService(WindowManager::class.java)
        if (android.provider.Settings.canDrawOverlays(app)) runCatching {
            val v = View(app)
            wm.addView(v, WindowManager.LayoutParams(1, 1, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, PixelFormat.TRANSLUCENT))
            anchor = v
        }
        runCatching {
            app.startForegroundService(Intent(app, DriveService::class.java).putExtra(DriveService.EXTRA_AUTO, true))
        }.onFailure { android.util.Log.w("PiloteAutoStart", "démarrage refusé : ${it.message}") }
        anchor?.let { v -> Handler(Looper.getMainLooper()).postDelayed({ runCatching { wm.removeView(v) } }, 5_000) }
    }
}

/** Reçoit les positions en arrière-plan, et le redémarrage du téléphone / la mise à jour de l'appli. */
class AutoStartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> AutoStart.sync(context)
            else -> {
                @Suppress("DEPRECATION")
                val loc = intent.getParcelableExtra<Location>(LocationManager.KEY_LOCATION_CHANGED) ?: return
                if (!DriveSettings(context).autoDrive || DriveService.session != null || AutoStart.paused(context)) return
                val kmh = AutoStart.estimateKmh(context, loc) ?: return
                if (kmh >= AutoStart.TRIGGER_KMH) AutoStart.launch(context)
            }
        }
    }
}
