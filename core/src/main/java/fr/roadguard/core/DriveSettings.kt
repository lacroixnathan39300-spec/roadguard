package fr.roadguard.core

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fr.roadguard.core.radar.RadarKind

enum class ThemeMode(val label: String) { AUTO("Automatique"), DAY("Jour"), NIGHT("Nuit") }

/** Réglages de DRIVE, mémorisés sur le poste et observables par l'interface. */
class DriveSettings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val appContext: Context = context.applicationContext ?: context

    var voice by mutableStateOf(prefs.getBoolean("voice", true)); private set
    var beep by mutableStateOf(prefs.getBoolean("beep", true)); private set
    var readMessages by mutableStateOf(prefs.getBoolean("read_messages", true)); private set
    var speedLimitWarning by mutableStateOf(prefs.getBoolean("speed_limit_warning", true)); private set
    var theme by mutableStateOf(runCatching { ThemeMode.valueOf(prefs.getString("theme", "AUTO")!!) }.getOrDefault(ThemeMode.AUTO)); private set
    var alertKinds by mutableStateOf(
        prefs.getStringSet("alert_kinds", null)?.mapNotNull { runCatching { RadarKind.valueOf(it) }.getOrNull() }?.toSet()
            ?: setOf(RadarKind.FIXED, RadarKind.REDLIGHT, RadarKind.AVERAGE, RadarKind.SECTION, RadarKind.MOBILE)
    ); private set

    /** DRIVE : s'ouvre tout seul au démarrage ou au réveil du poste. */
    var autostart by mutableStateOf(prefs.getBoolean("autostart", true)); private set
    var accent by mutableStateOf(prefs.getLong("accent", 0xFF00B8FF)); private set
    var scale by mutableStateOf(prefs.getFloat("scale", 1f)); private set

    init { fr.roadguard.core.ui.DriveColors.Accent = androidx.compose.ui.graphics.Color(accent); fr.roadguard.core.ui.DriveColors.Scale = scale }

    var avoidTolls by mutableStateOf(prefs.getBoolean("avoid_tolls", false)); private set
    var avoidHighways by mutableStateOf(prefs.getBoolean("avoid_highways", false)); private set
    fun changeAvoidTolls(v: Boolean) { avoidTolls = v; prefs.edit().putBoolean("avoid_tolls", v).apply() }
    fun changeAvoidHighways(v: Boolean) { avoidHighways = v; prefs.edit().putBoolean("avoid_highways", v).apply() }

    var northUp by mutableStateOf(prefs.getBoolean("north_up", false)); private set
    var adaptiveZoom by mutableStateOf(prefs.getBoolean("adaptive_zoom", true)); private set
    fun changeNorthUp(v: Boolean) { northUp = v; prefs.edit().putBoolean("north_up", v).apply() }
    fun changeAdaptiveZoom(v: Boolean) { adaptiveZoom = v; prefs.edit().putBoolean("adaptive_zoom", v).apply() }

    var proximityBeeps by mutableStateOf(prefs.getBoolean("proximity_beeps", true)); private set
    fun changeProximityBeeps(v: Boolean) { proximityBeeps = v; prefs.edit().putBoolean("proximity_beeps", v).apply() }

    /** DRIVE : source GPS « auto », « unit » (poste) ou « phone » (téléphone par Link). */
    var gpsSource by mutableStateOf(prefs.getString("gps_source", "auto") ?: "auto"); private set
    fun changeGpsSource(v: String) { gpsSource = v; prefs.edit().putString("gps_source", v).apply() }

    var duckMusic by mutableStateOf(prefs.getBoolean("duck_music", true)); private set
    fun changeDuckMusic(v: Boolean) { duckMusic = v; prefs.edit().putBoolean("duck_music", v).apply() }

    // Disposition de l'accueil DRIVE.
    var mapRight by mutableStateOf(prefs.getBoolean("map_right", false)); private set
    var mapWeight by mutableStateOf(prefs.getFloat("map_weight", 0.62f)); private set
    var showWeather by mutableStateOf(prefs.getBoolean("show_weather", true)); private set
    var showMusic by mutableStateOf(prefs.getBoolean("show_music", true)); private set
    var showVolume by mutableStateOf(prefs.getBoolean("show_volume", true)); private set
    fun changeMapRight(v: Boolean) { mapRight = v; prefs.edit().putBoolean("map_right", v).apply() }
    fun changeMapWeight(v: Float) { mapWeight = v; prefs.edit().putFloat("map_weight", v).apply() }
    fun changeShowWeather(v: Boolean) { showWeather = v; prefs.edit().putBoolean("show_weather", v).apply() }
    fun changeShowMusic(v: Boolean) { showMusic = v; prefs.edit().putBoolean("show_music", v).apply() }
    fun changeShowVolume(v: Boolean) { showVolume = v; prefs.edit().putBoolean("show_volume", v).apply() }

    /** Bulle vitesse/radar par-dessus les autres applis. */
    var bubble by mutableStateOf(prefs.getBoolean("bubble", true)); private set
    fun changeBubble(v: Boolean) { bubble = v; prefs.edit().putBoolean("bubble", v).apply() }

    var voiceName by mutableStateOf(prefs.getString("voice_name", null)); private set
    var speechRate by mutableStateOf(prefs.getFloat("speech_rate", 1f)); private set
    fun changeVoiceName(v: String?) { voiceName = v; prefs.edit().putString("voice_name", v).apply() }
    fun changeSpeechRate(v: Float) { speechRate = v; prefs.edit().putFloat("speech_rate", v).apply() }

    /** Style de carte : « roadguard » (nouveau, façon Waze) ou « classic ». */
    var mapStyle by mutableStateOf(prefs.getString("map_style", "roadguard") ?: "roadguard"); private set
    fun changeMapStyle(v: String) { mapStyle = v; prefs.edit().putString("map_style", v).apply() }


    /** Alarme forte quand on dépasse la limite en approchant d'un radar. */
    var radarAlarm by mutableStateOf(prefs.getBoolean("radar_alarm", true)); private set
    fun changeRadarAlarm(v: Boolean) { radarAlarm = v; prefs.edit().putBoolean("radar_alarm", v).apply() }
    // Anti-somnolence (caméra avant, Pilote téléphone).
    var drowsyGuard by mutableStateOf(prefs.getBoolean("drowsy_guard", false)); private set
    fun changeDrowsyGuard(v: Boolean) { drowsyGuard = v; prefs.edit().putBoolean("drowsy_guard", v).apply() }
    /** Sirène de police française (deux-tons) quand le conducteur s'endort (sinon : sonnerie d'alarme). */
    var sirenWake by mutableStateOf(prefs.getBoolean("siren_wake", true)); private set
    fun changeSirenWake(v: Boolean) { sirenWake = v; prefs.edit().putBoolean("siren_wake", v).apply() }
    /** Sirène de police au dernier moment avant un radar : 0 jamais, 1 seulement si trop vite, 2 à chaque radar. */
    var sirenRadar by mutableStateOf(prefs.getInt("siren_radar", 2)); private set
    fun changeSirenRadar(v: Int) { sirenRadar = v; prefs.edit().putInt("siren_radar", v).apply() }
    // Réprimandes si l'excès de vitesse dure ou se répète.
    var scold by mutableStateOf(prefs.getBoolean("scold", true)); private set
    fun changeScold(v: Boolean) { scold = v; prefs.edit().putBoolean("scold", v).apply() }
    // Alarme forte aussi hors radar, dès qu'on dépasse la limitation de la route.
    var alarmAnywhere by mutableStateOf(prefs.getBoolean("alarm_anywhere", false)); private set
    fun changeAlarmAnywhere(v: Boolean) { alarmAnywhere = v; prefs.edit().putBoolean("alarm_anywhere", v).apply() }

    /** Rappel de pause après 2 h de conduite. */
    var pauseReminder by mutableStateOf(prefs.getBoolean("pause_reminder", true)); private set
    fun changePauseReminder(v: Boolean) { pauseReminder = v; prefs.edit().putBoolean("pause_reminder", v).apply() }

    /** DRIVE : ancienne disposition fixe (carte + colonne) au lieu de l'accueil en pages et modules. */
    var classicHome by mutableStateOf(prefs.getBoolean("classic_home", false)); private set
    fun changeClassicHome(v: Boolean) { classicHome = v; prefs.edit().putBoolean("classic_home", v).apply() }

    /** DRIVE : horloge de l'accueil (0 = heure, 1 = avec secondes, 2 = avec la date). */
    var clockStyle by mutableStateOf(prefs.getInt("clock_style", 0)); private set
    fun changeClockStyle(v: Int) { clockStyle = v; prefs.edit().putInt("clock_style", v).apply() }

    /** DRIVE : gestes sur les bords de l'écran (retour, applis, volume). */
    var edgeGestures by mutableStateOf(prefs.getBoolean("edge_gestures", true)); private set
    fun changeEdgeGestures(v: Boolean) { edgeGestures = v; prefs.edit().putBoolean("edge_gestures", v).apply() }

    /** Marge avant l'alerte de dépassement : -1 = tolérance légale (5 km/h, 5 % au-delà de 100), sinon en km/h. */
    var overTolerance by mutableStateOf(prefs.getInt("over_tolerance", -1)); private set
    fun changeOverTolerance(v: Int) { overTolerance = v; prefs.edit().putInt("over_tolerance", v).apply() }

    /** Annonce vocale quand la limitation change (« Limitation 50 »). */
    // Interrupteurs rapides de l'accueil : alertes radars, et annonces de limitation / dépassement.
    // Alertes radars : coupées par défaut. Elles ne s'allument que si l'utilisateur a coché lui-même la case
    // d'activation de l'avertissement légal (version en cours) : sans cet accord, impossible de les activer.
    var radarsOn by mutableStateOf(prefs.getBoolean("radars_on", false) && fr.roadguard.core.ui.Legal.radarConsented(appContext)); private set
    fun changeRadarsOn(v: Boolean) {
        val on = v && fr.roadguard.core.ui.Legal.radarConsented(appContext)
        radarsOn = on; prefs.edit().putBoolean("radars_on", on).apply()
    }
    /** Alertes radars changées ailleurs (avertissement légal, autre écran) : la séance de conduite suit tout de suite. */
    private val radarsListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
        if (key == "radars_on") radarsOn = p.getBoolean("radars_on", false) && fr.roadguard.core.ui.Legal.radarConsented(appContext)
        if (key == "swiss_mode") swissMode = p.getBoolean("swiss_mode", true) || !fr.roadguard.core.ui.Legal.swissConsented(appContext)
    }
    init { prefs.registerOnSharedPreferenceChangeListener(radarsListener) }
    val limitVoiceOn get() = announceLimit || speedLimitWarning
    fun changeLimitVoice(v: Boolean) { changeAnnounceLimit(v); changeSpeedLimitWarning(v) }

    // Pilote (téléphone) : démarrage automatique en roulant, dès 20 km/h, avec la bulle par-dessus Waze.
    var autoDrive by mutableStateOf(prefs.getBoolean("auto_drive", false)); private set
    fun changeAutoDrive(v: Boolean) { autoDrive = v; prefs.edit().putBoolean("auto_drive", v).apply() }

    // Conducteur ou conductrice : phrases de la voix au masculin ou au féminin.
    var driverFemale by mutableStateOf(prefs.getBoolean("driver_female", false)); private set
    fun changeDriverFemale(v: Boolean) { driverFemale = v; prefs.edit().putBoolean("driver_female", v).apply() }
    // Ton de la voix : 0 classique, 1 charme (complice), 2 séduction (aguicheuse).
    var voiceTone by mutableStateOf(prefs.getInt("voice_tone", if (prefs.getBoolean("charm_voice", false)) 1 else 0)); private set
    fun changeVoiceTone(v: Int) { voiceTone = v; prefs.edit().putInt("voice_tone", v).apply() }

    // Voix réaliste (phrases enregistrées « Jessica ») pour les radars et limitations.
    var realVoice by mutableStateOf(prefs.getBoolean("real_voice", true)); private set
    fun changeRealVoice(v: Boolean) { realVoice = v; prefs.edit().putBoolean("real_voice", v).apply() }

    var announceLimit by mutableStateOf(prefs.getBoolean("announce_limit", true)); private set
    fun changeAnnounceLimit(v: Boolean) { announceLimit = v; prefs.edit().putBoolean("announce_limit", v).apply() }

    fun changeAutostart(v: Boolean) { autostart = v; prefs.edit().putBoolean("autostart", v).apply() }
    fun changeAccent(v: Long) {
        accent = v; prefs.edit().putLong("accent", v).apply()
        fr.roadguard.core.ui.DriveColors.Accent = androidx.compose.ui.graphics.Color(v)
    }
    fun changeScale(v: Float) {
        scale = v; prefs.edit().putFloat("scale", v).apply()
        fr.roadguard.core.ui.DriveColors.Scale = v
    }

    fun changeVoice(v: Boolean) { voice = v; prefs.edit().putBoolean("voice", v).apply() }
    fun changeReadMessages(v: Boolean) { readMessages = v; prefs.edit().putBoolean("read_messages", v).apply() }
    fun changeBeep(v: Boolean) { beep = v; prefs.edit().putBoolean("beep", v).apply() }
    fun changeSpeedLimitWarning(v: Boolean) { speedLimitWarning = v; prefs.edit().putBoolean("speed_limit_warning", v).apply() }
    fun changeTheme(v: ThemeMode) { theme = v; prefs.edit().putString("theme", v.name).apply() }
    fun toggleKind(kind: RadarKind) {
        alertKinds = if (kind in alertKinds) alertKinds - kind else alertKinds + kind
        prefs.edit().putStringSet("alert_kinds", alertKinds.map { it.name }.toSet()).apply()
    }


    // ---------- Sécurité et pratique (lot du 06/11) ----------
    private fun put(key: String, v: Any) = prefs.edit().apply {
        when (v) { is Boolean -> putBoolean(key, v); is Int -> putInt(key, v); is String -> putString(key, v); is Float -> putFloat(key, v) }
    }.apply()

    /** Suisse : annonces de radars interdites, coupées automatiquement dans le pays. */
    // Suisse : radars coupés automatiquement ; ne peut être désactivé qu'en cochant la case dédiée (accord enregistré).
    var swissMode by mutableStateOf(prefs.getBoolean("swiss_mode", true) || !fr.roadguard.core.ui.Legal.swissConsented(appContext)); private set
    fun changeSwissMode(v: Boolean) {
        val on = v || !fr.roadguard.core.ui.Legal.swissConsented(appContext)
        swissMode = on; put("swiss_mode", on)
    }
    /** France : limitations abaissées par temps de pluie (130→110, 110→100, 90→80). */
    var rainLimits by mutableStateOf(prefs.getBoolean("rain_limits", true)); private set
    fun changeRainLimits(v: Boolean) { rainLimits = v; put("rain_limits", v) }
    /** Profil : 0 normal, 1 jeune conducteur (France : 110 / 100 / 80). */
    var driverProfile by mutableStateOf(prefs.getInt("driver_profile", 0)); private set
    fun changeDriverProfile(v: Int) { driverProfile = v; put("driver_profile", v) }
    /** Vitesse maximale personnelle (remorque, caravane…), 0 = aucune. */
    var personalMax by mutableStateOf(prefs.getInt("personal_max", 0)); private set
    fun changePersonalMax(v: Int) { personalMax = v; put("personal_max", v) }
    /** Annonces météo dangereuse (verglas, brouillard, fortes pluies, neige). */
    var weatherAlerts by mutableStateOf(prefs.getBoolean("weather_alerts", true)); private set
    fun changeWeatherAlerts(v: Boolean) { weatherAlerts = v; put("weather_alerts", v) }
    /** Virages dangereux, passages à niveau, écoles (OpenStreetMap). */
    var hazardAlerts by mutableStateOf(prefs.getBoolean("hazard_alerts", true)); private set
    fun changeHazardAlerts(v: Boolean) { hazardAlerts = v; put("hazard_alerts", v) }
    /** Rappel « rien oublié à l'arrière ? » en fin de trajet. */
    var forgetReminder by mutableStateOf(prefs.getBoolean("forget_reminder", false)); private set
    fun changeForgetReminder(v: Boolean) { forgetReminder = v; put("forget_reminder", v) }
    /** Détection d'accident et contact d'urgence prévenu par SMS. */
    var crashDetect by mutableStateOf(prefs.getBoolean("crash_detect", false)); private set
    fun changeCrashDetect(v: Boolean) { crashDetect = v; put("crash_detect", v) }
    var emergencyName by mutableStateOf(prefs.getString("emergency_name", "") ?: ""); private set
    var emergencyPhone by mutableStateOf(prefs.getString("emergency_phone", "") ?: ""); private set
    fun changeEmergency(name: String, phone: String) { emergencyName = name; emergencyPhone = phone; put("emergency_name", name); put("emergency_phone", phone) }
    /** Bluetooth de la voiture : Pilote démarre à la connexion (adresse de l'appareil, vide = non). */
    var carBluetooth by mutableStateOf(prefs.getString("car_bt", "") ?: ""); private set
    var carBluetoothName by mutableStateOf(prefs.getString("car_bt_name", "") ?: ""); private set
    fun changeCarBluetooth(address: String, name: String) { carBluetooth = address; carBluetoothName = name; put("car_bt", address); put("car_bt_name", name) }
    /** Dashcam : enregistre en boucle avec la caméra arrière et garde la vidéo en cas de choc. */
    var dashcam by mutableStateOf(prefs.getBoolean("dashcam", false)); private set
    fun changeDashcam(v: Boolean) { dashcam = v; put("dashcam", v) }
    /** Coupe automatiquement les messages, à part l'essentiel, en mode moto (gros boutons, casque). */
    var motoMode by mutableStateOf(prefs.getBoolean("moto_mode", false)); private set
    fun changeMotoMode(v: Boolean) { motoMode = v; put("moto_mode", v) }
    /** Écran tête haute : affichage en miroir pour le pare-brise. */
    var hudMirror by mutableStateOf(prefs.getBoolean("hud_mirror", true)); private set
    fun changeHudMirror(v: Boolean) { hudMirror = v; put("hud_mirror", v) }
    /** Boutons du volant : appui long sur « suivant » = signaler un radar mobile. */
    var wheelReport by mutableStateOf(prefs.getBoolean("wheel_report", false)); private set
    fun changeWheelReport(v: Boolean) { wheelReport = v; put("wheel_report", v) }
    /** « Police toujours là ? » posé à la voix, réponse « oui / non » à la voix. */
    var voiceConfirm by mutableStateOf(prefs.getBoolean("voice_confirm", true)); private set
    fun changeVoiceConfirm(v: Boolean) { voiceConfirm = v; put("voice_confirm", v) }
    /** Notification vibrante à chaque radar (relayée sur la montre connectée). */
    var watchAlerts by mutableStateOf(prefs.getBoolean("watch_alerts", false)); private set
    fun changeWatchAlerts(v: Boolean) { watchAlerts = v; put("watch_alerts", v) }
    /** Lecture des notifications de messages (SMS, WhatsApp…) à voix haute. */
    var speakNotifications by mutableStateOf(prefs.getBoolean("speak_notifications", false)); private set
    fun changeSpeakNotifications(v: Boolean) { speakNotifications = v; put("speak_notifications", v) }
    /** Rappel d'entretien : kilomètres de la prochaine vidange (0 = aucun). */
    var serviceAtKm by mutableStateOf(prefs.getInt("service_at_km", 0)); private set
    fun changeServiceAtKm(v: Int) { serviceAtKm = v; put("service_at_km", v) }
    /** Rendez-vous de l'agenda : propose l'itinéraire et l'heure de départ. */
    var calendarHints by mutableStateOf(prefs.getBoolean("calendar_hints", false)); private set
    fun changeCalendarHints(v: Boolean) { calendarHints = v; put("calendar_hints", v) }
    /** Classement des trajets par défaut : true = pro. */
    var defaultTripPro by mutableStateOf(prefs.getBoolean("default_trip_pro", false)); private set
    fun changeDefaultTripPro(v: Boolean) { defaultTripPro = v; put("default_trip_pro", v) }

    /** Itinéraire : éviter routes étroites, chemins et centres. */
    var routeNarrow by mutableStateOf(prefs.getBoolean("route_narrow", false)); private set
    fun changeRouteNarrow(v: Boolean) { routeNarrow = v; put("route_narrow", v) }
    /** Itinéraire le plus économe en carburant. */
    var routeEco by mutableStateOf(prefs.getBoolean("route_eco", false)); private set
    fun changeRouteEco(v: Boolean) { routeEco = v; put("route_eco", v) }
    /** Étapes : dans l'ordre le plus rapide (sinon dans l'ordre choisi). */
    var optimizeStops by mutableStateOf(prefs.getBoolean("optimize_stops", true)); private set
    fun changeOptimizeStops(v: Boolean) { optimizeStops = v; put("optimize_stops", v) }
    /** Langue des annonces : fr, en, de, it (le français garde les tons). */
    var voiceLang by mutableStateOf(prefs.getString("voice_lang", "fr") ?: "fr"); private set
    fun changeVoiceLang(v: String) { voiceLang = v; put("voice_lang", v) }
    /** Économie de batterie : auto (batterie faible), toujours, jamais. 0 auto, 1 toujours, 2 jamais. */
    var batterySaver by mutableStateOf(prefs.getInt("battery_saver", 0)); private set
    fun changeBatterySaver(v: Int) { batterySaver = v; put("battery_saver", v) }
    /** Rappels administratifs : date (ms) ou 0. */
    fun reminderDate(key: String): Long = prefs.getLong("rem_$key", 0L)
    fun setReminderDate(key: String, at: Long) { prefs.edit().putLong("rem_$key", at).apply(); reminderTick++ }
    var reminderTick by mutableStateOf(0); private set

    /** Tous les réglages (sauvegarde dans le compte), avec leur type pour une restauration exacte. */
    fun exportAll(): org.json.JSONObject = org.json.JSONObject().apply {
        prefs.all.forEach { (k, v) ->
            val t = when (v) { is Boolean -> "b"; is Int -> "i"; is Long -> "l"; is Float -> "f"; is String -> "s"; is Set<*> -> "S"; else -> null } ?: return@forEach
            put(k, org.json.JSONObject().put("t", t).put("v", if (v is Set<*>) org.json.JSONArray(v.toList()) else v))
        }
    }
    /** Restaure les réglages sauvegardés (pris en compte au prochain démarrage de l'appli). */
    fun importAll(o: org.json.JSONObject) {
        val e = prefs.edit()
        o.keys().forEach { k ->
            val x = o.optJSONObject(k) ?: return@forEach
            runCatching {
                when (x.getString("t")) {
                    "b" -> e.putBoolean(k, x.getBoolean("v")); "i" -> e.putInt(k, x.getInt("v")); "l" -> e.putLong(k, x.getLong("v"))
                    "f" -> e.putFloat(k, x.getDouble("v").toFloat()); "s" -> e.putString(k, x.getString("v"))
                    "S" -> x.getJSONArray("v").let { a -> e.putStringSet(k, (0 until a.length()).map { a.getString(it) }.toSet()) }
                }
            }
        }
        e.apply()
    }

    fun isNight(autoNight: Boolean): Boolean = when (theme) {
        ThemeMode.AUTO -> autoNight
        ThemeMode.DAY -> false
        ThemeMode.NIGHT -> true
    }
}
