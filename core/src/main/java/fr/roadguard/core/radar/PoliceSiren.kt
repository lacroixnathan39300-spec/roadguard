package fr.roadguard.core.radar

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin

/**
 * Sirène « deux-tons » de la police française (« pin-pon » : la 435 Hz puis ré 580 Hz), fabriquée par l'appli
 * (aucun fichier son).
 * - [startAlarm] : en boucle jusqu'à [stop] (conducteur qui s'endort ; flux « alarme », audible même musique coupée).
 * - [burst] : quelques « pin-pon » au dernier moment avant un radar, sur le flux du guidage.
 */
object PoliceSiren {
    private const val RATE = 44_100
    /** Durée d'une note : un « pin » puis un « pon » font un cycle d'environ 0,85 s. */
    private const val NOTE_S = 0.42

    private val cycle: ShortArray by lazy { build() }
    private var track: AudioTrack? = null
    private val main = android.os.Handler(android.os.Looper.getMainLooper())

    private fun build(): ShortArray {
        val n = (RATE * NOTE_S).toInt()
        val fade = RATE / 125 // 8 ms d'attaque et de fin : pas de claquement entre les notes
        val out = ShortArray(n * 2)
        for ((k, f) in listOf(435.0, 580.0).withIndex()) {
            for (i in 0 until n) {
                val w = 2 * PI * f * i / RATE
                // Fondamentale et harmoniques : timbre de trompe, plus proche d'une vraie sirène qu'un son pur.
                val v = 0.62 * sin(w) + 0.25 * sin(2 * w) + 0.13 * sin(3 * w)
                val env = minOf(1.0, i.toDouble() / fade, (n - 1 - i).toDouble() / fade)
                out[k * n + i] = (v * env * 0.9 * Short.MAX_VALUE).toInt().toShort()
            }
        }
        return out
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
