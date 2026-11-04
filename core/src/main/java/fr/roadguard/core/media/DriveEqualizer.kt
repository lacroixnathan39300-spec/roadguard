package fr.roadguard.core.media

import android.content.Context
import android.media.audiofx.Equalizer

/** Une bande de l'égaliseur : fréquence centrale (Hz) et niveau (millibels). */
data class EqBand(val centerHz: Int, val levelMb: Int)

/**
 * Égaliseur appliqué à toute la sortie audio du poste (session globale 0).
 * Selon le poste, le constructeur peut le refuser : [available] vaut alors false.
 * Les réglages sont mémorisés et réappliqués au démarrage.
 */
class DriveEqualizer(context: Context) {
    private val prefs = context.getSharedPreferences("equalizer", Context.MODE_PRIVATE)
    private val eq: Equalizer? = runCatching { Equalizer(0, 0) }.getOrNull()

    val available: Boolean get() = eq != null
    val minMb: Int = eq?.bandLevelRange?.get(0)?.toInt() ?: -1500
    val maxMb: Int = eq?.bandLevelRange?.get(1)?.toInt() ?: 1500

    init {
        eq?.let { e ->
            runCatching {
                for (b in 0 until e.numberOfBands) {
                    val saved = prefs.getInt("band_$b", Int.MIN_VALUE)
                    if (saved != Int.MIN_VALUE) e.setBandLevel(b.toShort(), saved.coerceIn(minMb, maxMb).toShort())
                }
                e.enabled = prefs.getBoolean("enabled", true)
            }
        }
    }

    var enabled: Boolean
        get() = eq?.enabled == true
        set(value) {
            runCatching { eq?.enabled = value }
            prefs.edit().putBoolean("enabled", value).apply()
        }

    fun bands(): List<EqBand> {
        val e = eq ?: return emptyList()
        return runCatching {
            (0 until e.numberOfBands).map { b ->
                EqBand(e.getCenterFreq(b.toShort()) / 1000, e.getBandLevel(b.toShort()).toInt())
            }
        }.getOrDefault(emptyList())
    }

    fun setBand(index: Int, levelMb: Int) {
        val e = eq ?: return
        val level = levelMb.coerceIn(minMb, maxMb)
        runCatching { e.setBandLevel(index.toShort(), level.toShort()) }
        prefs.edit().putInt("band_$index", level).apply()
    }

    fun reset() { bands().indices.forEach { setBand(it, 0) } }

    fun release() { runCatching { eq?.release() } }
}

/** Libellé court d'une fréquence : 60 Hz, 230 Hz, 3,6 kHz… */
fun frequencyLabel(hz: Int): String = when {
    hz >= 1000 -> {
        val k = hz / 100 / 10.0
        (if (k % 1.0 == 0.0) k.toInt().toString() else k.toString().replace('.', ',')) + " kHz"
    }
    else -> "$hz Hz"
}
