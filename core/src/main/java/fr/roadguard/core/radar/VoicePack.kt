package fr.roadguard.core.radar

import android.content.Context
import android.media.MediaPlayer
import org.json.JSONObject
import java.io.File

/**
 * Voix « réaliste » : phrases pré-enregistrées au lieu de la voix de synthèse du téléphone.
 *  - tons 0 à 3 : voix « Jessica » générée une fois pour toutes (assets/voices/jessica) ;
 *  - ton 4, « Ma voix » : les phrases enregistrées soi-même (dossier myvoice de l'appli).
 * Les textes viennent de assets/voices/script.json (aussi utilisés pour la voix du téléphone en repli).
 */
class VoicePack(context: Context) {
    companion object {
        const val MY_VOICE = 9
        /** Ton « Coquin XXL » : bibliothèque de phrases liées aux vraies situations de route (section « xxl » du script). */
        const val XXL = 5

        /** Voix d'homme : les phrases (écrites pour une voix de femme) passent au masculin pour celui qui parle. */
        private val maleSpeaker = listOf(
            "je suis prête" to "je suis prêt", "je suis de nouveau prête" to "je suis de nouveau prêt",
            "toute retournée" to "tout retourné", "m'as menée" to "m'as mené", "plus performante" to "plus performant",
            "pas jalouse" to "pas jaloux", "sommes arrivées" to "sommes arrivés",
        )

        private val femaleWords = listOf(
            "mon gentil garçon" to "ma gentille fille", "gentil garçon" to "gentille fille", "vilain garçon" to "vilaine fille",
            "mon beau" to "ma belle", "beau pilote" to "belle pilote", "beau gosse" to "belle gosse",
            "mon chéri" to "ma chérie", "chéri" to "chérie", "mon fauve" to "ma tigresse", "maître de toi" to "maîtresse de toi",
            "tout à moi" to "toute à moi", "vilain" to "vilaine", "séduisant" to "séduisante",
            "fatigué" to "fatiguée", "épuisé" to "épuisée", "excité" to "excitée", "têtu" to "têtue", "obéissant" to "obéissante",
            "pressé" to "pressée", "mignon" to "mignonne", "entier" to "entière", "déchaîné" to "déchaînée",
            "bouillant" to "bouillante", "fougueux" to "fougueuse", "impatient" to "impatiente", "puni" to "punie",
            "prêt" to "prête", "seul" to "seule",
        ).flatMap { (m, f) -> listOf(m to f, m.replaceFirstChar { it.uppercase() } to f.replaceFirstChar { it.uppercase() }) }
            .map { (m, f) -> Regex("(?<![\\p{L}])" + Regex.escape(m) + "(?![\\p{L}])") to f }

        /** Met une phrase au féminin (mots entiers seulement : « pressés » au pluriel reste tel quel). */
        fun feminize(text: String): String = femaleWords.fold(text) { acc, (r, f) -> r.replace(acc, f) }
    }
    private val app = context.applicationContext
    val script: JSONObject = runCatching {
        JSONObject(app.assets.open("voices/script.json").bufferedReader().use { it.readText() })
    }.getOrDefault(JSONObject())
    private val generated: Set<String> by lazy {
        runCatching { app.assets.list("voices/jessica")?.map { it.removeSuffix(".mp3") }?.toSet() }.getOrNull() ?: emptySet()
    }
    val myDir = File(app.filesDir, "myvoice").apply { mkdirs() }
    fun myFile(key: String) = File(myDir, "$key.m4a")
    private fun mine(key: String) = myFile(key).takeIf { it.length() > 0 }

    /** Un morceau à jouer : fichier de l'appli (assets) ou enregistrement perso. */
    data class Clip(val asset: String? = null, val file: File? = null)

