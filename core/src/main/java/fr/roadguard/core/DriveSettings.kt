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
