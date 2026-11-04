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

    // ---- Baisse du volume de la musique pendant les annonces et les bips ----
    private val audio = context.applicationContext.getSystemService(AudioManager::class.java)
    private val guidance = android.media.AudioAttributes.Builder()
        .setUsage(android.media.AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH).build()
    private val focusRequest = android.media.AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(guidance).setOnAudioFocusChangeListener { }.build()
    /** Baisser la musique pendant les annonces (réglage). */
    var duckMusic = true
    private var focused = false
    private var speaking = 0
    private var shortSoundUntil = 0L
    private val releaseFocus = Runnable {
        if (speaking <= 0 && !beeping && android.os.SystemClock.uptimeMillis() >= shortSoundUntil && focused) {
            runCatching { audio?.abandonAudioFocusRequest(focusRequest) }
            focused = false
        }
    }

    /** Demande à la musique de baisser (Android l'atténue tout seul) ; rendue juste après le son. */
    private fun duck(forMs: Long = 0) {
        if (!duckMusic) return
        if (forMs > 0) shortSoundUntil = maxOf(shortSoundUntil, android.os.SystemClock.uptimeMillis() + forMs)
        beeper.removeCallbacks(releaseFocus)
        if (!focused) focused = runCatching { audio?.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED }.getOrDefault(false)
        scheduleRelease()
    }

    private fun scheduleRelease() {
        beeper.removeCallbacks(releaseFocus)
        beeper.postDelayed(releaseFocus, maxOf(500L, shortSoundUntil - android.os.SystemClock.uptimeMillis() + 300))
    }

    private fun say(text: String, id: String) {
        if (!(ttsReady && voiceEnabled) || text.isBlank()) return
        speaking++
        duck()
        tts.speak(text, TextToSpeech.QUEUE_ADD, null, id)
    }
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
    @Volatile private var startM = 0

    /**
     * Distance au radar annoncé (null quand il n'y en a plus) : les bips s'accélèrent en approchant,
     * jusqu'à devenir continus dans les derniers mètres.
     */
    fun proximity(distanceM: Int?) {
        proximityM = if (beepEnabled && proximityBeeps) distanceM else null
        // Distance de départ de l'alerte : l'accélération s'étale de ce point jusqu'au radar.
        if (distanceM == null) startM = 0 else if (distanceM > startM || startM == 0) startM = distanceM
        if (proximityM != null && !beeping) { beeping = true; beeper.post(beepTick) }
    }

    private val beepTick = object : Runnable {
        override fun run() {
            val d = proximityM
            if (d == null) { beeping = false; scheduleRelease(); return }
            val interval = BeepCadence.intervalMs(d, startM)
            duck(interval + 200)
            tone?.startTone(ToneGenerator.TONE_PROP_BEEP, BeepCadence.toneMs(d))
            beeper.postDelayed(this, interval)
        }
    }

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) return
        val result = tts.setLanguage(Locale.FRANCE)
        ttsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
        tts.setAudioAttributes(guidance)
        tts.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) { beeper.post { speaking = (speaking - 1).coerceAtLeast(0); scheduleRelease() } }
            @Deprecated("API") override fun onError(utteranceId: String?) { onDone(utteranceId) }
        })
    }

    fun announce(alert: RadarAlert) {
        if (beepEnabled) { duck(600); tone?.startTone(ToneGenerator.TONE_PROP_BEEP2, 300) }
        val text = if (proximityBeeps) AlertText.spokenShort(alert) else AlertText.spoken(alert)
        say(text, "rg-${alert.radar.id}")
    }

    fun announceAverageResult(status: AverageStatus) {
        if (status.over && beepEnabled) { duck(700); tone?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 400) }
        say(AlertText.averageSpoken(status), "rg-average")
    }

    /** Consigne de guidage ou message libre, à la suite des annonces en cours. */
    fun speak(text: String) {
        say(text, "rg-nav-${text.hashCode()}")
    }

    fun release() {
        proximityM = null
        if (focused) runCatching { audio?.abandonAudioFocusRequest(focusRequest) }
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
    /**
     * Délai entre deux bips : 1,5 s au début de l'alerte (à [startM]), puis de plus en plus court
     * de façon régulière jusqu'au son continu sur le radar (sous 40 m).
     */
    fun intervalMs(distanceM: Int, startM: Int = 500): Long {
        if (distanceM <= 40) return 160
        val span = (startM.coerceAtLeast(100) - 40).toDouble()
        val ratio = ((distanceM - 40) / span).coerceIn(0.0, 1.0)
        // Courbe douce : l'accélération se sent dès le début et s'intensifie à l'approche.
        return (160 + (1500 - 160) * Math.pow(ratio, 0.8)).toLong()
    }

    /** Durée du bip : il remplit tout l'intervalle quand on est sur le radar (son continu). */
    fun toneMs(distanceM: Int): Int = if (distanceM <= 40) 170 else 90
}