    private fun list(obj: JSONObject?, name: String): List<String> =
        obj?.optJSONArray(name)?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList()
    private fun tone(t: Int) = script.optJSONObject("tones")?.optJSONObject(t.coerceIn(0, 4).toString())
    /** Phrases au féminin (conductrice) : « ma belle », « ma chérie », « fatiguée »… */
    var feminine = false
    /** Langue des annonces (fr, en, de, it). Hors français : phrases simples, sans les tons. */
    var lang = "fr"
    private fun langObj() = script.optJSONObject("lang")?.optJSONObject(lang)

    /** Phrases du ton : chaque ton reprend aussi celles des tons plus doux (osé = osé + séduction + charme). */
    fun phrases(t: Int, group: String): List<String> = when {
        lang != "fr" && langObj() != null -> list(langObj(), group)
        t == MY_VOICE -> list(script.optJSONObject("myvoice"), group)
        t <= 0 -> list(tone(0), group)
        // « Coquin XXL » reprend aussi toutes les phrases des tons 1 à 4 pour ce qui n'est pas dans sa bibliothèque.
        else -> (t.coerceAtMost(4) downTo 1).flatMap { list(tone(it), group) }
    }.let { l -> if (feminine) l.map(::feminize) else l }

    /** La voix qui parle est une voix d'homme (accords « je suis prêt », « tout retourné »…). */
    var speakerMale = false

    /**
     * Phrase « Coquin XXL » du groupe [group] : variante conducteur ou conductrice, accordée à la voix,
     * variables ({distance}, {vitesse}) remplacées. Une phrase dont une variable n'est pas connue est écartée.
     * null s'il n'y a rien (le reste de l'appli reprend alors l'annonce habituelle).
     */
    fun xxl(group: String, vars: Map<String, String> = emptyMap()): String? {
        if (lang != "fr") return null
        val a = script.optJSONObject("xxl")?.optJSONArray(group) ?: return null
        val options = (0 until a.length()).mapNotNull { i ->
            val o = a.optJSONObject(i) ?: return@mapNotNull null
            var t = o.optString(if (feminine) "f" else "m").ifBlank { o.optString("m") }
            vars.forEach { (k, v) -> t = t.replace("{$k}", v) }
            t.takeIf { it.isNotBlank() && '{' !in it }
        }
        var s = options.randomOrNull() ?: return null
        if (speakerMale) maleSpeaker.forEach { (f, m) -> s = s.replace(f, m) }
        return s
    }
    fun number(v: Int): String? = script.optJSONObject("numbers")?.optString(v.toString())?.takeIf { it.isNotBlank() }
    fun numbers(): List<Int> = script.optJSONObject("numbers")?.keys()?.asSequence()?.mapNotNull { it.toIntOrNull() }?.sorted()?.toList() ?: emptyList()
    fun kinds(): Map<String, String> = script.optJSONObject("kinds")?.let { o -> o.keys().asSequence().associateWith { o.getString(it) } } ?: emptyMap()
    val limited: String get() = script.optString("limited", "limité à")

    /**
     * Les morceaux à enchaîner pour une annonce, ou null s'il en manque un (on reprend alors la voix du téléphone).
     * [group] : radar, limit, over ou alarm.
     */
    fun clips(t: Int, group: String, kind: RadarKind?, limit: Int?): List<Clip>? {
        if (t == MY_VOICE) return myClips(group, kind, limit)
        fun gen(key: String) = if (key in generated) Clip(asset = "voices/jessica/$key.mp3") else null
        return when (group) {
            "radar" -> {
                val k = kind ?: return null
                val body = gen("r.${k.name}.${limit ?: "x"}") ?: return null
                val intros = phrases(t, "radar").indices.mapNotNull { gen("t$t.radar.$it") }
                listOfNotNull(intros.randomOrNull(), body)
            }
            else -> {
                val v = limit ?: return null
                val options = phrases(t, group).indices.mapNotNull { gen("t$t.$group.$it.$v") }
                options.randomOrNull()?.let { listOf(it) }
            }
        }
    }

