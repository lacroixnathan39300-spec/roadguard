package fr.roadguard.drive

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fr.roadguard.drive.radar.RadarKind

enum class ThemeMode(val label: String) { AUTO("Automatique"), DAY("Jour"), NIGHT("Nuit") }

/** Réglages de DRIVE, mémorisés sur le poste et observables par l'interface. */
class DriveSettings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var voice by mutableStateOf(prefs.getBoolean("voice", true)); private set
    var beep by mutableStateOf(prefs.getBoolean("beep", true)); private set
    var speedLimitWarning by mutableStateOf(prefs.getBoolean("speed_limit_warning", true)); private set
    var theme by mutableStateOf(runCatching { ThemeMode.valueOf(prefs.getString("theme", "AUTO")!!) }.getOrDefault(ThemeMode.AUTO)); private set
    var alertKinds by mutableStateOf(
        prefs.getStringSet("alert_kinds", null)?.mapNotNull { runCatching { RadarKind.valueOf(it) }.getOrNull() }?.toSet()
            ?: setOf(RadarKind.FIXED, RadarKind.REDLIGHT, RadarKind.AVERAGE, RadarKind.SECTION, RadarKind.MOBILE)
    ); private set

    fun changeVoice(v: Boolean) { voice = v; prefs.edit().putBoolean("voice", v).apply() }
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
