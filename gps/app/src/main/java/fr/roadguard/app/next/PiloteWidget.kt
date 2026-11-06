package fr.roadguard.app.next

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import fr.roadguard.app.R
import fr.roadguard.core.DrivingSession

/** Widget de l'écran d'accueil : vitesse, limitation, radar ; un appui ouvre Pilote. */
class PiloteWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = update(context, DriveService.session)

    companion object {
        private var lastText = ""

        fun update(context: Context, s: DrivingSession?) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, PiloteWidget::class.java))
            if (ids.isEmpty()) return
            val speed = s?.fix?.kmh?.toString() ?: "--"
            val limit = s?.speedLimit
            val info = when {
                s == null -> "Pilote · touche pour démarrer"
                s.alert != null -> "📷 Radar à ${s.alert!!.distanceM} m"
                s.navigating -> s.guidanceState?.next?.instruction ?: "Guidage en cours"
                else -> s.localInfo.address ?: "Bonne route"
            }
            val key = "$speed|$limit|$info|${s?.overSpeed}"
            if (key == lastText) return
            lastText = key
            val v = RemoteViews(context.packageName, R.layout.pilote_widget)
            v.setTextViewText(R.id.widget_speed, speed)
            v.setTextColor(R.id.widget_speed, if (s?.overSpeed == true) 0xFFFF5252.toInt() else 0xFFFFFFFF.toInt())
            v.setTextViewText(R.id.widget_limit, limit?.toString() ?: "")
            v.setViewVisibility(R.id.widget_limit, if (limit != null) View.VISIBLE else View.GONE)
            v.setTextViewText(R.id.widget_info, info)
            val open = Intent(context, PhoneHomeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            v.setOnClickPendingIntent(R.id.widget_root, PendingIntent.getActivity(context, 9, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            runCatching { manager.updateAppWidget(ids, v) }
        }
    }
}
