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

    /** Historique des trajets, carburant, voiture garée. */
    val tripLog = fr.roadguard.core.safety.TripLog(context)
    /** Dangers OpenStreetMap (passages à niveau, virages, écoles). */
    private val hazards = fr.roadguard.core.safety.HazardWatch(scope)
    private val meter = fr.roadguard.core.safety.TripMeter()
    /** Dernier trajet terminé (bilan affiché) ; null une fois vu. */
    var lastTrip by mutableStateOf<fr.roadguard.core.safety.TripRecord?>(null)
    /** Choc détecté : heure (compte à rebours de 30 s avant de prévenir le contact d'urgence), 0 sinon. */
    var crashAt by mutableStateOf(0L); private set
    /** Envoi du SMS d'urgence (fourni par l'appli téléphone) : (numéro, texte). */
    var sendEmergency: ((String, String) -> Boolean)? = null
    /** Limite lue sur la carte, avant ajustement (pluie, profil, vitesse perso). */
    private var rawLimit: Int? = null
    /** En Suisse (annonces de radars interdites) : radars coupés automatiquement. */
    val radarsBlockedHere: Boolean get() = settings.swissMode && localInfo.country == "CH"

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
        // De 2 h à 5 h du matin, la vigilance baisse : rappel après 1 h 30, puis toutes les 20 min.
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val night = hour in 2..4
        if (drivingSince == 0L) { drivingSince = now; nextPauseAt = now + if (night) 90 * 60_000L else 2 * 3_600_000L }
        if (now >= nextPauseAt) {
            val h = (now - drivingSince) / 3_600_000.0
            if (night) voice.speak("Il est tard et tu roules depuis un moment : la vigilance baisse. Fais une pause.")
            else voice.speak(if (h < 2.2) "Deux heures de route. Pense à faire une pause." else "Cela fait %.0f heures de route. Une pause s'impose.".format(java.util.Locale.FRANCE, h))
            nextPauseAt = now + if (night) 20 * 60_000L else 30 * 60_000L
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
    private fun onLimit(read: Int) {
        // 0 : tronçon sans limite connue (route de campagne non renseignée…) : on n'affiche plus l'ancienne limite.
        val v: Int? = read.takeIf { it > 0 }
        val old = rawLimit
        if (old == null && v == null) { limitCandidate = null; return }
        if (v == old) { limitCandidate = null; return }
        val key = v ?: 0
        if (old != null && limitCandidate != key) { limitCandidate = key; return }
        limitCandidate = null
        rawLimit = v
        val adj = adjusted(v)
        val changed = adj != speedLimit
        speedLimit = adj
        if (changed && adj != null && settings.announceLimit && alert == null) voice.limitChanged(adj)
    }

    /** Limite à respecter : pluie, jeune conducteur, vitesse maximale personnelle. */
    private fun adjusted(v: Int?): Int? = fr.roadguard.core.safety.RoadRules.adjust(v, localInfo.country,
        rain = settings.rainLimits && localInfo.raining, young = settings.driverProfile == 1, personalMax = settings.personalMax)

    // Annonces liées au pays, à la météo et aux dangers.
    private var lastCountry: String? = null
    private var lastHazard: String? = null
    private var lastHazardAt = 0L
    private var lastAheadHazard: String? = null
    private var lastRain = false
    private fun contextAnnouncements(f: Fix, now: Long) {
        // Pluie, profil, vitesse perso : la limite affichée suit.
        val adj = adjusted(rawLimit)
        if (rawLimit != null && adj != speedLimit) {
            speedLimit = adj
            if (settings.announceLimit && adj != null) voice.speak(if (localInfo.raining && settings.rainLimits) "Il pleut : limitation abaissée à $adj" else "Limitation $adj")
        }
        if (localInfo.raining != lastRain) { lastRain = localInfo.raining }
        // Suisse : annonces de radars interdites.
        val c = localInfo.country
        if (c != null && c != lastCountry) {
            if (lastCountry != null && settings.swissMode) {
                if (c == "CH") voice.speak("Tu entres en Suisse : les alertes radars sont coupées, comme l'exige la loi suisse.")
                else if (lastCountry == "CH") voice.speak("Tu quittes la Suisse : alertes radars rétablies.")
            }
            lastCountry = c
        }
        // Météo dangereuse ici, puis sur l'itinéraire.
        if (settings.weatherAlerts) {
            val h = localInfo.hazard
            if (h != null && (h != lastHazard || now - lastHazardAt > 30 * 60_000)) {
                lastHazard = h; lastHazardAt = now
                voice.speak(when (h) {
                    "Risque de verglas", "Pluie verglaçante" -> "Attention : $h. Roule doucement et garde tes distances."
                    "Brouillard" -> "Brouillard : allume tes feux et garde tes distances."
                    else -> "Attention : $h. Adapte ta vitesse."
                })
            } else if (h == null) lastHazard = null
            route?.points?.let { pts ->
                val ahead = pts.firstOrNull { fr.roadguard.core.radar.Geo.distanceMeters(f.lat, f.lon, it.lat, it.lon) > 20_000 }
                if (ahead != null) localInfo.checkAhead(ahead.lat, ahead.lon, now)
                val ha = localInfo.hazardAhead
                if (ha != null && ha != lastAheadHazard && ha != h) { lastAheadHazard = ha; voice.speak("Sur ta route, dans une vingtaine de kilomètres : $ha.") }
            }
        }
        if (settings.hazardAlerts) hazards.onFix(f, now)?.let { voice.speak(it) }
    }

    // Fin de trajet : arrêt d'au moins une minute après au moins 5 min de route (ou Bluetooth de la voiture coupé).
    private var slowSince = 0L
    private fun tripStep(f: Fix, now: Long) {
        meter.onFix(f.lat, f.lon, f.speedKmh, overSpeed, f.timeMs, localInfo.address)
        if (f.speedKmh >= 5) { slowSince = 0; return }
        if (slowSince == 0L) slowSince = now
        if (meter.active && now - slowSince > 60_000 && now - meter.start > 5 * 60_000) endTrip(f.lat, f.lon)
    }

    /** Termine le trajet en cours : bilan, voiture garée, rappels. */
    fun endTrip(lat: Double? = here?.lat, lon: Double? = here?.lon) {
        val rec = meter.finish(System.currentTimeMillis(), localInfo.address, settings.defaultTripPro) ?: return
        tripLog.addTrip(rec)
        lastTrip = rec
        if (lat != null && lon != null) tripLog.parking = fr.roadguard.core.safety.Parking(lat, lon, System.currentTimeMillis(), localInfo.address, 0)
        if (settings.forgetReminder) voice.speak("Fin du trajet. Pense à vérifier : rien oublié à l'arrière ? Enfant, animal, sac ?")
        val due = settings.serviceAtKm
        if (due > 0 && tripLog.odometerKm >= due) voice.speak("Rappel : l'entretien de la voiture est à prévoir.")
    }

    // ---------- Détection d'accident ----------
    private var lastFastAt = 0L
    private var impactAt = 0L
    /** Choc mesuré par l'accéléromètre (en g), fourni par l'appli téléphone. */
    /** Nombre de chocs mesurés (la dashcam garde alors la vidéo). */
    var impacts by mutableIntStateOf(0); private set
    private var lastImpactCount = 0L
    fun onImpact(g: Double) {
        val t = System.currentTimeMillis()
        if (g >= 3.0 && t - lastImpactCount > 3_000) { lastImpactCount = t; impacts++ }
        if (!settings.crashDetect || g < 4.0 || crashAt != 0L) return
        val now = System.currentTimeMillis()
        if (now - lastFastAt < 6_000) impactAt = now
    }
    fun cancelCrash() { crashAt = 0L; impactAt = 0L; voice.speak("D'accord. Bonne route, et prends soin de toi.") }
    private fun crashStep(kmh: Double, now: Long) {
        if (kmh >= 25) lastFastAt = now
        // Choc puis arrêt dans les 15 s : on demande si tout va bien.
        if (impactAt != 0L && crashAt == 0L) {
            if (kmh < 5) {
                crashAt = now; impactAt = 0L
                val who = settings.emergencyName.ifBlank { "ton contact d'urgence" }
                voice.wakeUp()
                voice.speak("Choc détecté. Tout va bien ? Touche « Je vais bien ». Sinon, je préviens $who dans trente secondes.")
            } else if (now - impactAt > 15_000) impactAt = 0L
        }
    }
    private fun crashTimeout(now: Long) {
        if (crashAt == 0L || now - crashAt < 30_000) return
        crashAt = 0L
        val p = here
        val phone = settings.emergencyPhone
        if (phone.isBlank()) { voice.speak("Aucun contact d'urgence enregistré. Appelle le 112 si tu as besoin d'aide."); return }
        val pos = p?.let { "https://maps.google.com/?q=%.5f,%.5f".format(java.util.Locale.US, it.lat, it.lon) } ?: "position inconnue"
        val text = "Pilote : un choc a été détecté et le conducteur ne répond pas. Position : $pos. " +
            "Appelle-le, et le 112 si besoin."
        val ok = sendEmergency?.invoke(phone, text) == true
        voice.speak(if (ok) "Message envoyé à ${settings.emergencyName.ifBlank { "ton contact" }}." else "Impossible d'envoyer le message. Appelle le 112 si tu as besoin d'aide.")
    }

    // ---------- Tunnels : position estimée quand le GPS ne répond plus ----------
    private var lastRealFixAt = 0L
    private var lastReal: Fix? = null
    private fun deadReckon(now: Long) {
        crashTimeout(now)
        val f0 = lastReal ?: return
        val lost = now - lastRealFixAt
        if (lost < 2_500 || lost > 120_000 || f0.speedKmh < 30 || f0.bearing.isNaN()) return
        val d = f0.speedKmh / 3.6 * lost / 1000.0
        val (lat, lon) = advance(f0.lat, f0.lon, f0.bearing, d)
        radarStep(f0.copy(lat = lat, lon = lon, timeMs = f0.timeMs + lost, accuracyM = 30.0))
    }
    /** Avance de [m] mètres : le long de l'itinéraire s'il y en a un, sinon tout droit. */
    private fun advance(lat: Double, lon: Double, bearing: Double, m: Double): Pair<Double, Double> {
        val pts = route?.points
        if (pts != null && pts.size > 1) {
            var i = pts.indices.minByOrNull { fr.roadguard.core.radar.Geo.distanceMeters(lat, lon, pts[it].lat, pts[it].lon) } ?: 0
            if (fr.roadguard.core.radar.Geo.distanceMeters(lat, lon, pts[i].lat, pts[i].lon) < 80) {
                var left = m; var cLat = lat; var cLon = lon
                while (i < pts.size - 1) {
                    val seg = fr.roadguard.core.radar.Geo.distanceMeters(cLat, cLon, pts[i + 1].lat, pts[i + 1].lon)
                    if (seg >= left) { val t = left / seg; return (cLat + (pts[i + 1].lat - cLat) * t) to (cLon + (pts[i + 1].lon - cLon) * t) }
                    left -= seg; cLat = pts[i + 1].lat; cLon = pts[i + 1].lon; i++
                }
                return cLat to cLon
            }
        }
        val r = Math.toRadians(bearing)
        val dLat = m * Math.cos(r) / 111_320.0
        val dLon = m * Math.sin(r) / (111_320.0 * Math.cos(Math.toRadians(lat)))
        return (lat + dLat) to (lon + dLon)
    }

    init {
        scope.launch { while (true) { kotlinx.coroutines.delay(1_000); runCatching { deadReckon(System.currentTimeMillis()) } } }
        community.startConvoyLoop()
    }

    /** À appeler une fois (bases radars chargées en arrière-plan). */
    suspend fun prepare() {
        val index = RadarRepository.index(context)
        // Radar fixe signalé là où un radar est déjà connu des bases : validé d'office.
        community.knownRadarNear = { lat, lon -> index.near(lat, lon).any { fr.roadguard.core.radar.Geo.distanceMeters(lat, lon, it.lat, it.lon) < 100 } }
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

    /** Radars : alerte, bips, alarme et réprimandes pour une position (réelle, ou estimée dans un tunnel). */
    private fun radarStep(f: Fix) {
        alerts?.route = route?.points // radars comptés seulement sur le tracé pendant un guidage
        val newAlert = alerts?.update(f)?.takeIf { settings.radarsOn && !radarsBlockedHere && it.radar.kind in settings.alertKinds }
        if (newAlert?.isNew == true) voice.announce(newAlert)
        // Une position isolée sans cap fiable ne doit pas faire clignoter le bandeau : on garde
        // l'alerte précédente 4 s au plus, tant que le radar est encore devant (distance qui baisse).
        val held = alert?.takeIf { settings.radarsOn && !radarsBlockedHere && newAlert == null && !it.passed && f.timeMs - alertAt < 4_000 }?.let { a ->
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
            lastPassedId = previous.radar.id; trip.onRadarPassed(); meter.radars++
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
        voice.pack.feminine = settings.driverFemale
        community.voiceAnswers = settings.voiceConfirm && settings.voice
        val f = completeFix(location.toAlertFix())
        // Vitesse calculée par ROADGUARD si le GPS ne la donne pas (certains postes).
        fix = location.toSpeedFix().let { sf -> if (!location.hasSpeed() && f.speedKmh > 0) sf.copy(kmh = f.speedKmh.toInt()) else sf }
        here = f

        lastReal = f; lastRealFixAt = System.currentTimeMillis()
        radarStep(f)
        community.onFix(f)
        trip.onFix(f.lat, f.lon, f.speedKmh, f.accuracyM, f.timeMs, if (location.hasAltitude()) location.altitude else null)
        localInfo.onFix(f.lat, f.lon, System.currentTimeMillis())
        val nowMs = System.currentTimeMillis()
        contextAnnouncements(f, nowMs)
        tripStep(f, nowMs)
        crashStep(f.speedKmh, nowMs)
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
                alert?.let { "${it.radar.id}@${it.distanceM.toInt()}m" } ?: "-", alerts != null))
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
            state.speak?.let(voice::guide) // consigne au ton choisi (« …, mon chéri »)
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
