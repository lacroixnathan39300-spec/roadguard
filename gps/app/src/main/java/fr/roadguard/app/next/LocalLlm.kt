package fr.roadguard.app.next

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import fr.roadguard.core.community.Llm
import fr.roadguard.core.community.LlmEngine
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.File

/**
 * IA hors ligne de Pilote, sur le téléphone, via MediaPipe LLM Inference (Google). Modèle : Qwen2.5-1.5B (libre,
 * Apache-2.0), au format .task, téléchargé une fois depuis Hugging Face puis stocké dans l'appli. Tout est protégé :
 * à la moindre erreur (modèle absent, mémoire insuffisante…), on rend null et Pilote repart sur son pack.
 */
object LocalLlm : LlmEngine {
    // Dépôt public du modèle déjà converti pour le téléphone. On lit la liste des fichiers pour trouver le .task,
    // sans deviner son nom exact (il change selon la quantification).
    private const val REPO = "litert-community/Qwen2.5-1.5B-Instruct"
    private const val TREE = "https://huggingface.co/api/models/$REPO/tree/main"
    private fun resolve(name: String) = "https://huggingface.co/$REPO/resolve/main/$name?download=true"

    @Volatile private var llm: LlmInference? = null
    @Volatile private var busy = false
    private val http by lazy { OkHttpClient() }

    private fun modelFile(ctx: Context) = File(ctx.applicationContext.filesDir, "pilote-llm.task")

    override fun ready(): Boolean = llm != null

    /** Charge le modèle s'il est déjà téléchargé (appelé au démarrage). Silencieux en cas d'absence ou d'échec. */
    fun load(ctx: Context) {
        if (llm != null) return
        val f = modelFile(ctx)
        if (!f.exists() || f.length() < 50_000_000L) return
        runCatching {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(f.absolutePath)
                .setMaxTokens(900)
                .setTopK(40)
                .build()
            llm = LlmInference.createFromOptions(ctx.applicationContext, options)
            Llm.installed = true
            Llm.status = "✓ IA prête"
        }.onFailure { Llm.status = "⚠ IA non chargée (mémoire insuffisante ?)"; llm = null }
    }

    /** Télécharge le modèle (gros fichier, à faire en Wi-Fi) puis le charge. Reporte l'avancement dans Llm.status. */
    fun install(ctx: Context) {
        if (busy) return
        if (llm != null) { Llm.status = "✓ IA déjà installée"; return }
        busy = true
        Thread {
            try {
                Llm.status = "⏳ Recherche du modèle…"
                val name = findTaskFile() ?: run { Llm.status = "⚠ Modèle introuvable sur le serveur"; return@Thread }
                Llm.status = "⏳ Téléchargement de l'IA (gros fichier, garde le Wi-Fi)…"
                val tmp = File(ctx.applicationContext.filesDir, "pilote-llm.part")
                download(resolve(name), tmp) { pct -> Llm.status = "⏳ Téléchargement de l'IA… $pct%" }
                val dst = modelFile(ctx)
                if (dst.exists()) dst.delete()
                if (!tmp.renameTo(dst)) { tmp.copyTo(dst, overwrite = true); tmp.delete() }
                Llm.status = "⏳ Chargement…"
                load(ctx)
            } catch (e: Throwable) {
                Llm.status = "⚠ Échec du téléchargement de l'IA. Réessaie en Wi-Fi."
            } finally {
                busy = false
            }
        }.start()
    }

    private fun findTaskFile(): String? = runCatching {
        http.newCall(Request.Builder().url(TREE).build()).execute().use { r ->
            if (!r.isSuccessful) return null
            val arr = JSONArray(r.body!!.string())
            var best: String? = null; var bestSize = Long.MAX_VALUE
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val path = o.optString("path")
                if (!path.endsWith(".task")) continue
                val size = o.optLong("size", Long.MAX_VALUE)
                // On prend le plus petit .task (le plus léger à télécharger et le plus rapide), de préférence quantifié.
                val score = if (path.contains("q8") || path.contains("int8") || path.contains("int4")) size - 1 else size
                if (score < bestSize) { bestSize = score; best = path }
            }
            best
        }
    }.getOrNull()

    private fun download(url: String, dst: File, onPct: (Int) -> Unit) {
        http.newCall(Request.Builder().url(url).build()).execute().use { r ->
            if (!r.isSuccessful) throw java.io.IOException("HTTP ${r.code}")
            val body = r.body!!
            val total = body.contentLength()
            var done = 0L; var last = -1
            body.byteStream().use { input ->
                dst.outputStream().use { out ->
                    val buf = ByteArray(1 shl 16)
                    while (true) {
                        val n = input.read(buf); if (n < 0) break
                        out.write(buf, 0, n); done += n
                        if (total > 0) { val p = (done * 100 / total).toInt(); if (p != last) { last = p; onPct(p) } }
                    }
                }
            }
        }
    }

    override fun reply(system: String, messages: List<Pair<String, String>>): String? {
        val l = llm ?: return null
        if (busy) return null
        synchronized(this) {
            return runCatching {
                busy = true
                val prompt = buildString {
                    append("<|im_start|>system\n").append(system).append("<|im_end|>\n")
                    for ((role, content) in messages.takeLast(8)) {
                        append("<|im_start|>").append(if (role == "assistant") "assistant" else "user").append('\n')
                        append(content).append("<|im_end|>\n")
                    }
                    append("<|im_start|>assistant\n")
                }
                clean(l.generateResponse(prompt))
            }.getOrNull().also { busy = false }
        }
    }

    /** Enlève les marques de gabarit et coupe à la fin du tour. */
    private fun clean(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        var s = raw.substringBefore("<|im_end|>").substringBefore("<|im_start|>")
        s = s.replace("<|im_end|>", "").replace("<|endoftext|>", "").trim()
        return s.ifBlank { null }
    }
}