    private fun myClips(group: String, kind: RadarKind?, limit: Int?): List<Clip>? {
        fun my(key: String) = mine(key)?.let { Clip(file = it) }
        val n = limit?.let { my("n.$it") ?: return null }
        return when (group) {
            "radar" -> {
                val k = my("k.${(kind ?: return null).name}") ?: return null
                val intro = phrases(MY_VOICE, "radar").indices.mapNotNull { my("t4.radar.$it") }.randomOrNull()
                listOfNotNull(intro, k) + (if (n != null) listOf(my("c.limited") ?: return null, n) else emptyList())
            }
            else -> {
                if (n == null) return null
                val p = phrases(MY_VOICE, group).indices.mapNotNull { my("t4.$group.$it") }.randomOrNull() ?: return null
                listOf(p, n)
            }
        }
    }

    /** Texte de la même annonce, pour la voix du téléphone. */
    fun text(t: Int, group: String, kind: RadarKind?, limit: Int?): String {
        val tt = if (t == MY_VOICE) 0 else t
        val lim = limit?.toString().orEmpty()
        return when (group) {
            "radar" -> {
                val lo = langObj().takeIf { lang != "fr" }
                val label = kind?.let { k -> lo?.optJSONObject("kinds")?.optString(k.name)?.takeIf { it.isNotBlank() } ?: AlertText.kindLabel(k) } ?: "Radar"
                val word = lo?.optString("limited")?.takeIf { it.isNotBlank() } ?: limited
                val body = label + (limit?.let { ", $word $it" } ?: "")
                listOfNotNull(phrases(tt, "radar").randomOrNull(), body).joinToString(" ")
            }
            else -> "${phrases(tt, group).randomOrNull() ?: ""} $lim".trim()
        }
    }

    /** Liste des phrases à enregistrer pour « Ma voix » : clé → texte à prononcer. */
    fun recordingList(): List<Pair<String, String>> = buildList {
        phrases(MY_VOICE, "radar").forEachIndexed { i, p -> add("t4.radar.$i" to p) }
        kinds().forEach { (k, label) -> add("k.$k" to label) }
        add("c.limited" to limited)
        listOf("limit", "over", "alarm").forEach { g -> phrases(MY_VOICE, g).forEachIndexed { i, p -> add("t4.$g.$i" to p) } }
        numbers().forEach { v -> add("n.$v" to "$v  (${number(v)})") }
    }
}

/** Enchaîne les morceaux d'annonces les uns après les autres, sur le canal « guidage ». */
class ClipPlayer(context: Context, private val onStart: () -> Unit, private val onEnd: () -> Unit) {
    private val app = context.applicationContext
    private val queue = ArrayDeque<List<VoicePack.Clip>>()
    private var current: ArrayDeque<VoicePack.Clip>? = null
    private var player: MediaPlayer? = null
    private val attrs = android.media.AudioAttributes.Builder()
        .setUsage(android.media.AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH).build()

    fun play(clips: List<VoicePack.Clip>) {
        if (clips.isEmpty()) return
        queue.addLast(clips)
        if (current == null) nextSequence()
    }

    private fun nextSequence() {
        val seq = queue.removeFirstOrNull()
        if (seq == null) { current = null; return }
        current = ArrayDeque(seq)
        onStart()
        nextClip()
    }

    private fun nextClip() {
        val clip = current?.removeFirstOrNull()
        if (clip == null) { onEnd(); nextSequence(); return }
        val mp = MediaPlayer()
        player = mp
        val ok = runCatching {
            mp.setAudioAttributes(attrs)
            if (clip.asset != null) app.assets.openFd(clip.asset).use { mp.setDataSource(it.fileDescriptor, it.startOffset, it.length) }
            else mp.setDataSource(clip.file!!.absolutePath)
            mp.setOnCompletionListener { it.release(); player = null; nextClip() }
            mp.setOnErrorListener { p, _, _ -> p.release(); player = null; nextClip(); true }
            mp.prepare(); mp.start()
        }.isSuccess
        if (!ok) { runCatching { mp.release() }; player = null; nextClip() }
    }

    fun release() { queue.clear(); current = null; runCatching { player?.release() }; player = null }
}
