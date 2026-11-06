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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { stopSelf(); return START_NOT_STICKY }
        startForegroundNow()
        // Compte ROADGUARD sans accès (essai fini, bloqué ou déconnecté) : pas d'alertes en arrière-plan.
        if (!fr.roadguard.core.account.AccountManager.get(this).allowed) { stopSelf(); return START_NOT_STICKY }
        if (intent?.getBooleanExtra(EXTRA_AUTO, false) == true && session == null) autoStarted = true
        if (session == null) start()
        return START_STICKY
    }

    private fun start() {
        Net.init(this)
        fr.roadguard.core.account.CrashReporter.install(this)
        val v = AlertVoice(this).also { voice = it }
        val s = DrivingSession(applicationContext, scope, v, DriveSettings(applicationContext))
        session = s
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
                // Démarré tout seul en roulant : arrêt tout seul après 10 min à l'arrêt, appli fermée.
                if (kmh >= 5 || PhoneHomeActivity.visible) stillSince = now
                if (autoStarted && !s.navigating && now - stillSince > 10 * 60_000L) { bubble.hide(); stopSelf(); break }
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    private fun startForegroundNow() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "ROADGUARD en route", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(this, 0, Intent(this, PhoneHomeActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 1, Intent(this, DriveService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE)
        val n = Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("ROADGUARD actif")
            .setContentText("Alertes radars et guidage vocal, même écran éteint")
            .setContentIntent(open)
            .addAction(Notification.Action.Builder(null, "Arrêter", stop).build())
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 29) startForeground(NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        else startForeground(NOTIFICATION_ID, n)
    }

    private var bubbleView: fr.roadguard.core.ui.RadarBubble? = null
    private var autoStarted = false

    override fun onDestroy() {
        bubbleView?.hide()
        session = null
        scope.cancel()
        voice?.release()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL = "roadguard_drive"
        private const val NOTIFICATION_ID = 4741
        const val ACTION_STOP = "fr.roadguard.app.next.DRIVE_STOP"
        const val EXTRA_AUTO = "auto"

        /** Session en cours (lue par l'accueil), ou null si le service est arrêté. */
        @Volatile var session: DrivingSession? = null; private set

        fun start(context: Context) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
            context.startForegroundService(Intent(context, DriveService::class.java))
        }
    }
}
