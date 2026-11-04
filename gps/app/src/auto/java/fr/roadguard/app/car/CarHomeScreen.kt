package fr.roadguard.app.car

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarIcon
import androidx.car.app.model.DateTimeWithZone
import androidx.car.app.model.Distance
import androidx.car.app.model.Template
import androidx.car.app.navigation.NavigationManager
import androidx.car.app.navigation.NavigationManagerCallback
import androidx.car.app.navigation.model.Maneuver
import androidx.car.app.navigation.model.MessageInfo
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.car.app.navigation.model.RoutingInfo
import androidx.car.app.navigation.model.Step
import androidx.car.app.navigation.model.TravelEstimate
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import fr.roadguard.app.next.DriveService
import fr.roadguard.core.DrivingSession
import fr.roadguard.core.radar.AlertText
import fr.roadguard.core.ui.maneuverSymbol
import java.util.TimeZone

/** Écran principal Android Auto : carte, guidage, alertes radars, vitesse. */
class CarHomeScreen(carContext: CarContext) : Screen(carContext) {
    private val renderer = CarMapRenderer(carContext, lifecycleScope)
    private val handler = Handler(Looper.getMainLooper())
    private val nav = carContext.getCarService(NavigationManager::class.java)
    private var navStarted = false
    private var lastKey = ""

    private val tick = object : Runnable {
        override fun run() {
            val s = DriveService.session
            if (s == null && hasLocation()) DriveService.start(carContext)
            renderer.update(s)
            syncNavigation(s)
            // Le modèle n'est redemandé que si ce qui est affiché a changé.
            val key = templateKey(s)
            if (key != lastKey) { lastKey = key; invalidate() }
            handler.postDelayed(this, 1000)
        }
    }

