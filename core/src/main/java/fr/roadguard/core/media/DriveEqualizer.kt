package fr.roadguard.core.media

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.os.Build
import org.json.JSONArray

data class EqBand(val centerHz: Int, val levelMb: Int)

/** Préréglage : gain en dB pour chacune des 10 bandes. */
data class EqPreset(val name: String, val gains: List<Float>)

/**
 * Égaliseur « pro » appliqué à tout le son du poste ou du téléphone (session audio globale) :
 * - 10 bandes de 31 Hz à 16 kHz, ±15 dB (DynamicsProcessing, Android 9+) ; sinon les bandes de l'égaliseur
 *   Android de l'appareil (souvent 5), réglées d'après les 10 bandes ;
 * - boost de volume jusqu'à +12 dB (LoudnessEnhancer), renfort des basses, spatialisation ;
 * - préréglages (Route, Basses+, Rock, Pop, Voix…) et 3 réglages personnels enregistrés.
 * Tout est mémorisé et réappliqué au démarrage.
 */
class DriveEqualizer(context: Context) {
    private val prefs = context.getSharedPreferences("equalizer", Context.MODE_PRIVATE)

    val frequencies = listOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)
    val minDb = -15f
    val maxDb = 15f

    private val dp: DynamicsProcessing? = if (Build.VERSION.SDK_INT >= 28) runCatching {
        val cfg = DynamicsProcessing.Config.Builder(DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION, 2,
            true, frequencies.size, false, 0, false, 0, true).build()
        DynamicsProcessing(0, 0, cfg)
    }.getOrNull() else null
    private val eq: Equalizer? = if (dp == null) runCatching { Equalizer(0, 0) }.getOrNull() else null
    private val loud: LoudnessEnhancer? = runCatching { LoudnessEnhancer(0) }.getOrNull()
    private val bass: BassBoost? = runCatching { BassBoost(0, 0) }.getOrNull()
    private val virt: Virtualizer? = runCatching { Virtualizer(0, 0) }.getOrNull()

    val available: Boolean get() = dp != null || eq != null
    val engine: String get() = when {
        dp != null -> "10 bandes (traitement avancé Android)"
        eq != null -> "${eq.numberOfBands} bandes (égaliseur de l'appareil), réglées d'après les 10 bandes"
        else -> "non disponible"
    }
    val boostAvailable get() = loud != null
    val bassAvailable get() = bass?.strengthSupported == true
    val spatialAvailable get() = virt?.strengthSupported == true

    var gains: List<Float> = load("gains") ?: List(10) { 0f }; private set
    var boostDb: Float = prefs.getFloat("boost", 0f); private set
    var bassStrength: Int = prefs.getInt("bass", 0); private set
    var spatial: Int = prefs.getInt("spatial", 0); private set
    var presetName: String = prefs.getString("preset", "Plat") ?: "Plat"; private set

    var enabled: Boolean
        get() = prefs.getBoolean("enabled", false)
        set(v) { prefs.edit().putBoolean("enabled", v).apply(); applyAll() }

    init { applyAll() }

    val presets = listOf(
        EqPreset("Plat", List(10) { 0f }),
        EqPreset("Route", listOf(4f, 3.5f, 2.5f, 0.5f, -0.5f, 0f, 1.5f, 3f, 3.5f, 3f)),
        EqPreset("Basses+", listOf(8f, 7f, 5.5f, 3f, 0f, -0.5f, 0f, 0.5f, 1f, 1f)),
        EqPreset("Rock", listOf(5f, 4f, 2f, -1f, -2f, -0.5f, 2f, 3.5f, 4.5f, 4.5f)),
        EqPreset("Pop", listOf(-1f, 1f, 3f, 4f, 3f, 0f, -1f, -1f, 0.5f, 1.5f)),
        EqPreset("Électro", listOf(6f, 5f, 2f, 0f, -2f, 1f, 0.5f, 2f, 4f, 5f)),
        EqPreset("Rap / Hip-hop", listOf(7f, 6f, 3.5f, 1.5f, -1f, -1f, 1f, 0.5f, 1.5f, 2.5f)),
        EqPreset("Voix / Podcast", listOf(-3f, -2f, -1f, 1.5f, 3.5f, 4f, 3.5f, 2f, 0f, -1.5f)),
        EqPreset("Jazz", listOf(3f, 2f, 1f, 2f, -1.5f, -1.5f, 0f, 1.5f, 3f, 3.5f)),
        EqPreset("Classique", listOf(4f, 3f, 2f, 1f, -1f, -1f, 0f, 2f, 3f, 4f)),
        EqPreset("Aigus+", listOf(-1f, -1f, 0f, 0f, 0.5f, 1.5f, 3f, 5f, 6.5f, 7f)),
    )

    fun custom(slot: Int): EqPreset? = load("custom$slot")?.let { EqPreset("Perso $slot", it) }

    fun setGain(index: Int, db: Float) {
        gains = gains.toMutableList().also { it[index] = db.coerceIn(minDb, maxDb) }
        presetName = "Personnalisé"
        save(); applyBands()
    }

    fun applyPreset(p: EqPreset) { gains = p.gains; presetName = p.name; save(); if (!enabled) enabled = true else applyBands() }

    fun saveCustom(slot: Int) {
        prefs.edit().putString("custom$slot", JSONArray(gains.map { it.toDouble() }).toString()).apply()
        presetName = "Perso $slot"; save()
    }

    fun setBoost(db: Float) { boostDb = db.coerceIn(0f, 12f); save(); applyExtras() }
    fun setBass(v: Int) { bassStrength = v.coerceIn(0, 1000); save(); applyExtras() }
    fun setSpatial(v: Int) { spatial = v.coerceIn(0, 1000); save(); applyExtras() }

    fun reset() { gains = List(10) { 0f }; boostDb = 0f; bassStrength = 0; spatial = 0; presetName = "Plat"; save(); applyAll() }

    /** Pour l'ancienne interface : bandes de l'appareil (Hz, millibels). */
    fun bands(): List<EqBand> = frequencies.mapIndexed { i, f -> EqBand(f, (gains[i] * 100).toInt()) }

    private fun applyAll() { applyBands(); applyExtras() }

    private fun applyBands() {
        val on = enabled
        runCatching {
            dp?.let { d ->
                d.enabled = on
                frequencies.forEachIndexed { i, f ->
                    // Bande i : de la moitié de l'écart avec la précédente à la moitié avec la suivante.
                    val cutoff = if (i == frequencies.lastIndex) 20000f else ((f + frequencies[i + 1]) / 2f)
                    val band = DynamicsProcessing.EqBand(true, cutoff, if (on) gains[i] else 0f)
                    d.setPreEqBandAllChannelsTo(i, band)
                }
            }
        }
        runCatching {
            eq?.let { e ->
                e.enabled = on
                val range = e.bandLevelRange
                for (b in 0 until e.numberOfBands) {
                    val hz = e.getCenterFreq(b.toShort()) / 1000.0
                    val db = interpolate(hz)
                    e.setBandLevel(b.toShort(), (db * 100).toInt().coerceIn(range[0].toInt(), range[1].toInt()).toShort())
                }
            }
        }
    }

    private fun applyExtras() {
        val on = enabled
        runCatching { loud?.setTargetGain(if (on) (boostDb * 100).toInt() else 0); loud?.enabled = on && boostDb > 0 }
        runCatching { if (bassAvailable) { bass?.setStrength(bassStrength.toShort()); bass?.enabled = on && bassStrength > 0 } }
        runCatching { if (spatialAvailable) { virt?.setStrength(spatial.toShort()); virt?.enabled = on && spatial > 0 } }
    }

    /** Gain (dB) à une fréquence quelconque, interpolé entre les 10 bandes (échelle logarithmique). */
    private fun interpolate(hz: Double): Float {
        if (hz <= frequencies.first()) return gains.first()
        if (hz >= frequencies.last()) return gains.last()
        val i = frequencies.indexOfLast { it <= hz }
        val a = Math.log(frequencies[i].toDouble()); val b = Math.log(frequencies[i + 1].toDouble())
        val t = ((Math.log(hz) - a) / (b - a)).toFloat()
        return gains[i] + (gains[i + 1] - gains[i]) * t
    }

    private fun load(key: String): List<Float>? = prefs.getString(key, null)?.let { raw ->
        runCatching { val a = JSONArray(raw); List(a.length()) { a.getDouble(it).toFloat() } }.getOrNull()?.takeIf { it.size == 10 }
    }

    private fun save() {
        prefs.edit().putString("gains", JSONArray(gains.map { it.toDouble() }).toString()).putFloat("boost", boostDb)
            .putInt("bass", bassStrength).putInt("spatial", spatial).putString("preset", presetName).apply()
    }

    fun release() { runCatching { dp?.release() }; runCatching { eq?.release() }; runCatching { loud?.release() }; runCatching { bass?.release() }; runCatching { virt?.release() } }
}

/** « 31 Hz », « 1 kHz », « 16 kHz ». */
fun frequencyLabel(hz: Int): String = when {
    hz >= 1000 -> {
        val k = hz / 100 / 10.0
        if (k == k.toInt().toDouble()) "${k.toInt()} kHz" else "$k kHz"
    }
    else -> "$hz Hz"
}
