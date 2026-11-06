package fr.roadguard.core.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import fr.roadguard.core.DrivingSession
import fr.roadguard.core.radar.AlertText

/**
 * Bulle ROADGUARD par-dessus les autres applis (Waze, Google Maps…) : vitesse, limite et radar devant.
 * Déplaçable au doigt ; sa position est mémorisée. Demande l'autorisation « Afficher par-dessus les autres applis ».
 */
class RadarBubble(context: Context) {
    private val app = context.applicationContext
    private val wm = app.getSystemService(WindowManager::class.java)
    private val prefs = app.getSharedPreferences("bubble", Context.MODE_PRIVATE)
    private var root: LinearLayout? = null
    private lateinit var speed: TextView
    private lateinit var limit: TextView
    private lateinit var alert: TextView
    private lateinit var info: TextView
    private lateinit var bg: GradientDrawable
    private lateinit var picker: LinearLayout
    private var session: fr.roadguard.core.DrivingSession? = null
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val hidePicker = Runnable { if (::picker.isInitialized) picker.visibility = android.view.View.GONE }
    private val density = app.resources.displayMetrics.density

    val allowed: Boolean get() = Settings.canDrawOverlays(app)

    fun requestPermission() {
        runCatching {
            app.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${app.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    private fun dp(v: Int) = (v * density).toInt()

    private fun pill(color: Int, radius: Int) = GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }

    @SuppressLint("ClickableViewAccessibility")
    private fun create(): LinearLayout {
        val box = LinearLayout(app).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            bg = pill(Color.argb(235, 18, 24, 32), 22); background = bg
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        val row = LinearLayout(app).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        speed = TextView(app).apply { setTextColor(Color.WHITE); textSize = 30f; paint.isFakeBoldText = true }
        limit = TextView(app).apply {
            setTextColor(Color.BLACK); textSize = 16f; paint.isFakeBoldText = true; gravity = Gravity.CENTER
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.WHITE); setStroke(dp(4), Color.rgb(211, 47, 47)) }
            layoutParams = LinearLayout.LayoutParams(dp(40), dp(40)).apply { leftMargin = dp(8) }
        }
        row.addView(speed); row.addView(limit)
        alert = TextView(app).apply {
            setTextColor(Color.WHITE); textSize = 14f; paint.isFakeBoldText = true
            background = pill(Color.rgb(198, 40, 40), 10); setPadding(dp(8), dp(3), dp(8), dp(3))
        }
        // Informations de la communauté (police, accident, bouchon…) annoncées en route.
        info = TextView(app).apply {
            setTextColor(Color.WHITE); textSize = 13f; maxWidth = dp(220); maxLines = 2
            background = pill(Color.rgb(21, 101, 192), 10); setPadding(dp(8), dp(3), dp(8), dp(3))
        }
        // Signaler sans quitter Waze : « ⚠ Signaler » ouvre une rangée de choix, un appui envoie.
        val reportBtn = TextView(app).apply {
            text = "⚠ Signaler"; setTextColor(Color.WHITE); textSize = 14f; paint.isFakeBoldText = true; gravity = Gravity.CENTER
            background = pill(Color.rgb(230, 126, 34), 12); setPadding(dp(12), dp(5), dp(12), dp(5))
            setOnClickListener {
                picker.visibility = if (picker.visibility == android.view.View.VISIBLE) android.view.View.GONE else android.view.View.VISIBLE
                handler.removeCallbacks(hidePicker); handler.postDelayed(hidePicker, 8_000)
            }
        }
        picker = LinearLayout(app).apply { orientation = LinearLayout.HORIZONTAL; visibility = android.view.View.GONE }
        listOf(fr.roadguard.core.community.ReportType.POLICE, fr.roadguard.core.community.ReportType.MOBILE,
            fr.roadguard.core.community.ReportType.ACCIDENT, fr.roadguard.core.community.ReportType.JAM,
            fr.roadguard.core.community.ReportType.WORKS, fr.roadguard.core.community.ReportType.DANGER).forEach { t ->
            picker.addView(TextView(app).apply {
                text = t.emoji; textSize = 24f; gravity = Gravity.CENTER
                background = pill(Color.argb(255, 40, 48, 60), 12); setPadding(dp(6), dp(4), dp(6), dp(4))
                contentDescription = t.label
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(dp(2), 0, dp(2), 0) }
                setOnClickListener {
                    session?.community?.report(t)
                    picker.visibility = android.view.View.GONE
                    android.widget.Toast.makeText(app, "${t.emoji} ${t.label} : signalement envoyé", android.widget.Toast.LENGTH_SHORT).show()
                }
            })
        }
        box.addView(row); box.addView(alert); box.addView(info); box.addView(reportBtn); box.addView(picker)
        (info.layoutParams as LinearLayout.LayoutParams).topMargin = dp(4)
        (reportBtn.layoutParams as LinearLayout.LayoutParams).topMargin = dp(6)
        (picker.layoutParams as LinearLayout.LayoutParams).topMargin = dp(6)

        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.TOP or Gravity.START; x = prefs.getInt("x", dp(16)); y = prefs.getInt("y", dp(120)) }

        // Déplacement au doigt (la position est retenue pour la prochaine fois).
        var startX = 0; var startY = 0; var touchX = 0f; var touchY = 0f
        box.setOnTouchListener { v, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { startX = lp.x; startY = lp.y; touchX = e.rawX; touchY = e.rawY; true }
                MotionEvent.ACTION_MOVE -> {
                    lp.x = startX + (e.rawX - touchX).toInt(); lp.y = startY + (e.rawY - touchY).toInt()
                    runCatching { wm.updateViewLayout(v, lp) }; true
                }
                MotionEvent.ACTION_UP -> {
                    prefs.edit().putInt("x", lp.x).putInt("y", lp.y).apply()
                    // Simple appui (sans glisser) : ouvre Pilote.
                    if (Math.abs(e.rawX - touchX) < dp(10) && Math.abs(e.rawY - touchY) < dp(10)) runCatching {
                        app.packageManager.getLaunchIntentForPackage(app.packageName)?.let {
                            app.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
                        }
                    }
                    true
                }
                else -> false
            }
        }
        wm.addView(box, lp)
        return box
    }

    /** Affiche ou met à jour la bulle avec l'état de conduite. */
    fun show(s: DrivingSession) {
        session = s
        if (!allowed) return
        val r = root ?: runCatching { create() }.getOrNull()?.also { root = it } ?: return
        val kmh = s.fix?.kmh
        speed.text = kmh?.toString() ?: "--"
        speed.setTextColor(if (s.overSpeed) Color.rgb(255, 82, 82) else Color.WHITE)
        val lim = s.speedLimit
        limit.visibility = if (lim != null) android.view.View.VISIBLE else android.view.View.GONE
        limit.text = lim?.toString().orEmpty()
        val a = s.alert
        alert.visibility = if (a != null) android.view.View.VISIBLE else android.view.View.GONE
        if (a != null) alert.text = "${AlertText.kindLabel(a.radar.kind)} · ${AlertText.roundedDistance(a.distanceM)} m"
        // Fond rouge pendant une alerte radar ou un excès de vitesse, sombre sinon.
        bg.setColor(if (a != null || s.overSpeed) Color.argb(240, 150, 20, 28) else Color.argb(235, 18, 24, 32))
        val t = s.community.toast
        info.visibility = if (t != null) android.view.View.VISIBLE else android.view.View.GONE
        info.text = t.orEmpty()
        r.alpha = 1f
    }

    fun hide() {
        root?.let { runCatching { wm.removeView(it) } }
        root = null
    }
}
