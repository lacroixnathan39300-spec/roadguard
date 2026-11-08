package fr.roadguard.app.next

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import fr.roadguard.core.DriveSettings
import fr.roadguard.core.DrivingSession
import fr.roadguard.core.SpeedSource
import fr.roadguard.core.net.Net
import fr.roadguard.core.radar.AlertVoice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * ROADGUARD en arrière-plan : le cœur de conduite (alertes radars, vitesse moyenne, limite,
 * guidage vocal) continue écran éteint ou dans une autre appli. L'accueil affiche le même état.
 */
class DriveService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var voice: AlertVoice? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() { super.onCreate(); instance = this }

    /** Pilote balayé des applis récentes : même règle que la fermeture par le bouton retour. */
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        appClosed(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { stopSelf(); return START_NOT_STICKY }
        startForegroundNow()
        // Relancé par Android après un arrêt brutal (appli balayée sur certains téléphones, manque de mémoire) :
        // on ne reprend que si Pilote n'a pas été fermé et qu'un guidage ou un démarrage automatique était en cours.
        if (intent == null && session == null &&
            (AutoStart.paused(this) || !(AutoStart.wasNavigating(this) || AutoStart.wasAuto(this)))) {
            stopSelf(); return START_NOT_STICKY
        }
        if (intent?.getBooleanExtra(EXTRA_AUTO, false) == true && AutoStart.paused(this)) { stopSelf(); return START_NOT_STICKY }
        // Compte ROADGUARD sans accès (essai fini, bloqué ou déconnecté) : pas d'alertes en arrière-plan.
        if (!fr.roadguard.core.account.AccountManager.get(this).allowed) { stopSelf(); return START_NOT_STICKY }
        if (intent?.getBooleanExtra(EXTRA_AUTO, false) == true && session == null) autoStarted = true
        if (intent == null && AutoStart.wasAuto(this)) autoStarted = true
        if (session == null) start()
        return START_STICKY
    }

    private fun start() {
        Net.init(this)
        fr.roadguard.core.account.CrashReporter.install(this)
        val v = AlertVoice(this).also { voice = it }
        val s = DrivingSession(applicationContext, scope, v, DriveSettings(applicationContext))
        session = s
        s.sendEmergency = { phone, text -> sendSms(phone, text) }
        s.community.startConvoyLoop()
        startImpactSensor(s)
        scope.launch {
            s.prepare()
            SpeedSource(this@DriveService).locations().collect { s.onLocation(it) }
        }
        // Nouvelle version : notification Android, même quand l'appli est en arrière-plan (vérifié toutes les 6 h).
        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            kotlinx.coroutines.delay(60_000)
            while (true) { fr.roadguard.core.ui.UpdateNotifier.checkAndNotify(this@DriveService); kotlinx.coroutines.delay(6 * 3_600_000L) }
        }
        // Messages (réponse de l'administrateur, ou nouveau message pour l'administrateur) : toutes les 5 min.
        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            kotlinx.coroutines.delay(30_000)
            while (true) { runCatching { fr.roadguard.core.ui.InboxNotifier.check(this@DriveService) }; kotlinx.coroutines.delay(300_000) }
        }
        // Bulle par-dessus Waze, Google Maps… quand l'accueil ROADGUARD n'est pas affiché.
        val bubble = fr.roadguard.core.ui.RadarBubble(this)
        bubbleView = bubble
        scope.launch {
            var stillSince = System.currentTimeMillis()
            var stoppedSince = 0L
            while (true) {
                val now = System.currentTimeMillis()
                val kmh = s.fix?.kmh ?: 0
                // À l'arrêt (moins de 5 km/h pendant 8 s) : bulle cachée ; elle revient dès 10 km/h ou si une alerte arrive.
                if (kmh >= 10) stoppedSince = 0L else if (kmh < 5 && stoppedSince == 0L) stoppedSince = now
                val stopped = stoppedSince != 0L && now - stoppedSince > 8_000 && s.alert == null
                if (s.settings.bubble && !PhoneHomeActivity.visible && bubble.allowed && !stopped) bubble.show(s) else bubble.hide()
                // Widget de l'écran d'accueil, et vibration sur la montre à chaque nouveau radar.
                PiloteWidget.update(this@DriveService, s)
                val a = s.alert
                if (a != null && a.radar.id != lastWatchRadar) { lastWatchRadar = a.radar.id; if (s.settings.watchAlerts) watchAlert(a) }
                if (a == null) lastWatchRadar = null
                // Démarré tout seul en roulant : arrêt tout seul après 10 min à l'arrêt, appli fermée.
                if (kmh >= 5 || PhoneHomeActivity.visible) stillSince = now
                if (autoStarted && !s.navigating && now - stillSince > 10 * 60_000L) { bubble.hide(); stopSelf(); break }
                // Mémorisé pour une éventuelle relance par Android (voir onStartCommand).
                if (s.navigating != lastNav) { lastNav = s.navigating; AutoStart.remember(this@DriveService, autoStarted, s.navigating) }
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    private fun startForegroundNow() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Pilote en route", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(this, 0, Intent(this, PhoneHomeActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 1, Intent(this, DriveService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE)
        val n = Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("Pilote actif")
            .setContentText("Annonces et guidage, même écran éteint · ferme Pilote ou touche « Arrêter » pour couper")
            .setContentIntent(open)
            .addAction(Notification.Action.Builder(null, "Arrêter", stop).build())
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 29) startForeground(NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        else startForeground(NOTIFICATION_ID, n)
    }

    private var bubbleView: fr.roadguard.core.ui.RadarBubble? = null
    private var autoStarted = false
    private var lastNav: Boolean? = null
    private var lastWatchRadar: String? = null
    private var sensorListener: android.hardware.SensorEventListener? = null

    /** Accéléromètre : chocs violents (accident, dashcam). */
    private fun startImpactSensor(s: DrivingSession) {
        val sm = getSystemService(android.hardware.SensorManager::class.java) ?: return
        val acc = sm.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER) ?: return
        val l = object : android.hardware.SensorEventListener {
            override fun onSensorChanged(e: android.hardware.SensorEvent) {
                val g = Math.sqrt((e.values[0] * e.values[0] + e.values[1] * e.values[1] + e.values[2] * e.values[2]).toDouble()) / 9.81
                if (g >= 3.0) s.onImpact(g)
            }
            override fun onAccuracyChanged(sensor: android.hardware.Sensor?, accuracy: Int) {}
        }
        sensorListener = l
        sm.registerListener(l, acc, android.hardware.SensorManager.SENSOR_DELAY_GAME)
    }

    /** SMS au contact d'urgence (autorisation « SMS » donnée dans les réglages). */
    @android.annotation.SuppressLint("MissingPermission")
    private fun sendSms(phone: String, text: String): Boolean = runCatching {
        if (checkSelfPermission(android.Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) return false
        @Suppress("DEPRECATION")
        val sms = (if (Build.VERSION.SDK_INT >= 31) getSystemService(android.telephony.SmsManager::class.java) else android.telephony.SmsManager.getDefault()) ?: return false
        sms.sendMultipartTextMessage(phone, null, sms.divideMessage(text), null, null)
        true
    }.getOrDefault(false)

    /** Notification vibrante : relayée sur la montre connectée (Wear OS, Galaxy Watch, Mi Band…). */
    private fun watchAlert(a: fr.roadguard.core.radar.RadarAlert) = runCatching {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return@runCatching
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(WATCH_CHANNEL, "Radars (montre)", NotificationManager.IMPORTANCE_HIGH).apply {
            enableVibration(true); vibrationPattern = longArrayOf(0, 300, 150, 300); setSound(null, null)
        })
        val limit = a.radar.speedKmh?.let { " · limité à $it" } ?: ""
        nm.notify(WATCH_ID, Notification.Builder(this, WATCH_CHANNEL).setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("📷 ${fr.roadguard.core.radar.AlertText.kindLabel(a.radar.kind)}")
            .setContentText("À ${fr.roadguard.core.radar.AlertText.roundedDistance(a.distanceM)} m$limit").setTimeoutAfter(25_000).setAutoCancel(true)
            .setOnlyAlertOnce(false).build())
    }

    override fun onDestroy() {
        bubbleView?.hide()
        sensorListener?.let { getSystemService(android.hardware.SensorManager::class.java)?.unregisterListener(it) }
        session?.endTrip()
        PiloteWidget.update(this, null)
        session = null
        scope.cancel()
        voice?.release()
        if (instance === this) instance = null
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL = "roadguard_drive"
        private const val NOTIFICATION_ID = 4741
        private const val WATCH_CHANNEL = "pilote_watch"
        private const val WATCH_ID = 4742
        const val ACTION_STOP = "fr.roadguard.app.next.DRIVE_STOP"
        const val EXTRA_AUTO = "auto"

        @Volatile private var instance: DriveService? = null

        /**
         * Pilote fermé (bouton retour, ou balayé des applis récentes) : tout s'arrête (annonces, bulle, bips), sauf un
         * guidage en cours (jusqu'à l'arrivée) ou le réglage « Continuer quand je ferme Pilote ».
         */
        fun appClosed(context: Context) {
            if (DriveSettings(context).keepRunning || session?.navigating == true) return
            quit(context)
        }

        /** « Quitter Pilote » : tout s'arrête, guidage compris ; plus de démarrage automatique tant que Pilote n'est pas rouvert. */
        fun quit(context: Context) {
            AutoStart.pause(context)
            instance?.stopSelf()
        }

        /** Session en cours (lue par l'accueil), ou null si le service est arrêté. */
        @Volatile var session: DrivingSession? = null; private set

        fun start(context: Context) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
            context.startForegroundService(Intent(context, DriveService::class.java))
        }
    }
}
