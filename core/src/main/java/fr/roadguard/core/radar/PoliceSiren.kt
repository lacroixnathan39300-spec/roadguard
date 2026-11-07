package fr.roadguard.core.radar

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tanh

/**
 * Sirène « deux-tons » de la police française (« pin-pon » : la 435 Hz puis ré 580 Hz), fabriquée par l'appli
 * (aucun fichier son).
 * - [startAlarm] : en boucle jusqu'à [stop] (conducteur qui s'endort ; flux « alarme », audible même musique coupée).
 * - [burst] : quelques « pin-pon » au dernier moment avant un radar, sur le flux du guidage.
 */
object PoliceSiren {
    private const val RATE = 44_100
    /** Durée d'une note : un « pin » puis un « pon » font un cycle d'environ 0,9 s. */
    private const val NOTE_S = 0.46
    /** La trompe « monte » vers sa note au départ (l'air arrive). */
    private const val GLIDE_S = 0.07

    private val cycle: ShortArray by lazy { build() }
    private var track: AudioTrack? = null
    private val main = android.os.Handler(android.os.Looper.getMainLooper())

    /** Prépare le son à l'avance (en arrière-plan) pour qu'il parte sans délai la première fois. */
    fun prepare() { Thread { runCatching { cycle } }.start() }

    /**
     * Une note de trompe à air : son cuivré (harmoniques renforcées vers 1 à 3 kHz), attaque un peu basse qui monte,
     * léger vibrato, deux trompes légèrement désaccordées, saturation douce.
     */
    private fun hornNote(f0: Double, n: Int): DoubleArray {
        val weights = DoubleArray((6_500 / f0).toInt()) { j ->
            val k = j + 1
            val x = (k * f0 - 1_700) / 1_100
            (1.0 / k.toDouble().pow(0.85)) * (1.0 + 1.8 * exp(-x * x))
        }
        val norm = weights.sum()
        val attack = 0.022 * RATE; val release = 0.018 * RATE
        val out = DoubleArray(n)
        var p1 = 0.0; var p2 = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / RATE
            val f = f0 * (1.0 - 0.045 * exp(-t / (GLIDE_S / 3))) * (1.0 + 0.0025 * sin(2 * PI * 5.3 * t))
            p1 += 2 * PI * f / RATE
            p2 += 2 * PI * f * 1.007 / RATE
            var s1 = 0.0; var s2 = 0.0
            for (j in weights.indices) { val k = j + 1; s1 += weights[j] * sin(k * p1); s2 += weights[j] * sin(k * p2) }
            val env = minOf(1.0, i / attack, (n - 1 - i) / release) * (1.0 + 0.08 * exp(-i / (0.05 * RATE)))
            out[i] = tanh((s1 + 0.8 * s2) / norm * env * 1.9)
        }
        return out
    }

    private fun build(): ShortArray {
        val n = (RATE * NOTE_S).toInt()
        val dry = hornNote(435.0, n) + hornNote(580.0, n)
        // Écho de la rue (réflexions sur les façades), en boucle : la sirène tourne sans coupure.
        val wet = dry.copyOf()
        for ((d, g) in listOf(0.031 to 0.22, 0.057 to 0.15, 0.089 to 0.10, 0.131 to 0.06)) {
            val k = (d * RATE).toInt()
            for (i in dry.indices) wet[(i + k) % dry.size] += g * dry[i]
        }
        val peak = wet.maxOf { abs(it) }.coerceAtLeast(1e-9)
        return ShortArray(wet.size) { (wet[it] / peak * 0.92 * Short.MAX_VALUE).toInt().toShort() }
    }

    /** Sirène en cours (alarme ou dernier moment avant un radar). */
    val playing: Boolean
        @Synchronized get() = track != null

    @Synchronized
    fun stop() {
        track?.let { runCatching { it.stop() }; runCatching { it.release() } }
        track = null
    }

    /** En boucle jusqu'à [stop] : réveil du conducteur. */
    fun startAlarm() = play(-1, AudioAttributes.USAGE_ALARM)

    /** [cycles] « pin-pon » puis arrêt : dernier moment avant un radar. */
    fun burst(cycles: Int = 3) = play(cycles - 1, AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)

    @Synchronized
    private fun play(loops: Int, usage: Int) {
        stop()
        val data = cycle
        val t = runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(usage).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(data.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
        }.getOrNull() ?: return
        val ok = runCatching {
            t.write(data, 0, data.size)
            if (loops != 0) t.setLoopPoints(0, data.size, loops)
            t.play()
        }.isSuccess
        if (!ok) { runCatching { t.release() }; return }
        track = t
        if (loops >= 0) {
            // Fin du nombre de « pin-pon » demandé : on libère le lecteur.
            val ms = (NOTE_S * 2 * 1000 * (loops + 1)).toLong() + 300
            main.postDelayed({ synchronized(this) { if (track === t) stop() } }, ms)
        }
    }
}