    init {
        carContext.getCarService(AppManager::class.java).setSurfaceCallback(renderer)
        nav.setNavigationManagerCallback(object : NavigationManagerCallback {
            override fun onStopNavigation() { DriveService.session?.stopNavigation() }
        })
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) { handler.post(tick) }
            override fun onStop(owner: LifecycleOwner) { handler.removeCallbacks(tick) }
            override fun onDestroy(owner: LifecycleOwner) {
                if (navStarted) runCatching { nav.navigationEnded() }
                renderer.release()
            }
        })
    }

    private fun hasLocation() = ContextCompat.checkSelfPermission(carContext, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

    private fun syncNavigation(s: DrivingSession?) {
        val active = s?.route != null
        if (active && !navStarted) { runCatching { nav.navigationStarted(); navStarted = true } }
        if (!active && navStarted) { runCatching { nav.navigationEnded() }; navStarted = false }
    }

    private fun templateKey(s: DrivingSession?): String {
        val g = s?.guidanceState
        return listOf(s != null, s?.fix?.kmh, s?.speedLimit, s?.alert?.radar?.id, s?.alert?.distanceM?.div(50), s?.navStatus,
            s?.route?.hashCode(), g?.next?.instruction, g?.toNextM?.div(25), g?.remainingS?.div(30), g?.arrived).joinToString("|")
    }

    override fun onGetTemplate(): Template {
        val s = DriveService.session
        val actions = ActionStrip.Builder()
            .addAction(Action.Builder().setTitle(if (s?.route != null) "Arrêter" else "Où aller ?").setOnClickListener {
                if (s != null && (s.route != null || s.navigating)) s.stopNavigation()
                else screenManager.push(CarDestinationScreen(carContext))
            }.build())
            .build()
        val mapActions = ActionStrip.Builder()
            .addAction(Action.Builder().setIcon(textIcon("+")).setOnClickListener { renderer.zoomBy(1.0) }.build())
            .addAction(Action.Builder().setIcon(textIcon("−")).setOnClickListener { renderer.zoomBy(-1.0) }.build())
            .build()
        val b = NavigationTemplate.Builder().setActionStrip(actions).setMapActionStrip(mapActions)
        if (!hasLocation() || s == null) {
            b.setNavigationInfo(MessageInfo.Builder("ROADGUARD").setText(
                if (!hasLocation()) "Ouvre ROADGUARD sur le téléphone pour autoriser la position." else "Démarrage…").build())
            return b.build()
        }
        val g = s.guidanceState
        val next = g?.next
        when {
            s.navStatus != null -> b.setNavigationInfo(MessageInfo.Builder(s.navStatus ?: "").build())
            s.route != null && g != null && next != null && !g.arrived -> {
                val step = Step.Builder(next.instruction.ifBlank { "Continuez" })
                    .setManeuver(maneuver(next))
                    .apply { if (next.street.isNotBlank()) setRoad(next.street) }
                    .build()
                b.setNavigationInfo(RoutingInfo.Builder().setCurrentStep(step, distance(g.toNextM)).build())
                val arrival = System.currentTimeMillis() + g.remainingS * 1000L
                b.setDestinationTravelEstimate(TravelEstimate.Builder(distance(g.remainingM),
                    DateTimeWithZone.create(arrival, TimeZone.getDefault())).setRemainingTimeSeconds(g.remainingS.toLong()).build())
            }
            g?.arrived == true -> b.setNavigationInfo(MessageInfo.Builder("Vous êtes arrivé").build())
            else -> {
                val alert = s.alert
                val title = alert?.let { "${AlertText.kindLabel(it.radar.kind)}${it.radar.speedKmh?.let { v -> " $v" } ?: ""} · ${it.distanceM} m" }
                    ?: "${s.fix?.kmh ?: 0} km/h"
                val text = listOfNotNull(
                    s.speedLimit?.let { "Limite $it km/h" + if (s.overSpeed) " · ATTENTION" else "" },
                    s.average?.let { "Vitesse moyenne en cours" },
                ).joinToString(" · ").ifBlank { "Alertes radars actives" }
                b.setNavigationInfo(MessageInfo.Builder(title).setText(text).build())
            }
        }
        return b.build()
    }

    private fun distance(m: Int): Distance =
        if (m >= 1000) Distance.create(m / 1000.0, Distance.UNIT_KILOMETERS) else Distance.create((m / 10 * 10).toDouble(), Distance.UNIT_METERS)

    /** Types de manœuvre ROADGUARD (ceux de Valhalla) → Android Auto, avec une flèche dessinée. */
    private fun maneuver(m: fr.roadguard.core.nav.Maneuver): Maneuver {
        val exit = m.roundaboutExit
        val type = when (m.type) {
            4, 5, 6 -> Maneuver.TYPE_DESTINATION
            9, 20, 23 -> Maneuver.TYPE_TURN_SLIGHT_RIGHT
            10, 18, 37 -> Maneuver.TYPE_TURN_NORMAL_RIGHT
            11 -> Maneuver.TYPE_TURN_SHARP_RIGHT
            12 -> Maneuver.TYPE_U_TURN_RIGHT
            13 -> Maneuver.TYPE_U_TURN_LEFT
            14 -> Maneuver.TYPE_TURN_SHARP_LEFT
            15, 19, 38 -> Maneuver.TYPE_TURN_NORMAL_LEFT
            16, 21, 24 -> Maneuver.TYPE_TURN_SLIGHT_LEFT
            26, 27 -> if (exit != null) Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW else Maneuver.TYPE_ROUNDABOUT_ENTER_CCW
            else -> Maneuver.TYPE_STRAIGHT
        }
        val b = Maneuver.Builder(type).setIcon(textIcon(maneuverSymbol(m)))
        if (type == Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW && exit != null) b.setRoundaboutExitNumber(exit)
        return b.build()
    }

    /** Petite icône blanche dessinée à partir d'un symbole (flèche, +, −). */
    private fun textIcon(symbol: String): CarIcon {
        val size = 128
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE; textAlign = Paint.Align.CENTER
            textSize = if (symbol.length > 1) 64f else 96f; isFakeBoldText = true
        }
        Canvas(bmp).drawText(symbol, size / 2f, size / 2f - (p.descent() + p.ascent()) / 2, p)
        return CarIcon.Builder(IconCompat.createWithBitmap(bmp)).build()
    }

}
