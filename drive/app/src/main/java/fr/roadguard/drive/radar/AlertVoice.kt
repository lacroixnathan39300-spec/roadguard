package fr.roadguard.drive.radar

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
    private val tone = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 90) }.getOrNull()

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) return
        val result = tts.setLanguage(Locale.FRANCE)
        ttsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
    }

    fun announce(alert: RadarAlert) {
        tone?.startTone(ToneGenerator.TONE_PROP_BEEP2, 300)
        if (ttsReady) tts.speak(AlertText.spoken(alert), TextToSpeech.QUEUE_ADD, null, "rg-${alert.radar.id}")
    }

    fun announceAverageResult(status: AverageStatus) {
        if (status.over) tone?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 400)
        if (ttsReady) tts.speak(AlertText.averageSpoken(status), TextToSpeech.QUEUE_ADD, null, "rg-average")
    }

    /** Consigne de guidage ou message libre, à la suite des annonces en cours. */
    fun speak(text: String) {
        if (ttsReady && text.isNotBlank()) tts.speak(text, TextToSpeech.QUEUE_ADD, null, "rg-nav-${text.hashCode()}")
    }

    fun release() {
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
