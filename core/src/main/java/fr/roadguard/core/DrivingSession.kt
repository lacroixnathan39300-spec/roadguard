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
import kotlinx.coroutines.withContext

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

    /** Économie de batterie (batterie faible) : moins de requêtes réseau, écran moins lumineux. */
    var batterySaving by mutableStateOf(false)

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
    /** Limitation d'après la carte (avant correction de la communauté) : l'« ancienne » valeur d'une correction. */
    private var mapLimit: Int? = null
    private var lastOverWarning = 0L
    /** Petit bip discret de dépassement (avertissements coupés) : déjà bipé pour cet excès, et quand. */
    private var softOver = false
    private var lastSoftBeep = 0L
    private var overWarned = false
    private var avgOverSaid = false
    private var nextSurpriseAt = 0L

    /** « Coquin XXL » : une phrase surprise de temps en temps (25 à 45 min), jamais près d'une manœuvre ni d'un radar. */
    private fun surprise(f: Fix) {
        if (!voice.xxlOn || f.speedKmh < 20) return
        if (nextSurpriseAt == 0L) { nextSurpriseAt = f.timeMs + 20 * 60_000L; return }
        if (f.timeMs < nextSurpriseAt || alert != null || (guidanceState?.toNextM ?: Int.MAX_VALUE) < 1500) return
        nextSurpriseAt = f.timeMs + (25 + (Math.random() * 20).toInt()) * 60_000L
        voice.xxl("surprise")?.let(voice::speak)
    }

    /** « Coquin XXL » : distances restantes (100, 50, 20, 10, 5, 2, 1 km, 500 m), une fois chacune par itinéraire. */
    private val milestones = listOf(100_000, 50_000, 20_000, 10_000, 5_000, 2_000, 1_000, 500)
    private val milestonesDone = HashSet<Int>()
    private var milestoneRoute: Route? = null
    private fun remainingMilestone(r: Route, state: GuidanceState) {
        if (milestoneRoute !== r) {
            milestoneRoute = r; milestonesDone.clear()
            milestones.filter { it >= state.remainingM }.forEach { milestonesDone.add(it) } // déjà en dessous au départ
            return
        }
        val due = milestones.firstOrNull { it !in milestonesDone && state.remainingM <= it } ?: return
        milestones.filter { it >= due }.forEach { milestonesDone.add(it) }
        // Pas en même temps qu'une consigne, ni juste avant une manœuvre.
        if (voice.xxlOn && state.speak == null && state.toNextM > 300 && !state.arrived) voice.xxl("remaining_$due")?.let(voice::speak)
    }
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
            if (night) voice.speakNamed("Il est tard et tu roules depuis un moment : la vigilance baisse. Fais une pause.", fr.roadguard.core.radar.SoundLevel.ALERTS)
            else voice.speakNamed(if (h < 2.2) "Deux heures de route. Pense à faire une pause." else "Cela fait %.0f heures de route. Une pause s'impose.".format(java.util.Locale.FRANCE, h), fr.roadguard.core.radar.SoundLevel.ALERTS)
            nextPauseAt = now + if (night) 20 * 60_000L else 30 * 60_000L
        }
    }
    private var limitCandidate: Int? = null
    private var overSince = 0L
    private val overEpisodes = ArrayDeque<Long>()
    private var lastScold = 0L
    private var lastExtraAt = 0L // dernier recalcul des radars signalés (throttle anti-saccade)
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
    private var lastAheadCheck = 0L
    private fun contextAnnouncements(f: Fix, now: Long) {
        // Étape atteinte (à moins de 150 m, comme le recalcul qui l'ignore alors) : retirée de la liste,
        // pour ne pas y retourner en cas de recalcul (adresse parfois un peu à l'écart de la route).
        if (navigating && stops.isNotEmpty()) {
            stops.firstOrNull { fr.roadguard.core.radar.Geo.distanceMeters(f.lat, f.lon, it.point.lat, it.point.lon) < 150 }?.let { reached ->
                stops = stops - reached
                voice.speak("Étape ${reached.name} atteinte." + if (stops.isEmpty()) " Direction l'arrivée." else " Encore ${stops.size} étape${if (stops.size > 1) "s" else ""}.")
            }
        }
        // Pluie, profil, vitesse perso : la limite affichée suit.
        val adj = adjusted(rawLimit)
        if (rawLimit != null && adj != speedLimit) {
            speedLimit = adj
            if (settings.announceLimit && adj != null) voice.speakAlert(if (localInfo.raining && settings.rainLimits) "Il pleut : limitation abaissée à $adj" else "Limitation $adj")
        }
        if (localInfo.raining != lastRain) { lastRain = localInfo.raining }
        // Suisse : annonces de radars interdites.
        val c = localInfo.country
        if (c != null && c != lastCountry) {
            if (lastCountry != null && settings.swissMode) {
                if (c == "CH") voice.speakRadar("Tu entres en Suisse : les alertes radars sont coupées, comme l'exige la loi suisse.")
                else if (lastCountry == "CH") voice.speakRadar("Tu quittes la Suisse : alertes radars rétablies.")
            } else if (lastCountry != null && c == "CH" && settings.radarsOn) {
                voice.speakRadar("Tu entres en Suisse : les alertes radars y sont interdites. Tu as choisi de les garder, sous ta responsabilité.")
            }
            lastCountry = c
        }
        // Météo dangereuse ici, puis sur l'itinéraire.
        if (settings.weatherAlerts) {
            val h = localInfo.hazard
            if (h != null && (h != lastHazard || now - lastHazardAt > 30 * 60_000)) {
                lastHazard = h; lastHazardAt = now
                val plain = when (h) {
                    "Risque de verglas", "Pluie verglaçante" -> "Attention : $h. Roule doucement et garde tes distances."
                    "Brouillard" -> "Brouillard : allume tes feux et garde tes distances."
                    else -> "Attention : $h. Adapte ta vitesse."
                }
                voice.speakAlert(when (h) {
                    "Brouillard" -> voice.xxl("fog")
                    "Risque de verglas", "Pluie verglaçante" -> voice.xxl("ice")?.let { "Attention : $h. $it" }
                    else -> null
                } ?: plain)
            } else if (h == null) lastHazard = null
            // Point situé 20 km DEVANT sur le tracé (et non le premier point à plus de 20 km, qui peut être derrière).
            val g = guidance
            if (g != null && route != null) {
                if (now - lastAheadCheck > 60_000) {
                    lastAheadCheck = now
                    g.pointAhead(20_000.0)?.let { localInfo.checkAhead(it.lat, it.lon, now) }
                }
                val ha = localInfo.hazardAhead
                if (ha != null && ha != lastAheadHazard && ha != h) { lastAheadHazard = ha; voice.speakAlert("Sur ta route, dans une vingtaine de kilomètres : $ha.") }
            }
        }
        if (settings.hazardAlerts && !batterySaving) hazards.onFix(f, now)?.let { voice.speakAlert(voice.xxlHazard(it) ?: it) }
    }

    // Fin de trajet : arrêt d'au moins 3 min après au moins 5 min de route (ou Bluetooth de la voiture coupé).
    // Une minute ne suffit pas : un bouchon, un feu long ou un passage à niveau terminaient le trajet en pleine route.
    private var slowSince = 0L
    private fun tripStep(f: Fix, now: Long) {
        meter.onFix(f.lat, f.lon, f.speedKmh, overSpeed, f.timeMs, localInfo.address)
        if (f.speedKmh >= 5) { slowSince = 0; return }
        if (slowSince == 0L) slowSince = now
        if (meter.active && now - slowSince > 3 * 60_000 && now - meter.start > 5 * 60_000) endTrip(f.lat, f.lon, end = slowSince)
    }

    /** Termine le trajet en cours : bilan, voiture garée, rappels. [end] : heure d'arrêt (l'attente n'est pas comptée). */
    fun endTrip(lat: Double? = here?.lat, lon: Double? = here?.lon, end: Long = System.currentTimeMillis()) {
        val rec = meter.finish(end.coerceAtLeast(meter.start), localInfo.address, settings.defaultTripPro) ?: return
        tripLog.addTrip(rec)
        lastTrip = rec
        if (lat != null && lon != null) tripLog.parking = fr.roadguard.core.safety.Parking(lat, lon, System.currentTimeMillis(), localInfo.address, 0)
        voice.xxl("trip_end")?.let(voice::speak)
        if (settings.forgetReminder) voice.speakNamed("Fin du trajet. Pense à vérifier : rien oublié à l'arrière ? Enfant, animal, sac ?", fr.roadguard.core.radar.SoundLevel.MUTE)
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
    fun cancelCrash() { crashAt = 0L; impactAt = 0L; voice.speakNamed("D'accord. Bonne route, et prends soin de toi.", fr.roadguard.core.radar.SoundLevel.MUTE) }
    private fun crashStep(kmh: Double, now: Long) {
        if (kmh >= 25) lastFastAt = now
        // Choc puis arrêt dans les 15 s : on demande si tout va bien.
        if (impactAt != 0L && crashAt == 0L) {
            if (kmh < 5) {
                crashAt = now; impactAt = 0L
                val who = settings.emergencyName.ifBlank { "ton contact d'urgence" }
                voice.wakeUp()
                voice.speakNamed(min = fr.roadguard.core.radar.SoundLevel.MUTE, text = "Choc détecté. Tout va bien ? Touche « Je vais bien ». Sinon, je préviens $who dans trente secondes.")
            } else if (now - impactAt > 15_000) impactAt = 0L
        }
    }
    private fun crashTimeout(now: Long) {
        if (crashAt == 0L || now - crashAt < 30_000) return
        crashAt = 0L
        val p = here
        val phone = settings.emergencyPhone
        if (phone.isBlank()) { voice.speakAlways("Aucun contact d'urgence enregistré. Appelle le 112 si tu as besoin d'aide."); return }
        val pos = p?.let { "https://maps.google.com/?q=%.5f,%.5f".format(java.util.Locale.US, it.lat, it.lon) } ?: "position inconnue"
        val text = "Pilote : un choc a été détecté et le conducteur ne répond pas. Position : $pos. " +
            "Appelle-le, et le 112 si besoin."
        val ok = sendEmergency?.invoke(phone, text) == true
        voice.speakAlways(if (ok) "Message envoyé à ${settings.emergencyName.ifBlank { "ton contact" }}." else "Impossible d'envoyer le message. Appelle le 112 si tu as besoin d'aide.")
    }

    // ---------- Tunnels : position estimée quand le GPS ne répond plus ----------
    private var lastRealFixAt = 0L
    private var lastReal: Fix? = null
    private var gpsLostSaid = false
    private fun deadReckon(now: Long) {
        crashTimeout(now)
        val f0 = lastReal ?: return
        val lost = now - lastRealFixAt
        // « Coquin XXL » : signal GPS perdu en roulant pendant un guidage (tunnel…), dit une fois.
        if (navigating && !gpsLostSaid && lost in 8_000L..120_000L && f0.speedKmh >= 30) {
            gpsLostSaid = true
            voice.xxl("gps_lost")?.let(voice::speak)
        }
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
        community.askAloud = { text, then -> voice.speakThen(text, then) }
        community.currentWay = { SpeedLimits.lastWay }
        community.currentLimit = { mapLimit }
        community.announceAloud = { r, meters -> voice.xxlReport(r.type.code, meters, r.yes) }
        // Bouton son : contrôles signalés avec les radars, le reste (accident, bouchon, danger…) avec les alertes.
        community.sayReport = { type, text -> if (type.radarLike) voice.speakRadar(text) else voice.speakAlert(text) }
        community.startConvoyLoop()
        // Échéances de la voiture (contrôle technique, assurance…) : notification un mois avant.
        scope.launch { kotlinx.coroutines.delay(20_000); runCatching { fr.roadguard.core.safety.Reminders.check(context) } }
    }

    /** À appeler une fois (bases radars chargées en arrière-plan). */
    suspend fun prepare() {
        val index = RadarRepository.index(context)
        // Radar fixe signalé là où un radar est déjà connu des bases : validé d'office.
        community.knownRadarNear = { lat, lon -> index.near(lat, lon).any { fr.roadguard.core.radar.Geo.distanceMeters(lat, lon, it.lat, it.lon) < 100 } }
        alerts = AlertEngine(index)
        averages = AverageSpeedTracker(index)
        // Mode conversation : suit le réglage (démarre/arrête le micro).
        scope.launch { androidx.compose.runtime.snapshotFlow { settings.conversationOn }.collect { setConversation(it) } }
    }

    // ---------- Mode conversation ----------
    private var ears: fr.roadguard.core.community.ConversationEars? = null

    // Mode « auto » : un seul personnage qui s'adapte. Niveau 1 = amical, 2 = amoureux, 3 = coquin. Il monte selon ce
    // que dit le conducteur (tendresse, flirt…) et redescend tout seul après quelques échanges neutres, ou tout de
    // suite s'il demande de se calmer. Pas besoin de changer de réglage : la relation évolue d'elle-même.
    private var mood = 1
    private var moodCalm = 0

    private fun autoType(raw: String): String {
        val t = fr.roadguard.core.community.Conversation.normalize(raw)
        fun has(vararg w: String) = w.any { " $it " in t }
        when {
            has("arrete", "stop", "calme", "calme toi", "soft", "pas a l aise", "change de sujet", "on se calme",
                "trop loin", "reste correct", "on arrete") -> { mood = 1; moodCalm = 0 }
            has("coquin", "coquine", "sexy", "chaud", "chaude", "envie de toi", "tu me chauffes", "excite", "excitant",
                "vilain", "vilaine", "cochon", "embrasse", "embrasse moi", "bisou", "caresse", "taquine") -> { mood = 3; moodCalm = 0 }
            has("je t aime", "mon amour", "mon coeur", "cheri", "cherie", "tu me manques", "calin", "tendre", "tendresse",
                "amoureux", "amoureuse", "bebe", "je t adore", "tu me plais", "je pense a toi") -> { mood = maxOf(mood, 2); moodCalm = 0 }
            else -> { moodCalm++; if (moodCalm >= 6) { mood = maxOf(1, mood - 1); moodCalm = 0 } }
        }
        return when (mood) { 3 -> "coquin"; 2 -> "amoureux"; else -> "ami" }
    }

    private fun startEars() {
        ears?.start()
        val ok = ears?.active == true
        fr.roadguard.core.community.Conversation.listening = ok
        fr.roadguard.core.community.Conversation.status = when {
            ok -> ""
            context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED -> "micro non autorisé"
            else -> "reconnaissance vocale indisponible sur ce téléphone"
        }
        // Micro pas encore autorisé (ou reconnaissance vocale pas prête) : on réessaie tout seul toutes les 3 s,
        // pour que l'écoute démarre dès que l'autorisation est donnée, sans avoir à couper/relancer le mode.
        if (!ok && settings.conversationOn) scope.launch {
            kotlinx.coroutines.delay(3000)
            if (settings.conversationOn && !fr.roadguard.core.community.Conversation.listening) startEars()
        }
    }

    private fun setConversation(on: Boolean) {
        fr.roadguard.core.community.Conversation.resetContext()
        mood = 1; moodCalm = 0
        if (!on) { ears?.stop(); fr.roadguard.core.community.Conversation.listening = false; fr.roadguard.core.community.Conversation.status = ""; return }
        if (ears == null) ears = fr.roadguard.core.community.ConversationEars(context) { heard -> onHeard(heard) }
        if (fr.roadguard.core.community.Conversation.loadCached(context)) startEars()
        else scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            fr.roadguard.core.community.Conversation.status = "téléchargement des phrases…"
            val got = fr.roadguard.core.community.Conversation.download(context, community.baseUrl)
            withContext(kotlinx.coroutines.Dispatchers.Main) {
                if (!settings.conversationOn) return@withContext
                if (got) startEars() else fr.roadguard.core.community.Conversation.status = "phrases non téléchargées (connexion ?)"
            }
        }
    }

    fun stopConversation() {
        ears?.stop()
        fr.roadguard.core.community.Conversation.listening = false
        fr.roadguard.core.community.Conversation.resetContext(); mood = 1; moodCalm = 0
    }

    /** Une phrase entendue : réponse en direct si possible, sinon phrase du pack, puis Pilote répond et se remet à écouter. */
    private fun onHeard(text: String) {
        // Genre de l'interlocuteur → le compagnon prend le genre opposé, et Pilote accorde ses mots (chéri/chérie…).
        val gender = settings.convGender
        fr.roadguard.core.community.Conversation.addressGender = gender
        val type = if (settings.convPersona == "auto") autoType(text) else settings.convPersona
        val persona = when (type) {
            "ami" -> if (gender == "femme") "pote" else "amie"
            "amoureux" -> if (gender == "femme") "copain" else "copine"
            "coquin" -> "coquin"
            else -> "normal"
        }
        // Si de la musique joue, le micro l'entend aussi : on ne répond alors qu'à une vraie question ou une phrase
        // reconnue, jamais à du bruit (pas de relance), pour ne pas bavarder par-dessus les paroles d'une chanson.
        val music = runCatching { (context.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager).isMusicActive }.getOrDefault(false)
        val conv = fr.roadguard.core.community.Conversation
        conv.familiar = settings.convFamiliar && persona != "normal"
        // Mémoire (jamais pendant la musique : des paroles de chanson ne doivent pas être retenues comme des faits).
        val mem = fr.roadguard.core.community.ConversationMemory
        val memOn = settings.convMemory && !music
        if (memOn) {
            mem.load(context)
            mem.seedName(runCatching { fr.roadguard.core.account.AccountManager.get(context).status?.firstName }.getOrNull())
            mem.observe(text)
            // Il retient quelque chose, répond à une question sur le conducteur, ou donne une réponse apprise.
            mem.handle(text)?.let { r -> voice.converse(conv.polish(r)) { ears?.listen() }; return }
        }
        val live = dynamicReply(text)?.let { conv.polish(it) }
        var reply = live ?: conv.staticReply(text, persona)?.let { if (memOn) mem.flavor(it, text, false) else it }
        if (reply == null && !music) {
            // Rien de précis reconnu : phrase dite souvent → Pilote demande quoi répondre ; sinon une relance, parfois
            // personnelle (ses proches, ses goûts), avec les mots du conducteur.
            reply = (if (memOn) mem.noteUnknown(text) else null)?.let { conv.polish(it) }
                ?: (if (memOn && Math.random() < 0.25) mem.personal() else null)?.let { conv.polish(it) }
                ?: conv.continuerReply(persona)?.let { if (memOn) mem.flavor(it, text, true) else it }
                ?: conv.fallbackReply(persona)
        }
        if (reply != null) voice.converse(reply) { ears?.listen() } else ears?.listen()
    }

    /** Réponses « vivantes » (données du trajet) par mots-clés ; null si la question n'en est pas une. */
    private fun dynamicReply(text: String): String? {
        val t = fr.roadguard.core.community.Conversation.normalize(text)
        fun has(vararg words: String) = words.any { " $it " in t || t.trim() == it }
        val kmh = here?.speedKmh?.toInt() ?: 0
        return when {
            has("ma vitesse", "quelle vitesse", "je roule a combien", "je vais a combien", "combien je roule") ->
                if (kmh > 0) "Tu roules à $kmh kilomètres heure." else "Tu es à l'arrêt."
            has("la limitation", "limite ici", "limite a combien", "c'est limite a combien", "quelle limitation") ->
                speedLimit?.let { "Ici c'est limité à $it." } ?: "Je ne connais pas la limitation à cet endroit."
            has("quelle heure", "il est quelle heure", "l'heure") ->
                "Il est " + java.text.SimpleDateFormat("H'h'mm", java.util.Locale.FRANCE).format(java.util.Date()) + "."
            has("prochain radar", "un radar", "y a un radar", "des radars", "radar devant") -> alert?.let { a ->
                "Radar à ${if (a.distanceM >= 1000) "%.1f kilomètres".format(java.util.Locale.FRANCE, a.distanceM / 1000.0) else "${a.distanceM} mètres"}."
            } ?: "Aucun radar en vue pour l'instant."
            has("on arrive quand", "il reste combien de temps", "encore combien de temps", "combien de temps", "on arrive bientot", "c'est encore loin", "encore loin") ->
                guidanceState?.let { g ->
                    if (g.remainingS > 60) "Il reste environ ${Math.round(g.remainingS / 60.0)} minutes." else "On arrive, c'est tout près."
                } ?: "Aucun trajet en cours. Lance un guidage et je te dirai."
            has("il reste combien de kilometres", "encore combien de route", "combien de kilometres", "encore combien de kilometres") ->
                guidanceState?.let { g ->
                    if (g.remainingM >= 1000) "Il reste %.0f kilomètres.".format(java.util.Locale.FRANCE, g.remainingM / 1000.0) else "Il reste ${g.remainingM} mètres."
                } ?: "Aucun trajet en cours."
            has("on est ou", "ou je suis", "ou on est", "quelle ville", "quelle route") ->
                localInfo.address?.takeIf { it.isNotBlank() }?.let { "On est vers $it." } ?: "Je ne sais pas trop où on est là."
            else -> null
        }
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
        // Radars signalés par la communauté : recalculés au plus toutes les 2 s (les radars ne bougent pas), pour ne
        // pas reparcourir tous les signalements et créer des objets à chaque point GPS — ce qui faisait saccader la carte.
        if (!settings.radarsOn || radarsBlockedHere) {
            alerts?.extra = emptyList(); lastExtraAt = 0L
        } else if (f.timeMs - lastExtraAt >= 2_000) {
            lastExtraAt = f.timeMs
            alerts?.extra = community.alertRadars(f.lat, f.lon).mapNotNull { r ->
                val fixed = r.type == fr.roadguard.core.community.ReportType.FIXED
                // Radar fixe déjà dans les bases : il est annoncé une seule fois, par la base.
                if (fixed && community.knownRadarNear?.invoke(r.lat, r.lon) == true) null
                else fr.roadguard.core.radar.Radar("c-${r.id}", r.lat, r.lon,
                    if (fixed) fr.roadguard.core.radar.RadarKind.FIXED else fr.roadguard.core.radar.RadarKind.MOBILE, "Signalé", null)
            }
        }
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
            if (Math.random() < 0.5) voice.xxl("radar_passed")?.let(voice::speak)
        }
        // Excès de vitesse en approchant d'un radar : alarme forte qui réveille, jusqu'à repasser sous la limite.
        val cur = alert
        val limit = cur?.radar?.speedKmh
        val radarOver = settings.radarAlarm && cur != null && limit != null &&
            fr.roadguard.core.nav.SpeedLimitTracker.isOver(f.speedKmh.toInt(), limit, settings.overTolerance)
        // Option : la même alarme dès qu'on dépasse la limitation de la route, même sans radar devant.
        // Avertissements de dépassement coupés (« Me prévenir si je dépasse ») : plus aucune voix pour ralentir
        // (ni « Ralentis » de l'alarme, ni réprimande), seulement un petit bip discret (plus bas).
        val talkOver = settings.speedLimitWarning
        val roadLimit = speedLimit
        val roadOver = talkOver && settings.alarmAnywhere && roadLimit != null &&
            fr.roadguard.core.nav.SpeedLimitTracker.isOver(f.speedKmh.toInt(), roadLimit, settings.overTolerance)
        voice.overspeedAlarm(radarOver || roadOver, if (radarOver) limit else roadLimit, talk = talkOver, radar = radarOver)
        // Réprimande : excès qui dure (plus de 15 s) ou qui se répète (3 fois en 10 min), au plus toutes les 45 s.
        val overNow = radarOver || (roadLimit != null && fr.roadguard.core.nav.SpeedLimitTracker.isOver(f.speedKmh.toInt(), roadLimit, settings.overTolerance))
        if (overNow) { if (overSince == 0L) { overSince = f.timeMs; overEpisodes.addLast(f.timeMs) } } else overSince = 0L
        while (overEpisodes.isNotEmpty() && f.timeMs - overEpisodes.first() > 600_000) overEpisodes.removeFirst()
        if (talkOver && settings.scold && overNow && (f.timeMs - overSince > 15_000 || overEpisodes.size >= 3) && f.timeMs - lastScold > 45_000) {
            lastScold = f.timeMs; voice.scold()
        }
        voice.proximityBeeps = settings.proximityBeeps
        val a = alert
        // (Sirène de police avant les radars retirée à la demande de Nathan : la sirène ne sert plus qu'à l'anti-somnolence.)
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
        voice.level = settings.soundLevel
        voice.duckMusic = settings.duckMusic
        voice.configure(settings.voiceName, settings.speechRate, settings.voiceTone, settings.voiceLang)
        voice.realistic = false // même voix (celle du téléphone) pour le guidage et les radars
        voice.pack.feminine = settings.driverFemale
        community.voiceAnswers = settings.voiceConfirm && settings.voice
        // Communauté (police, radars signalés) : jamais en Suisse, où avertir publiquement des contrôles est puni (art. 98a LCR).
        community.radarAlertsAllowed = settings.radarsOn && !radarsBlockedHere && localInfo.country != "CH"
        // En Suisse, radars rallumés (bouton « Suisse ») : seulement ses propres radars, gardés privés.
        community.ownRadarsOnly = settings.radarsOn && !radarsBlockedHere && localInfo.country == "CH"
        val f = completeFix(location.toAlertFix())
        // Vitesse calculée par ROADGUARD si le GPS ne la donne pas (certains postes).
        fix = location.toSpeedFix().let { sf -> if (!location.hasSpeed() && f.speedKmh > 0) sf.copy(kmh = f.speedKmh.toInt()) else sf }
        here = f

        lastReal = f; lastRealFixAt = System.currentTimeMillis()
        if (gpsLostSaid) { gpsLostSaid = false; voice.xxl("gps_back")?.let(voice::speak) }
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
        // « Coquin XXL » : moyenne au-dessus de la limite pendant le tronçon (une fois par tronçon).
        if (newAverage != null && !newAverage.finished && newAverage.over && newAverage.meters > 500 && !avgOverSaid) {
            avgOverSaid = true
            voice.xxl("avg_over")?.let { voice.speakRadar("$it Moyenne ${newAverage.averageKmh}, limite ${newAverage.limitKmh}.") }
        }
        if (newAverage == null || newAverage.finished) avgOverSaid = false
        average = newAverage

        autoNight = DayNight.isNight(f.lat, f.lon, System.currentTimeMillis())
        // Journal de diagnostic (essais émulateur et poste) : une ligne toutes les 5 s au plus.
        if (f.timeMs - lastLogAt > 5_000) {
            lastLogAt = f.timeMs
            android.util.Log.i("RoadguardSession", "pos=%.5f,%.5f v=%.0f cap=%.0f prec=%.0f nuit=%b alerte=%s radars=%b".format(
                java.util.Locale.US, f.lat, f.lon, f.speedKmh, f.bearing, f.accuracyM, night,
                alert?.let { "${it.radar.id}@${it.distanceM.toInt()}m" } ?: "-", alerts != null))
        }
        limitTracker.add(f.lat, f.lon, f.timeMs, f.speedKmh, if (batterySaving) 8_000 else 3_000)?.let { trail ->
            // Limitations prises 50 m en avance : on lit celle du point 50 m devant (le long de l'itinéraire s'il y en a un).
            val ahead = if (f.bearing.isNaN()) null
                else advance(f.lat, f.lon, f.bearing, SpeedLimits.AHEAD_M).let { (la, lo) -> fr.roadguard.core.nav.LatLon(la, lo) }
            if (!limitBusy) {
                limitBusy = true
                scope.launch {
                    try {
                        SpeedLimits.current(trail, ahead)?.let { read ->
                            // Limitation corrigée par la communauté sur la portion de route lue (50 m devant) : elle remplace celle de la carte.
                            val fixed = community.limitFix(SpeedLimits.aheadWay, ahead?.lat ?: f.lat, ahead?.lon ?: f.lon)
                            mapLimit = read.takeIf { it > 0 }
                            onLimit(fixed ?: read)
                        }
                    } finally { limitBusy = false }
                }
            }
        }
        val kmh = fix?.kmh ?: 0
        val overNowLimit = SpeedLimitTracker.isOver(kmh, speedLimit, settings.overTolerance)
        if (settings.speedLimitWarning && overNowLimit && f.timeMs - lastOverWarning > 30_000) {
            lastOverWarning = f.timeMs
            overWarned = true
            voice.overLimit(speedLimit, kmh)
        }
        // Avertissements coupés : un tout petit bip en passant au-dessus de la limite, rappelé au plus toutes les 60 s.
        if (!settings.speedLimitWarning) {
            if (overNowLimit && (!softOver || f.timeMs - lastSoftBeep > 60_000)) { softOver = true; lastSoftBeep = f.timeMs; voice.softBeep() }
            if (!overNowLimit) softOver = false
        } else softOver = false
        // « Coquin XXL » : retour sous la limite après un avertissement.
        val lim = speedLimit
        if (overWarned && lim != null && kmh <= lim) { overWarned = false; voice.xxl("back_under")?.let(voice::speak) }
        if (lim == null) overWarned = false
        surprise(f)
        pauseReminder(f.speedKmh, f.timeMs)

        if (navigating && route == null && !rerouting && navStatus?.startsWith("En attente du GPS") == true) {
            pendingDestination?.let { computeRoute(it, announce = true) }
        }
        guidance?.let { g ->
            val state = g.update(f.lat, f.lon, f.speedKmh)
            // Choix du trajet proposé au départ seulement : retiré après 1 km parcouru.
            if (alternatives.isNotEmpty() && g.route.lengthM - state.remainingM > 1000) alternatives = emptyList()
            guidanceState = state
            // Consigne au ton choisi ; en « Coquin XXL », phrase de la bibliothèque qui dit la même manœuvre.
            state.speak?.let { voice.guideStep(it, state.next, state.toNextM, state.speakKind,
                hasThen = state.speakKind == GuidanceState.NOW && state.then != null) }
            remainingMilestone(g.route, state)
            // Arrivé : le guidage s'arrête de lui-même (1 min après, ou dès qu'on s'éloigne pour se garer),
            // au lieu de recalculer un trajet pour revenir à la destination.
            if (state.arrived && stops.isEmpty()) {
                if (arrivedAt == 0L) arrivedAt = f.timeMs
                if (state.offRoute || f.timeMs - arrivedAt > 60_000) { stopNavigation(); return }
            }
            // Sortie du trajet : nouveau calcul, au plus toutes les 15 s.
            if (state.offRoute && !state.arrived && !rerouting && f.timeMs - lastRerouteAt > 15_000) {
                lastRerouteAt = f.timeMs
                computeRoute(g.route.destination, announce = false)
            }
        }
    }

    /** Étapes avant la destination (dans l'ordre choisi, ou réordonnées au plus rapide). */
    var stops by mutableStateOf<List<Place>>(emptyList()); private set
    fun addStop(place: Place) { stops = stops + place; if (navigating) pendingDestination?.let { computeRoute(it, announce = true) } }
    fun removeStop(place: Place) { stops = stops - place; if (navigating) pendingDestination?.let { computeRoute(it, announce = true) } }
    fun clearStops() { stops = emptyList() }

    /** Langue Valhalla des consignes selon la langue des annonces. */
    private fun routeLanguage() = when (settings.voiceLang) { "en" -> "en-GB"; "de" -> "de-DE"; "it" -> "it-IT"; else -> "fr-FR" }

    fun startNavigation(place: Place) {
        recents = recentStore.add(place)
        arrivedAt = 0L
        navigating = true; route = null; guidance = null; guidanceState = null
        pendingDestination = place.point
        navStatus = "Calcul du trajet vers ${place.name}…"
        computeRoute(place.point, announce = true)
    }

    fun stopNavigation() {
        routeRequest++ // un calcul encore en cours ne relancera pas le guidage
        rerouting = false
        arrivedAt = 0L
        stops = emptyList()
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

    /** Numéro du dernier calcul demandé : un calcul plus ancien qui finit après ne remplace pas le trajet. */
    private var routeRequest = 0
    private var arrivedAt = 0L

    private fun computeRoute(destination: LatLon, announce: Boolean) {
        val from = here
        if (from == null) { navStatus = "En attente du GPS…"; return }
        rerouting = true
        val req = ++routeRequest
        scope.launch {
            val start = LatLon(from.lat, from.lon)
            val heading = from.bearing.takeUnless { it.isNaN() }
            runCatching {
                // En ligne d'abord (Valhalla, avec variantes au départ) ; sans réseau, graphe routier installé.
                runCatching {
                    // Recalcul en route : seules les étapes encore devant (à plus de 150 m) comptent.
                    val vias = stops.map { it.point }.filter { fr.roadguard.core.radar.Geo.distanceMeters(start.lat, start.lon, it.lat, it.lon) > 150 }
                    NavApi.routes(start, heading, destination, if (announce) 2 else 0, settings.avoidTolls, settings.avoidHighways,
                        vias = vias, optimize = settings.optimizeStops, narrow = settings.routeNarrow, eco = settings.routeEco, language = routeLanguage())
                }.getOrElse { online ->
                    if (stops.isNotEmpty()) throw online // le calcul hors ligne ne gère pas les étapes
                    val offline = fr.roadguard.core.nav.OfflineRouter.route(context, start, heading, destination)
                        ?: throw java.io.IOException(
                            if (fr.roadguard.core.nav.OfflineRouter.installed(context).isEmpty()) online.message ?: "pas de réseau"
                            else "pas de réseau et région non couverte par les itinéraires hors ligne"
                        )
                    listOf(offline)
                }
            }.onSuccess { all ->
                if (!navigating || req != routeRequest) return@onSuccess
                val r = all.first()
                alternatives = if (announce && all.size > 1) all else emptyList()
                route = r; guidance = Guidance(r); guidanceState = null; navStatus = null
                if (!announce) voice.speak(voice.xxl("reroute") ?: "Nouvel itinéraire")
            }.onFailure {
                if (navigating && req == routeRequest) navStatus = "Trajet impossible : ${it.message ?: "pas de réseau"}"
            }
            if (req == routeRequest) rerouting = false
        }
    }
}
