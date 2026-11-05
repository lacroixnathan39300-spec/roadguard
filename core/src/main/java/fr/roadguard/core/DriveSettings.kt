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

    /** DRIVE : gestes sur les bords de l'écran (retour, applis, volume). */
    var edgeGestures by mutableStateOf(prefs.getBoolean("edge_gestures", true)); private set
    fun changeEdgeGestures(v: Boolean) { edgeGestures = v; prefs.edit().putBoolean("edge_gestures", v).apply() }

    /** Marge avant l'alerte de dépassement : -1 = tolérance légale (5 km/h, 5 % au-delà de 100), sinon en km/h. */
    var overTolerance by mutableStateOf(prefs.getInt("over_tolerance", -1)); private set
    fun changeOverTolerance(v: Int) { overTolerance = v; prefs.edit().putInt("over_tolerance", v).apply() }

    /** Annonce vocale quand la limitation change (« Limitation 50 »). */
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

    fun isNight(autoNight: Boolean): Boolean = when (theme) {
        ThemeMode.AUTO -> autoNight
        ThemeMode.DAY -> false
        ThemeMode.NIGHT -> true
    }
}
