package fr.roadguard.core

import android.content.Context
import android.location.Location
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fr.roadguard.core.nav.Guidance
import fr.roadguard.core.nav.GuidanceState
import fr.roadguard.core.nav.LatLon
import fr.roadguard.core.nav.NavApi
import fr.roadguard.core.nav.Place
import fr.roadguard.core.nav.RecentPlaces
import fr.roadguard.core.nav.Route
import fr.roadguard.core.nav.SpeedLimitTracker
import fr.roadguard.core.nav.SpeedLimits
import fr.roadguard.core.radar.AlertEngine
import fr.roadguard.core.radar.AlertVoice
import fr.roadguard.core.radar.AverageSpeedTracker
import fr.roadguard.core.radar.AverageStatus
import fr.roadguard.core.radar.Fix
import fr.roadguard.core.radar.RadarAlert
import fr.roadguard.core.radar.RadarRepository
import fr.roadguard.core.ui.DayNight
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Cœur de conduite ROADGUARD, identique sur le téléphone et sur l'autoradio :
 * à chaque position GPS, compteur, alertes radars, vitesse moyenne, limite de vitesse,
 * thème jour/nuit et guidage (avec voix et recalcul). L'interface ne fait qu'afficher cet état.
 */
class DrivingSession(
    private val context: Context,
    private val scope: CoroutineScope,
    val voice: AlertVoice,
    val settings: DriveSettings,
) {
    var fix by mutableStateOf<SpeedFix?>(null); private set
    var alert by mutableStateOf<RadarAlert?>(null); private set
    var average by mutableStateOf<AverageStatus?>(null); private set
    var here by mutableStateOf<Fix?>(null); private set
    var speedLimit by mutableStateOf<Int?>(null); private set
    var autoNight by mutableStateOf(false); private set

    var route by mutableStateOf<Route?>(null); private set
    var guidanceState by mutableStateOf<GuidanceState?>(null); private set
    var navStatus by mutableStateOf<String?>(null); private set
    var navigating by mutableStateOf(false); private set
    var recenter by mutableIntStateOf(0)

    private val recentStore = RecentPlaces(context)
    var recents by mutableStateOf(recentStore.load()); private set
    var home by mutableStateOf(recentStore.saved("home")); private set
    var work by mutableStateOf(recentStore.saved("work")); private set

    private var guidance: Guidance? = null
    private var rerouting = false
    private var lastRerouteAt = 0L
    private var pendingDestination: LatLon? = null
    private val limitTracker = SpeedLimitTracker()
    private var lastOverWarning = 0L
    private var alerts: AlertEngine? = null
    private var averages: AverageSpeedTracker? = null

    val night: Boolean get() = settings.isNight(autoNight)
    val overSpeed: Boolean get() = SpeedLimitTracker.isOver(fix?.kmh ?: 0, speedLimit)

    /** À appeler une fois (bases radars chargées en arrière-plan). */
    suspend fun prepare() {
        val index = RadarRepository.index(context)
        alerts = AlertEngine(index)
        averages = AverageSpeedTracker(index)
    }

    private var radarVersion = -1

    fun onLocation(location: Location) {
        // Pays de radars ajouté ou retiré : on recharge les bases en arrière-plan.
        if (radarVersion != RadarRepository.version.value) {
            radarVersion = RadarRepository.version.value
            if (alerts != null) scope.launch { prepare() }
        }
        voice.voiceEnabled = settings.voice
        voice.beepEnabled = settings.beep
        fix = location.toSpeedFix()
        val f = location.toAlertFix()
        here = f

        val newAlert = alerts?.update(f)?.takeIf { it.radar.kind in settings.alertKinds }
        if (newAlert?.isNew == true) voice.announce(newAlert)
        alert = newAlert
        val newAverage = averages?.update(f)
        if (newAverage?.finished == true && average?.finished != true) voice.announceAverageResult(newAverage)
        average = newAverage

        autoNight = DayNight.isNight(f.lat, f.lon, System.currentTimeMillis())
        limitTracker.add(f.lat, f.lon, f.timeMs, f.speedKmh)?.let { trail ->
            scope.launch { SpeedLimits.current(trail)?.let { speedLimit = it } }
        }
        val kmh = fix?.kmh ?: 0
        if (settings.speedLimitWarning && SpeedLimitTracker.isOver(kmh, speedLimit) && f.timeMs - lastOverWarning > 30_000) {
            lastOverWarning = f.timeMs
            voice.speak("Attention, limité à $speedLimit")
        }

        if (navigating && route == null && !rerouting && navStatus?.startsWith("En attente du GPS") == true) {
            pendingDestination?.let { computeRoute(it, announce = true) }
        }
        guidance?.let { g ->
            val state = g.update(f.lat, f.lon, f.speedKmh)
            guidanceState = state
            state.speak?.let(voice::speak)
            // Sortie du trajet : nouveau calcul, au plus toutes les 15 s.
            if (state.offRoute && !rerouting && f.timeMs - lastRerouteAt > 15_000) {
                lastRerouteAt = f.timeMs
                computeRoute(g.route.destination, announce = false)
            }
        }
    }

    fun startNavigation(place: Place) {
        recents = recentStore.add(place)
        navigating = true; route = null; guidance = null; guidanceState = null
        pendingDestination = place.point
        navStatus = "Calcul du trajet vers ${place.name}…"
        computeRoute(place.point, announce = true)
    }

    fun stopNavigation() {
        navigating = false; route = null; guidance = null; guidanceState = null; navStatus = null
        pendingDestination = null
    }

    fun saveSlot(slot: String, place: Place) {
        recentStore.save(slot, place)
        if (slot == "home") home = place else work = place
    }

    private fun computeRoute(destination: LatLon, announce: Boolean) {
        val from = here
        if (from == null) { navStatus = "En attente du GPS…"; return }
        rerouting = true
        scope.launch {
            runCatching {
                NavApi.route(LatLon(from.lat, from.lon), from.bearing.takeUnless { it.isNaN() }, destination)
            }.onSuccess { r ->
                if (!navigating) return@onSuccess
                route = r; guidance = Guidance(r); guidanceState = null; navStatus = null
                if (!announce) voice.speak("Nouvel itinéraire")
            }.onFailure {
                if (navigating) navStatus = "Trajet impossible : ${it.message ?: "pas de réseau"}"
            }
            rerouting = false
        }
    }
}
