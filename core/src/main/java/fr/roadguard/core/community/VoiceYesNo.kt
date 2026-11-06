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
 * Réponse « oui / non » à la voix (« Police toujours là ? »), sans toucher l'écran.
 * Écoute quelques secondes après la question ; ne fait rien sans micro autorisé ou sans reconnaissance vocale.
 */
object VoiceYesNo {
    private val main = Handler(Looper.getMainLooper())
    private var current: SpeechRecognizer? = null

    /**
     * « oui » → true, « non » → false, sinon null. Mots entiers seulement, et une phrase qui contient
     * les deux (« oui ou non », la question elle-même entendue par le micro) ne compte pas.
     */
    fun parse(text: String): Boolean? {
        val t = " " + java.text.Normalizer.normalize(text.lowercase().replace('’', '\''), java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "").replace(Regex("[^a-z' ]+"), " ").replace(Regex(" +"), " ").trim() + " "
        fun has(w: String) = " $w " in t
        val no = listOf("non", "nan", "nope", "plus la", "parti", "partie", "partis", "rien", "aucun", "aucune").any(::has)
        val yes = listOf("oui", "ouais", "toujours", "encore", "yes", "ok", "affirmatif", "il est la", "c'est la", "elle est la", "ils sont la").any(::has)
        return when {
            no && yes -> null
            no -> false
            yes -> true
            else -> null
        }
    }

    fun ask(context: Context, delayMs: Long, onAnswer: (Boolean) -> Unit) {
        val app = context.applicationContext
        if (app.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) return
        if (!SpeechRecognizer.isRecognitionAvailable(app)) return
        main.postDelayed({
            runCatching {
                current?.destroy()
                val r = SpeechRecognizer.createSpeechRecognizer(app)
                current = r
                r.setRecognitionListener(object : RecognitionListener {
                    override fun onResults(results: Bundle) {
                        val heard = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                        heard.firstNotNullOfOrNull { parse(it) }?.let(onAnswer)
                        r.destroy(); if (current === r) current = null
                    }
                    override fun onError(error: Int) { r.destroy(); if (current === r) current = null }
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
                r.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR")
                    .putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
                    .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3))
            }
        }, delayMs)
    }
}
