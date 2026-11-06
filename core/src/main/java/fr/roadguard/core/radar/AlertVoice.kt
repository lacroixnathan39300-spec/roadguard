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
    /** Phrases pré-enregistrées (voix réaliste « Jessica » ou « Ma voix »). */
    val pack = VoicePack(context)
    private val clipsLazy = lazy {
        ClipPlayer(context, onStart = { speaking++; duck() }, onEnd = { speaking = (speaking - 1).coerceAtLeast(0); scheduleRelease() })
    }
    private val clips by clipsLazy
    /** Voix réaliste au lieu de la voix du téléphone pour les annonces radars et limitations. */
    var realistic = false

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
            .mapIndexed { i, v ->
                val g = when (gender(v.name)) { 'F' -> " ♀ femme"; 'M' -> " ♂ homme"; else -> "" }
                v.name to "Voix ${i + 1}$g" + (if (v.locale.country.isNotBlank()) " (${v.locale.country})" else "")
            }
    }.getOrDefault(emptyList())

    /** Voix des autres langues installées (anglais, allemand, italien…), après téléchargement dans Android. */
    fun otherVoices(): List<Pair<String, String>> = runCatching {
        tts.voices.orEmpty().filter { it.locale.language != "fr" && !it.isNetworkConnectionRequired }
            .sortedWith(compareBy({ it.locale.getDisplayLanguage(java.util.Locale.FRENCH) }, { it.name }))
            .groupBy { it.locale.getDisplayLanguage(java.util.Locale.FRENCH).replaceFirstChar { c -> c.uppercase() } }
            .flatMap { (lang, vs) ->
                vs.mapIndexed { i, v ->
                    v.name to "$lang ${i + 1}" + (if (v.locale.country.isNotBlank()) " (${v.locale.country})" else "")
                }
            }
    }.getOrDefault(emptyList())

    /** Première voix de femme connue (pour les tons charme / séduction / osé). */
    fun femaleVoice(): String? = frenchVoices().map { it.first }.let { names ->
        listOf("vlf", "frc", "fra", "cac", "caa").firstNotNullOfOrNull { code -> names.firstOrNull { it.contains("-x-$code") && it.endsWith("-local") } ?: names.firstOrNull { it.contains("-x-$code") } }
    }

    companion object {
        /** Genre des voix Google connues (fr-fr-x-frb-local…) : 'F', 'M' ou null si inconnu. */
        fun gender(name: String): Char? {
            val code = Regex("-x-([a-z]{3})").find(name)?.groupValues?.get(1) ?: return null
            return when (code) { "fra", "frc", "vlf", "caa", "cac" -> 'F'; "frb", "frd", "cab", "cad" -> 'M'; else -> null }
        }
    }

    private var appliedVoice: String? = null
    private var appliedRate = 1f

    /** Ton : 0 classique, 1 charme, 2 séduction, 3 osé, 4 « Ma voix » (enregistrements perso). */
    var style = 0
    private var appliedStyle = 0

    /** Voix, débit et ton choisis dans les réglages (appliqués seulement s'ils changent). */
    fun configure(voiceName: String?, rate: Float, voiceTone: Int = style, language: String = pack.lang) {
        style = voiceTone
        if (language != pack.lang) {
            pack.lang = language
            if (ttsReady && voiceName == null) runCatching {
                tts.language = localeOf(language)
            }
        }
        if (!ttsReady) return
        if (rate != appliedRate || style != appliedStyle) {
            tts.setSpeechRate(rate * when (style) { 1 -> 0.92f; 2 -> 0.86f; 3 -> 0.84f; 4 -> 0.82f; else -> 1f })
            tts.setPitch(when (style) { 1 -> 1.0f; 2 -> 0.98f; 3 -> 0.97f; 4 -> 0.96f; else -> 1f }) // trop grave, une voix de femme sonnait masculine
            appliedRate = rate; appliedStyle = style
        }
        if (voiceName != appliedVoice) {
            appliedVoice = voiceName
            if (voiceName == null) tts.language = localeOf(pack.lang)
            else tts.voices?.firstOrNull { it.name == voiceName }?.let { tts.voice = it }
        }
    }

    private fun localeOf(l: String): Locale = when (l) { "en" -> Locale.UK; "de" -> Locale.GERMANY; "it" -> Locale.ITALY; else -> Locale.FRANCE }

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
        // Langue des annonces choisie avant que la synthèse soit prête : appliquée ici (sinon voix française).
        var result = tts.setLanguage(localeOf(pack.lang))
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) result = tts.setLanguage(Locale.FRANCE)
        ttsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
        appliedVoice = null // voix choisie réappliquée au prochain réglage
        tts.setAudioAttributes(guidance)
        tts.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                beeper.post {
                    speaking = (speaking - 1).coerceAtLeast(0); scheduleRelease()
                    utteranceId?.let { afterSpeech.remove(it) }?.invoke()
                }
            }
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
            if (now - lastAlarmVoice > 7_000) { lastAlarmVoice = now; line("alarm", null, alarmLimit, "rg-alarm") }
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
        if (proximityBeeps || pack.lang != "fr") line("radar", alert.radar.kind, alert.radar.speedKmh, "rg-${alert.radar.id}")
        else say(AlertText.spoken(alert), "rg-${alert.radar.id}") // avec la distance : voix du téléphone
    }

    fun announceAverageResult(status: AverageStatus) {
        if (status.over && beepEnabled) { duck(700); tone?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 400) }
        say(AlertText.averageSpoken(status), "rg-average")
    }

    /** Nouvelle limitation de vitesse. */
    fun limitChanged(limit: Int) = line("limit", null, limit, "rg-limit")

    /**
     * Consigne de guidage au ton choisi : la consigne reste entière et claire, avec une touche avant ou après
     * (« Mmmh… dans 300 mètres, tournez à droite, mon chéri. »). Ton classique : consigne seule.
     */
    fun guide(instruction: String) {
        if (style <= 0 || style == VoicePack.MY_VOICE || pack.lang != "fr") return speak(instruction)
        val base = instruction.trim().trimEnd('.', ' ')
        val pre = pack.phrases(style, "guide_pre").takeIf { Math.random() < 0.45 }?.randomOrNull()
        val post = pack.phrases(style, "guide_post").takeIf { pre == null || Math.random() < 0.5 }?.randomOrNull()
        val body = if (pre != null) base.replaceFirstChar { it.lowercase() } else base
        speak(listOfNotNull(pre, body + (post?.let { ", $it" } ?: "") + ".").joinToString(" "))
    }

    /** Réprimande (excès de vitesse qui dure ou se répète). */
    fun scold() = line("scold", null, null, "rg-scold")

    /** Somnolence : réveil d'urgence, ou conseil de pause. */
    fun wakeUp() {
        // Toujours dit, même quand les annonces sont coupées.
        if (!ttsReady) return
        speaking++; duck()
        tts.speak(pack.text(style, "wake", null, null), TextToSpeech.QUEUE_ADD, null, "rg-wake")
    }
    fun tired() = line("tired", null, null, "rg-tired")

    /** Dépassement de la limitation (hors radar). */
    fun overLimit(limit: Int?) = line("over", null, limit, "rg-over")

    /** Annonce par phrases enregistrées si elles existent, sinon par la voix du téléphone. */
    private fun line(group: String, kind: RadarKind?, limit: Int?, id: String) {
        if (!voiceEnabled) return
        val parts = if (realistic) pack.clips(style, group, kind, limit) else null
        if (parts != null) clips.play(parts) else say(pack.text(style, group, kind, limit), id)
    }

    /** Consigne de guidage ou message libre, à la suite des annonces en cours. */
    fun speak(text: String) {
        say(text, "rg-nav-${text.hashCode()}")
    }

    /** Suites à lancer quand une phrase est finie (écoute de la réponse « oui / non »). */
    private val afterSpeech = HashMap<String, () -> Unit>()

    /**
     * Pose une question puis appelle [then] seulement une fois la phrase entièrement dite :
     * le micro ne doit pas entendre la voix de l'appli (« … oui ou non ») et la prendre pour une réponse.
     * Rien n'est appelé si la voix est coupée (pas de question posée, donc pas d'écoute).
     */
    fun speakThen(text: String, then: () -> Unit) {
        if (!(ttsReady && voiceEnabled) || text.isBlank()) return
        val id = "rg-ask-${System.nanoTime()}"
        afterSpeech[id] = then
        say(text, id)
    }

    fun release() {
        if (clipsLazy.isInitialized()) runCatching { clips.release() } // sans créer le lecteur juste pour le fermer
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
