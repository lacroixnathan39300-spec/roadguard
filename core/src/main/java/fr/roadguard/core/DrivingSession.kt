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

    /** Ordinateur de bord (distances, temps, vitesses, chronos). */
    val trip = TripComputer(context)
    /** Trace des trajets (export GPX). */
    val tracks = TrackRecorder(context)
    /** Météo et adresse actuelle. */
    val localInfo = LocalInfo(scope)

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
    val overSpeed: Boolean get() = SpeedLimitTracker.isOver(fix?.kmh ?: 0, speedLimit, settings.overTolerance)
    private var lastPassedId: String? = null

    // Rappel de pause : 2 h de conduite sans arrêt d'au moins 15 min, puis toutes les 30 min.
    private var drivingSince = 0L
    private var stoppedAt = 0L
    private var nextPauseAt = 0L
    private fun pauseReminder(kmh: Double, now: Long) {
        if (!settings.pauseReminder) return
        if (kmh < 5) { if (stoppedAt == 0L) stoppedAt = now; if (stoppedAt != 0L && now - stoppedAt > 15 * 60_000) { drivingSince = 0; nextPauseAt = 0 }; return }
        stoppedAt = 0
        if (drivingSince == 0L) { drivingSince = now; nextPauseAt = now + 2 * 3_600_000L }
        if (now >= nextPauseAt) {
            val h = (now - drivingSince) / 3_600_000.0
            voice.speak(if (h < 2.2) "Deux heures de route. Pense à faire une pause." else "Cela fait %.0f heures de route. Une pause s'impose.".format(java.util.Locale.FRANCE, h))
            nextPauseAt = now + 30 * 60_000L
        }
    }
    private var limitCandidate: Int? = null
    private var overSince = 0L
    private val overEpisodes = ArrayDeque<Long>()
    private var lastScold = 0L
    @Volatile private var limitBusy = false

    /**
     * Nouvelle limite lue (une lecture toutes les 3 s) : retenue quand deux lectures de suite concordent,
     * soit quelques secondes après l'entrée dans la zone, et sans fausse annonce au passage d'un carrefour.
     */
    private fun onLimit(v: Int) {
        val old = speedLimit
        if (old == null) { speedLimit = v; limitCandidate = null; return }
        if (v == old) { limitCandidate = null; return }
        if (limitCandidate != v) { limitCandidate = v; return }
        limitCandidate = null
        speedLimit = v
        if (settings.announceLimit && alert == null) voice.limitChanged(v)
    }

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
        // Saut impossible (position de départ fausse, GPS qui se recale) : on repart de cette position.
        if (d / dt * 3.6 > 300) { previous = f; computedKmh = 0.0; return f.copy(speedKmh = if (f.speedKmh in 0.0..300.0) f.speedKmh else 0.0) }
        if (d < 6) {
            // Trop près pour mesurer : on garde la référence ; vitesse calculée conservée un court instant.
            if (f.speedKmh > 0 || dt >= 3) return f.copy(bearing = when {
                lastMovedBearing.isNaN() -> f.bearing
                f.bearing.isNaN() || fr.roadguard.core.radar.Geo.headingDelta(f.bearing, lastMovedBearing) > 25 -> lastMovedBearing
                else -> f.bearing
            })
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
        // Le sens réel du déplacement (entre deux positions) fait foi : certains téléphones donnent un
        // « cap » issu de la boussole (téléphone en biais sur son support) ou figé à 0. Le cap du GPS
        // n'est gardé que s'il concorde avec le déplacement (il est alors plus lisse).
        val bearing = when {
            moved.isNaN() -> f.bearing
            f.bearing.isNaN() -> moved
            fr.roadguard.core.radar.Geo.headingDelta(f.bearing, moved) > 25 -> moved
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
        voice.duckMusic = settings.duckMusic
        voice.configure(settings.voiceName, settings.speechRate, settings.voiceTone)
        voice.realistic = false // même voix (celle du téléphone) pour le guidage et les radars
        val f = completeFix(location.toAlertFix())
        // Vitesse calculée par ROADGUARD si le GPS ne la donne pas (certains postes).
        fix = location.toSpeedFix().let { sf -> if (!location.hasSpeed() && f.speedKmh > 0) sf.copy(kmh = f.speedKmh.toInt()) else sf }
        here = f

        alerts?.route = route?.points // radars comptés seulement sur le tracé pendant un guidage
        val newAlert = alerts?.update(f)?.takeIf { settings.radarsOn && it.radar.kind in settings.alertKinds }
        if (newAlert?.isNew == true) voice.announce(newAlert)
        // Une position isolée sans cap fiable ne doit pas faire clignoter le bandeau : on garde
        // l'alerte précédente 4 s au plus, tant que le radar est encore devant (distance qui baisse).
        val held = alert?.takeIf { settings.radarsOn && newAlert == null && !it.passed && f.timeMs - alertAt < 4_000 }?.let { a ->
            val raw = fr.roadguard.core.radar.Geo.distanceMeters(f.lat, f.lon, a.radar.lat, a.radar.lon)
            val d = (raw - fr.roadguard.core.radar.AlertEngine.lagMeters(f.speedKmh)).toInt()
            // Jamais après le radar : dès qu'il est atteint (ou derrière), l'alerte s'arrête.
            val behind = !f.bearing.isNaN() && fr.roadguard.core.radar.Geo.headingDelta(f.bearing,
                fr.roadguard.core.radar.Geo.bearing(f.lat, f.lon, a.radar.lat, a.radar.lon)) > 90
            if (!behind && d > 0 && d < a.distanceM + 15) a.copy(distanceM = d, isNew = false) else null
        }
        if (newAlert != null) alertAt = f.timeMs
        // Radar franchi : compté une fois dans l'ordinateur de bord.
        val previous = alert
        alert = newAlert ?: held
        if (alert == null && previous != null && previous.distanceM < 80 && previous.radar.id != lastPassedId) {
            lastPassedId = previous.radar.id; trip.onRadarPassed()
        }
        // Excès de vitesse en approchant d'un radar : alarme forte qui réveille, jusqu'à repasser sous la limite.
        val cur = alert
        val limit = cur?.radar?.speedKmh
        val radarOver = settings.radarAlarm && cur != null && limit != null &&
            fr.roadguard.core.nav.SpeedLimitTracker.isOver(f.speedKmh.toInt(), limit, settings.overTolerance)
        // Option : la même alarme dès qu'on dépasse la limitation de la route, même sans radar devant.
        val roadLimit = speedLimit
        val roadOver = settings.alarmAnywhere && roadLimit != null &&
            fr.roadguard.core.nav.SpeedLimitTracker.isOver(f.speedKmh.toInt(), roadLimit, settings.overTolerance)
        voice.overspeedAlarm(radarOver || roadOver, if (radarOver) limit else roadLimit)
        // Réprimande : excès qui dure (plus de 15 s) ou qui se répète (3 fois en 10 min), au plus toutes les 45 s.
        val overNow = radarOver || (roadLimit != null && fr.roadguard.core.nav.SpeedLimitTracker.isOver(f.speedKmh.toInt(), roadLimit, settings.overTolerance))
        if (overNow) { if (overSince == 0L) { overSince = f.timeMs; overEpisodes.addLast(f.timeMs) } } else overSince = 0L
        while (overEpisodes.isNotEmpty() && f.timeMs - overEpisodes.first() > 600_000) overEpisodes.removeFirst()
        if (settings.scold && overNow && (f.timeMs - overSince > 15_000 || overEpisodes.size >= 3) && f.timeMs - lastScold > 45_000) {
            lastScold = f.timeMs; voice.scold()
        }
        voice.proximityBeeps = settings.proximityBeeps
        val a = alert
        // Radar dépassé : bips encore le temps de parcourir les derniers mètres (40 m après le radar).
        voice.proximity(a?.distanceM, if (a?.passed == true) (a.remainingM / (f.speedKmh / 3.6).coerceAtLeast(2.0) * 1000).toLong().coerceAtLeast(1) else 0)
        community.onFix(f)
        trip.onFix(f.lat, f.lon, f.speedKmh, f.accuracyM, f.timeMs, if (location.hasAltitude()) location.altitude else null)
        localInfo.onFix(f.lat, f.lon, System.currentTimeMillis())
        tracks.onFix(f.lat, f.lon, f.speedKmh, if (location.hasAltitude()) location.altitude else null, f.timeMs)
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
            if (!limitBusy) {
                limitBusy = true
                scope.launch { try { SpeedLimits.current(trail)?.let { onLimit(it) } } finally { limitBusy = false } }
            }
        }
        val kmh = fix?.kmh ?: 0
        if (settings.speedLimitWarning && SpeedLimitTracker.isOver(kmh, speedLimit, settings.overTolerance) && f.timeMs - lastOverWarning > 30_000) {
            lastOverWarning = f.timeMs
            voice.overLimit(speedLimit)
        }
        pauseReminder(f.speedKmh, f.timeMs)

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
