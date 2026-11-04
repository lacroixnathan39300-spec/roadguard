package fr.roadguard.drive

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Ouverture automatique de DRIVE quand le poste démarre ou se réveille.
 * Les autoradios à puce Autochips (alps 8227L) ne redémarrent pas à chaque contact : ils sortent
 * d'une veille rapide (« quick boot »), d'où les annonces propres au fabricant en plus du démarrage normal.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("autostart", true)) return
        val start = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        // Quelques secondes de répit : le lanceur d'origine se met en place puis DRIVE passe devant.
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            runCatching { context.startActivity(start) }
        }, 4000)
    }
}
