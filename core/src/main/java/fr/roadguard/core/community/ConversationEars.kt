package fr.roadguard.core.community

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * Micro qui écoute en continu pour le mode conversation. À chaque phrase entendue, [onHeard] est appelé ; pendant
 * que Pilote répond, on n'écoute pas (sinon il s'entendrait lui-même) : la séance rappelle [listen] une fois la
 * réponse dite. Les silences (erreurs « rien entendu ») relancent l'écoute tout seuls.
 *
 * Tout se passe sur le fil d'affichage (obligatoire pour SpeechRecognizer), comme pour le « oui/non ».
 */
class ConversationEars(context: Context, private val onHeard: (String) -> Unit) {
    private val app = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    @Volatile var active = false
        private set

    fun available(): Boolean =
        app.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
            SpeechRecognizer.isRecognitionAvailable(app)

    /** Démarre l'écoute en continu. */
    fun start() {
        if (active) return
        if (!available()) return
        active = true
        listen()
    }

    /** Arrête tout (bouton coupé, Pilote fermé). */
    fun stop() {
        active = false
        main.post {
            runCatching { recognizer?.cancel() }
            runCatching { recognizer?.destroy() }
            recognizer = null
        }
    }

    /** Relance une écoute (après que Pilote a fini de répondre, ou après un silence). */
    fun listen() {
        if (!active) return
        main.post {
            if (!active) return@post
            runCatching { recognizer?.destroy() }
            val r = runCatching { SpeechRecognizer.createSpeechRecognizer(app) }.getOrNull() ?: run { retry(); return@post }
            recognizer = r
            r.setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: Bundle) {
                    val heard = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                        .firstOrNull { it.isNotBlank() }
                    if (heard != null) onHeard(heard) else retry()
                }
                override fun onError(error: Int) {
                    // ERROR_NO_MATCH / SPEECH_TIMEOUT : silence normal, on réécoute. Micro occupé : on réessaie plus tard.
                    retry(if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY || error == SpeechRecognizer.ERROR_CLIENT) 1200L else 400L)
                }
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            runCatching {
                r.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR")
                    // Préfère la reconnaissance hors ligne quand le français « hors connexion » est installé sur le téléphone.
                    .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                    .putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
                    .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3))
            }.onFailure { retry() }
        }
    }

    private fun retry(delay: Long = 400L) {
        if (!active) return
        main.postDelayed({ listen() }, delay)
    }
}
