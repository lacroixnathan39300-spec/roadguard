package fr.roadguard.core.radar

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Annonce sonore : bip puis phrase en français. Si le poste n'a pas de voix française,
 * seul le bip est joué.
 */
class AlertVoice(context: Context) : TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(context.applicationContext, this)
    private var ttsReady = false
    /** Réglages : voix et bip (modifiables à tout moment). */
    var voiceEnabled = true
    var beepEnabled = true
    /** Bips de proximité façon détecteur de métaux (sinon : la distance est dite à voix haute). */
    var proximityBeeps = true
    private val tone = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 90) }.getOrNull()
    private val beeper = android.os.Handler(android.os.Looper.getMainLooper())
    @Volatile private var proximityM: Int? = null
    private var beeping = false

    /**
     * Distance au radar annoncé (null quand il n'y en a plus) : les bips s'accélèrent en approchant,
     * jusqu'à devenir continus dans les derniers mètres.
     */
    fun proximity(distanceM: Int?) {
        proximityM = if (beepEnabled && proximityBeeps) distanceM else null
        if (proximityM != null && !beeping) { beeping = true; beeper.post(beepTick) }
    }

    private val beepTick = object : Runnable {
        override fun run() {
            val d = proximityM
            if (d == null) { beeping = false; return }
            val interval = BeepCadence.intervalMs(d)
            tone?.startTone(ToneGenerator.TONE_PROP_BEEP, BeepCadence.toneMs(d))
            beeper.postDelayed(this, interval)
        }
    }

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) return
        val result = tts.setLanguage(Locale.FRANCE)
        ttsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
    }

    fun announce(alert: RadarAlert) {
        if (beepEnabled) tone?.startTone(ToneGenerator.TONE_PROP_BEEP2, 300)
        val text = if (proximityBeeps) AlertText.spokenShort(alert) else AlertText.spoken(alert)
        if (ttsReady && voiceEnabled) tts.speak(text, TextToSpeech.QUEUE_ADD, null, "rg-${alert.radar.id}")
    }

    fun announceAverageResult(status: AverageStatus) {
        if (status.over && beepEnabled) tone?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 400)
        if (ttsReady && voiceEnabled) tts.speak(AlertText.averageSpoken(status), TextToSpeech.QUEUE_ADD, null, "rg-average")
    }

    /** Consigne de guidage ou message libre, à la suite des annonces en cours. */
    fun speak(text: String) {
        if (ttsReady && voiceEnabled && text.isNotBlank()) tts.speak(text, TextToSpeech.QUEUE_ADD, null, "rg-nav-${text.hashCode()}")
    }

    fun release() {
        proximityM = null
        beeper.removeCallbacksAndMessages(null)
        tts.shutdown()
        tone?.release()
    }
}

/** Textes des alertes (purs, testables). */
object AlertText {
    fun kindLabel(kind: RadarKind): String = when (kind) {
        RadarKind.FIXED -> "Radar fixe"
        RadarKind.REDLIGHT -> "Radar feu rouge"
        RadarKind.AVERAGE -> "Vitesse moyenne"
        RadarKind.SECTION -> "Tronçon contrôlé"
        RadarKind.MOBILE -> "Radar mobile signalé"
        RadarKind.OTHER -> "Contrôle"
    }

    /** Distance arrondie à 50 m pour l'affichage et la voix. */
    fun roundedDistance(meters: Int): Int = ((meters + 25) / 50) * 50

    fun banner(alert: RadarAlert): String {
        val limit = alert.radar.speedKmh?.let { " · $it km/h" }.orEmpty()
        return "${kindLabel(alert.radar.kind)}$limit · ${roundedDistance(alert.distanceM)} m"
    }

    /** Annonce sans distance (les bips de proximité indiquent l'approche). */
    fun spokenShort(alert: RadarAlert): String {
        val limit = alert.radar.speedKmh?.let { ", limité à $it" }.orEmpty()
        return "${kindLabel(alert.radar.kind)}$limit"
    }

    fun spoken(alert: RadarAlert): String {
        val limit = alert.radar.speedKmh?.let { ", limité à $it" }.orEmpty()
        return "${kindLabel(alert.radar.kind)} dans ${roundedDistance(alert.distanceM)} mètres$limit"
    }

    fun average(status: AverageStatus): String {
        val limit = status.limitKmh?.let { "/$it" }.orEmpty()
        val verdict = when {
            status.limitKmh == null -> ""
            status.over -> " · trop vite"
            else -> " · OK"
        }
        val prefix = if (status.finished) "Tronçon terminé · moyenne " else "Moyenne "
        return "$prefix${status.averageKmh}$limit km/h$verdict"
    }

    fun averageSpoken(status: AverageStatus): String {
        val verdict = when {
            status.limitKmh == null -> ""
            status.over -> ", au-dessus de la limite"
            else -> ", c'est bon"
        }
        return "Fin du tronçon, moyenne ${status.averageKmh} kilomètres heure$verdict"
    }
}

/** Rythme des bips de proximité (pur, testable). */
object BeepCadence {
    /** Délai entre deux bips : ~1,5 s à 500 m, de plus en plus court, continu sous 40 m. */
    fun intervalMs(distanceM: Int): Long = when {
        distanceM <= 40 -> 160
        distanceM >= 500 -> 1500
        else -> (160 + (distanceM - 40) * (1500 - 160) / (500 - 40)).toLong()
    }

    /** Durée du bip : il remplit tout l'intervalle quand on est sur le radar (son continu). */
    fun toneMs(distanceM: Int): Int = if (distanceM <= 40) 170 else 90
}
