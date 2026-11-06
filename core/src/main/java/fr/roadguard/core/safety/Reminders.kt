package fr.roadguard.core.safety

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import fr.roadguard.core.DriveSettings

/** Échéances administratives de la voiture : prévenir un mois avant (une fois par semaine au plus). */
object Reminders {
    val items = listOf(
        "ct" to "Contrôle technique",
        "insurance" to "Assurance",
        "vignette_ch" to "Vignette autoroutière suisse",
        "service" to "Révision / vidange",
        "tyres" to "Pneus hiver / été",
        "license" to "Permis (visite médicale, fin de période probatoire…)",
    )

    /** Échéances à moins de 30 jours (ou dépassées) : (libellé, jours restants). */
    fun due(settings: DriveSettings, now: Long = System.currentTimeMillis()): List<Pair<String, Int>> = items.mapNotNull { (k, label) ->
        val at = settings.reminderDate(k)
        if (at == 0L) return@mapNotNull null
        val days = ((at - now) / 86_400_000L).toInt()
        if (days <= 30) label to days else null
    }

    fun check(context: Context) {
        val settings = DriveSettings(context)
        val prefs = context.getSharedPreferences("reminders", Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        if (now - prefs.getLong("last", 0) < 7 * 86_400_000L) return
        val list = due(settings, now)
        if (list.isEmpty()) return
        prefs.edit().putLong("last", now).apply()
        val text = list.joinToString(" · ") { (l, d) -> if (d < 0) "$l : dépassé de ${-d} j" else if (d == 0) "$l : aujourd'hui" else "$l : dans $d j" }
        runCatching {
            if (android.os.Build.VERSION.SDK_INT >= 33 &&
                context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return@runCatching
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel("pilote_reminders", "Échéances de la voiture", NotificationManager.IMPORTANCE_DEFAULT))
            val open = context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
                android.app.PendingIntent.getActivity(context, 61, it, android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT)
            }
            nm.notify(4501, Notification.Builder(context, "pilote_reminders").setSmallIcon(android.R.drawable.ic_menu_my_calendar)
                .setContentTitle("📅 Échéances de la voiture").setContentText(text).setStyle(Notification.BigTextStyle().bigText(text))
                .setAutoCancel(true).apply { open?.let { setContentIntent(it) } }.build())
        }
    }
}
