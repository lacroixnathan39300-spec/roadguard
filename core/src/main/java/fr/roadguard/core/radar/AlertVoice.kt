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

    /** Voix françaises installées sur l'appareil (nom technique → libellé lisible). */
    fun frenchVoices(): List<Pair<String, String>> = runCatching {
        tts.voices.orEmpty().filter { it.locale.language == "fr" && !it.isNetworkConnectionRequired }
            .sortedBy { it.name }
            .mapIndexed { i, v -> v.name to "Voix ${i + 1}" + (if (v.locale.country.isNotBlank()) " (${v.locale.country})" else "") }
    }.getOrDefault(emptyList())

    private var appliedVoice: String? = null
    private var appliedRate = 1f

    /** Ton : 0 classique, 1 charme (complice), 2 séduction (aguicheuse) ; voix plus grave et plus lente. */
    var style = 0
    private var appliedStyle = 0

    /** Voix, débit et ton choisis dans les réglages (appliqués seulement s'ils changent). */
    fun configure(voiceName: String?, rate: Float, voiceTone: Int = style) {
        style = voiceTone
        if (!ttsReady) return
        if (rate != appliedRate || style != appliedStyle) {
            tts.setSpeechRate(rate * when (style) { 1 -> 0.9f; 2 -> 0.82f; 3 -> 0.8f; else -> 1f })
            tts.setPitch(when (style) { 1 -> 0.93f; 2 -> 0.88f; 3 -> 0.86f; else -> 1f })
            appliedRate = rate; appliedStyle = style
        }
        if (voiceName != appliedVoice) {
            appliedVoice = voiceName
            if (voiceName == null) tts.language = Locale.FRANCE
            else tts.voices?.firstOrNull { it.name == voiceName }?.let { tts.voice = it }
        }
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
    // Un seul générateur de sons : deux en même temps (bips + alarme) se coupaient l'un l'autre sur certains téléphones.
    private val tone = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 100) }.getOrNull()
    private val beeper = android.os.Handler(android.os.Looper.getMainLooper())
    @Volatile private var proximityM: Int? = null
    private var beeping = false
    @Volatile private var startM = 0

    /**
     * Distance au radar annoncé (null quand il n'y en a plus) : les bips s'accélèrent en approchant,
     * jusqu'à devenir continus dans les derniers mètres.
     */
    @Volatile private var stopAt = 0L

    /** [stopAfterMs] > 0 : radar dépassé, bips rapides encore ce temps-là puis arrêt (sans attendre la position suivante). */
    fun proximity(distanceM: Int?, stopAfterMs: Long = 0) {
        stopAt = if (stopAfterMs > 0 && distanceM != null) android.os.SystemClock.uptimeMillis() + stopAfterMs else 0L
        proximityM = if (beepEnabled && proximityBeeps) distanceM else null
        // Distance de départ de l'alerte : l'accélération s'étale de ce point jusqu'au radar.
        if (distanceM == null) startM = 0 else if (distanceM > startM || startM == 0) startM = distanceM
        if (proximityM != null && !beeping) { beeping = true; beeper.post(beepTick) }
    }

    private val beepTick = object : Runnable {
        override fun run() {
            if (stopAt > 0 && android.os.SystemClock.uptimeMillis() >= stopAt) { proximityM = null; stopAt = 0 }
            val d = proximityM
            if (d == null) { beeping = false; scheduleRelease(); return }
            val interval = BeepCadence.intervalMs(d, startM)
            duck(interval + 200)
            // Trop vite devant le radar : les bips deviennent l'alarme aiguë (même rythme, même générateur).
            if (alarmOn) tone?.startTone(ToneGenerator.TONE_CDMA_HIGH_PBX_SS, minOf(interval - 40, 600L).toInt().coerceAtLeast(120))
            else tone?.startTone(ToneGenerator.TONE_PROP_BEEP, BeepCadence.toneMs(d))
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

    // Alarme d'excès de vitesse devant un radar : son aigu répété, plus « Ralentis » à la voix.
    // Pendant les bips de proximité, ce sont les bips eux-mêmes qui passent en alarme (voir beepTick).
    private var alarmOn = false
    private var alarmLimit: Int? = null
    private var lastAlarmVoice = 0L
    private val alarmTick = object : Runnable {
        override fun run() {
            if (!alarmOn) return
            if (!beeping) { duck(900); tone?.startTone(ToneGenerator.TONE_CDMA_HIGH_PBX_SS, 700) }
            val now = android.os.SystemClock.uptimeMillis()
            if (now - lastAlarmVoice > 7_000) { lastAlarmVoice = now; say(Charm.alarm(style, alarmLimit), "rg-alarm") }
            beeper.postDelayed(this, 1_100)
        }
    }

    /** Active ou coupe l'alarme (appelé à chaque position). */
    fun overspeedAlarm(on: Boolean, limitKmh: Int?) {
        alarmLimit = limitKmh
        if (on == alarmOn) return
        alarmOn = on
        if (on) { lastAlarmVoice = 0; beeper.post(alarmTick) } else beeper.removeCallbacks(alarmTick)
    }

    fun announce(alert: RadarAlert) {
        if (beepEnabled) { duck(600); tone?.startTone(ToneGenerator.TONE_PROP_BEEP2, 300) }
        val text = if (proximityBeeps) AlertText.spokenShort(alert) else AlertText.spoken(alert)
        say(Charm.radar(style, text), "rg-${alert.radar.id}")
    }

    fun announceAverageResult(status: AverageStatus) {
        if (status.over && beepEnabled) { duck(700); tone?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 400) }
        say(AlertText.averageSpoken(status), "rg-average")
    }

    /** Nouvelle limitation de vitesse. */
    fun limitChanged(limit: Int) = speak(Charm.limit(style, limit))

    /** Dépassement de la limitation (hors radar). */
    fun overLimit(limit: Int?) = speak(Charm.over(style, limit))

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

/** Phrases des tons « Charme » (complice) et « Séduction » (aguicheuse), tirées au hasard pour ne pas lasser. */
object Charm {
    private fun pick(vararg options: String) = options.random()

    fun radar(tone: Int, base: String) = when (tone) {
        1 -> pick("Hmm… doucement, mon chéri. $base", "Attention à toi… $base", "Lève un peu le pied pour moi… $base", "Sois sage… $base")
        2 -> pick(
            "Mmmh… ralentis pour moi, mon beau… $base",
            "Tout doux, chéri… j'aime quand tu prends ton temps. $base",
            "Hmm… garde tes points, j'ai d'autres projets pour toi ce soir. $base",
            "Doucement… on n'est pas pressés, toi et moi. $base",
            "Sois sage maintenant… tu pourras être vilain plus tard. $base",
        )
        3 -> pick(
            "Oh oui… ralentis… doucement… encore… $base",
            "Mmmh, freine pour moi… je préfère quand c'est moi qui te fais monter en pression. $base",
            "Tout doux, mon fauve… garde ton énergie pour ce soir. $base",
            "Un flash ? Le seul flash que je veux, c'est quand tu me regardes. $base",
            "Ralentis, mon beau… j'adore quand tu y vas lentement. $base",
        )
        else -> base
    }

    fun limit(tone: Int, v: Int) = when (tone) {
        1 -> pick("On passe à $v… tout en douceur.", "Limitation $v, mon cœur.", "$v maintenant… prends ton temps.")
        2 -> pick(
            "Mmmh… $v maintenant. Tout en douceur, comme j'aime.",
            "On descend à $v, chéri… prends ton temps avec moi.",
            "$v… doucement, beau pilote. Fais durer le plaisir.",
            "Limitation $v… laisse-toi guider par ma voix.",
        )
        3 -> pick(
            "$v… on ralentit… j'aime sentir le rythme descendre.",
            "Passe à $v, chéri… prends le temps de bien faire les choses.",
            "$v maintenant… doucement… comme ça, c'est parfait.",
            "Limitation $v… tu sais que j'adore quand tu m'obéis.",
        )
        else -> "Limitation $v"
    }

    fun over(tone: Int, v: Int?) = when (tone) {
        1 -> pick("Tu vas un peu trop vite pour moi… c'est limité à $v.", "Doucement, beau pilote… limité à $v.", "Pas si vite… on est à $v ici.")
        2 -> pick(
            "Hmm… tu vas trop vite, mon beau. C'est $v ici… calme-toi pour moi.",
            "Oh… tu es fougueux aujourd'hui. Mais c'est limité à $v.",
            "Pas si vite, chéri… les meilleures choses se font lentement. $v.",
        )
        3 -> pick(
            "Hmm, tu es bien excité aujourd'hui… calme-toi, c'est $v ici.",
            "Trop vite, mon beau… tu vas finir avant moi. On reste à $v.",
            "Oh… tu me fais de l'effet quand tu roules comme ça… mais c'est $v.",
        )
        else -> "Attention, limité à $v"
    }

    fun alarm(tone: Int, v: Int?) = when (tone) {
        1 -> "Ralentis, mon chéri ! Limité à ${v ?: ""}"
        2 -> "Ralentis tout de suite, mon beau ! Limité à ${v ?: ""}"
        3 -> "Stop, stop, stop… freine, mon fauve ! Limité à ${v ?: ""}"
        else -> "Ralentis ! Limité à ${v ?: ""}"
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
