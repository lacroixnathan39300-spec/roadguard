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
    /** Trajets proposés au départ (le premier est le plus rapide) ; vide une fois le choix fait. */
    var alternatives by mutableStateOf<List<Route>>(emptyList()); private set
    var guidanceState by mutableStateOf<GuidanceState?>(null); private set
    var navStatus by mutableStateOf<String?>(null); private set
    var navigating by mutableStateOf(false); private set
    var recenter by mutableIntStateOf(0)

    /** Signalements et conducteurs de la communauté ROADGUARD. */
    val community = fr.roadguard.core.community.Community(context, scope) { voice.speak(it) }

    private val recentStore = RecentPlaces(context)
    var recents by mutableStateOf(recentStore.load()); private set
    var home by mutableStateOf(recentStore.saved("home")); private set
    var work by mutableStateOf(recentStore.saved("work")); private set
    var favorites by mutableStateOf(recentStore.favorites()); private set

    fun toggleFavorite(place: Place) { favorites = recentStore.toggleFavorite(place) }

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
    private var lastLogAt = 0L
    private var alertAt = 0L
    private var previous: Fix? = null
    private var computedKmh = 0.0
    private var lastMovedBearing = Double.NaN

    /**
     * Cap et vitesse déduits des deux dernières positions quand le GPS ne les fournit pas
     * (puces GPS d'autoradio, positions simulées) : sans cap, aucune alerte radar n'est possible.
     */
    private fun completeFix(f: Fix): Fix {
        val p = previous
        val dt = p?.let { (f.timeMs - it.timeMs) / 1000.0 } ?: 0.0
        val d = p?.let { fr.roadguard.core.radar.Geo.distanceMeters(it.lat, it.lon, f.lat, f.lon) } ?: 0.0
        if (p == null || dt > 10 || dt <= 0) { previous = f; computedKmh = 0.0; return f }
        if (d < 6) {
            // Trop près pour mesurer : on garde la référence ; vitesse calculée conservée un court instant.
            if (f.speedKmh > 0 || dt >= 3) return f
            return f.copy(speedKmh = computedKmh, bearing = if (computedKmh > 7) lastMovedBearing else f.bearing)
        }
        previous = f
        val speed = if (f.speedKmh > 0) f.speedKmh else {
            // Vitesse calculée lissée (positions irrégulières des puces d'autoradio).
            // Une voiture ne gagne ou ne perd pas plus de ~15 km/h par seconde : on écarte les sauts.
            val raw = (d / dt * 3.6).let { r -> if (computedKmh > 0) r.coerceIn(computedKmh - 15 * dt, computedKmh + 15 * dt) else r }
            computedKmh = if (computedKmh <= 0) raw else computedKmh * 0.5 + raw * 0.5
            computedKmh
        }
        val moved = if (speed > 7) fr.roadguard.core.radar.Geo.bearing(p.lat, p.lon, f.lat, f.lon) else Double.NaN
        if (!moved.isNaN()) lastMovedBearing = moved
        // Certaines puces renvoient un cap figé à 0 : le sens réel du déplacement l'emporte alors.
        val bearing = when {
            f.bearing.isNaN() -> moved
            f.bearing == 0.0 && !moved.isNaN() && fr.roadguard.core.radar.Geo.headingDelta(0.0, moved) > 30 -> moved
            else -> f.bearing
        }
        return f.copy(speedKmh = speed, bearing = bearing)
    }

    fun onLocation(location: Location) {
        // Pays de radars ajouté ou retiré : on recharge les bases en arrière-plan.
        if (radarVersion != RadarRepository.version.value) {
            radarVersion = RadarRepository.version.value
            if (alerts != null) scope.launch { prepare() }
        }
        voice.voiceEnabled = settings.voice
        voice.beepEnabled = settings.beep
        val f = completeFix(location.toAlertFix())
        // Vitesse calculée par ROADGUARD si le GPS ne la donne pas (certains postes).
        fix = location.toSpeedFix().let { sf -> if (!location.hasSpeed() && f.speedKmh > 0) sf.copy(kmh = f.speedKmh.toInt()) else sf }
        here = f

        val newAlert = alerts?.update(f)?.takeIf { it.radar.kind in settings.alertKinds }
        if (newAlert?.isNew == true) voice.announce(newAlert)
        // Une position isolée sans cap fiable ne doit pas faire clignoter le bandeau : on garde
        // l'alerte précédente 4 s au plus, tant que le radar est encore devant (distance qui baisse).
        val held = alert?.takeIf { newAlert == null && f.timeMs - alertAt < 4_000 }?.let { a ->
            val d = fr.roadguard.core.radar.Geo.distanceMeters(f.lat, f.lon, a.radar.lat, a.radar.lon).toInt()
            if (d < a.distanceM + 15) a.copy(distanceM = d, isNew = false) else null
        }
        if (newAlert != null) alertAt = f.timeMs
        alert = newAlert ?: held
        voice.proximityBeeps = settings.proximityBeeps
        voice.proximity(alert?.distanceM)
        community.onFix(f)
        val newAverage = averages?.update(f)
        if (newAverage?.finished == true && average?.finished != true) voice.announceAverageResult(newAverage)
        average = newAverage

        autoNight = DayNight.isNight(f.lat, f.lon, System.currentTimeMillis())
        // Journal de diagnostic (essais émulateur et poste) : une ligne toutes les 5 s au plus.
        if (f.timeMs - lastLogAt > 5_000) {
            lastLogAt = f.timeMs
            android.util.Log.i("RoadguardSession", "pos=%.5f,%.5f v=%.0f cap=%.0f prec=%.0f nuit=%b alerte=%s radars=%b".format(
                java.util.Locale.US, f.lat, f.lon, f.speedKmh, f.bearing, f.accuracyM, night,
                newAlert?.let { "${it.radar.id}@${it.distanceM.toInt()}m" } ?: "-", alerts != null))
        }
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
            // Choix du trajet proposé au départ seulement : retiré après 1 km parcouru.
            if (alternatives.isNotEmpty() && g.route.lengthM - state.remainingM > 1000) alternatives = emptyList()
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
        alternatives = emptyList()
        pendingDestination = null
    }

    /** Passe sur une autre des variantes proposées. */
    fun chooseRoute(r: Route) {
        if (!navigating || route === r) return
        route = r; guidance = Guidance(r); guidanceState = null
    }

    fun dismissAlternatives() { alternatives = emptyList() }

    fun saveSlot(slot: String, place: Place) {
        recentStore.save(slot, place)
        if (slot == "home") home = place else work = place
    }

    private fun computeRoute(destination: LatLon, announce: Boolean) {
        val from = here
        if (from == null) { navStatus = "En attente du GPS…"; return }
        rerouting = true
        scope.launch {
            val start = LatLon(from.lat, from.lon)
            val heading = from.bearing.takeUnless { it.isNaN() }
            runCatching {
                // En ligne d'abord (Valhalla, avec variantes au départ) ; sans réseau, graphe routier installé.
                runCatching {
                    NavApi.routes(start, heading, destination, if (announce) 2 else 0, settings.avoidTolls, settings.avoidHighways)
                }.getOrElse { online ->
                    val offline = fr.roadguard.core.nav.OfflineRouter.route(context, start, heading, destination)
                        ?: throw java.io.IOException(
                            if (fr.roadguard.core.nav.OfflineRouter.installed(context).isEmpty()) online.message ?: "pas de réseau"
                            else "pas de réseau et région non couverte par les itinéraires hors ligne"
                        )
                    listOf(offline)
                }
            }.onSuccess { all ->
                if (!navigating) return@onSuccess
                val r = all.first()
                alternatives = if (announce && all.size > 1) all else emptyList()
                route = r; guidance = Guidance(r); guidanceState = null; navStatus = null
                if (!announce) voice.speak("Nouvel itinéraire")
            }.onFailure {
                if (navigating) navStatus = "Trajet impossible : ${it.message ?: "pas de réseau"}"
            }
            rerouting = false
        }
    }
}
