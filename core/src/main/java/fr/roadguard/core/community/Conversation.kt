package fr.roadguard.core.community

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import fr.roadguard.core.net.Net
import org.json.JSONObject
import java.io.File
import java.text.Normalizer

/**
 * Mode conversation hors-ligne : le pack de phrases (personnages + répliques) est téléchargé depuis le serveur,
 * gardé en mémoire sur le téléphone, et lu par la voix choisie. La compréhension se fait par mots-clés, sans
 * Internet. Les réponses « vivantes » (temps de trajet, vitesse, prochain radar…) sont fabriquées par la séance
 * de conduite ; ici on ne gère que les phrases préparées du pack.
 */
object Conversation {

    /** Une intention reconnue : des mots-clés déclencheurs et des répliques par personnage. */
    class Intent(val id: String, val keywords: List<String>, val replies: Map<String, List<String>>)
    class Pack(
        val version: Int, val personas: List<String>, val intents: List<Intent>,
        val fallback: Map<String, List<String>>,
        /** Listes de morceaux interchangeables : une réplique « {salut} {surnom} » pioche dans slots["salut"], etc. */
        val slots: Map<String, List<String>>,
        /** Relances : réactions + questions ouvertes dites quand rien de précis n'est reconnu, pour garder la conversation. */
        val continuers: Map<String, List<String>>,
    )

    @Volatile var pack: Pack? = null
        private set

    // Vrai quand le mode conversation est lancé et que le micro est réellement en écoute (pour afficher l'indicateur
    // 🎤 à l'écran de conduite). Mis à jour par la séance.
    var listening by androidx.compose.runtime.mutableStateOf(false)
    /** Pourquoi Pilote n'écoute pas (ou mal), en clair ; vide quand tout va bien. Affiché sur la carte. */
    var status by androidx.compose.runtime.mutableStateOf("")

    // Anti-répétition : les dernières phrases dites (après remplissage des trous) ; on évite de les redire.
    private val recent = ArrayDeque<String>()
    private const val RECENT_MAX = 400

    // Mémoire de conversation : Pilote retient « où on en est ». Quand un scénario s'ouvre (une histoire, un jeu, un
    // rendez-vous imaginaire…), on garde le fil et on le fait avancer aux tours suivants au lieu de repartir sur une
    // relance générique. Le fil s'efface tout seul après quelques tours sans qu'on y revienne.
    private var thread: String? = null   // id de l'intention de scénario en cours
    private var threadStep = 0           // progression dans ce scénario (ligne suivante à dire)
    private var threadLife = 0           // tours restants avant d'oublier le fil
    private const val THREAD_LIFE = 8    // un scénario tient ~8 échanges

    /** Oublie le fil de conversation en cours (au démarrage/arrêt du mode, ou pour repartir à neuf). */
    fun resetContext() { thread = null; threadStep = 0; threadLife = 0 }

    // Genre de l'interlocuteur (l'utilisateur) : "homme" ou "femme". On accorde les mots d'adresse en conséquence,
    // quel que soit le pack (ça marche aussi pour les packs neutres ou futurs). Réglé par la séance avant chaque réponse.
    @Volatile var addressGender: String = "homme"

    // Paires masculin → féminin des mots par lesquels le compagnon s'adresse au conducteur. On force vers le genre
    // de l'interlocuteur. L'article est inclus pour ne pas produire « mon chérie ».
    private val genderPairs = listOf(
        "mon chéri" to "ma chérie", "mon beau" to "ma belle", "mon grand" to "ma grande",
        "mon coquin" to "ma coquine", "beau gosse" to "belle gosse",
    )

    private fun flipWord(s: String, from: String, to: String, blockAfterMonMa: Boolean = false): String {
        val before = if (blockAfterMonMa) "(?<!mon )(?<!ma )" else ""
        val rx = Regex(before + "(?<!\\p{L})" + Regex.escape(from) + "(?!\\p{L})", RegexOption.IGNORE_CASE)
        return rx.replace(s) { m ->
            if (m.value.firstOrNull()?.isUpperCase() == true) to.replaceFirstChar { it.uppercase() } else to
        }
    }

    /** Accorde les mots d'adresse de [text] au genre de l'interlocuteur (chéri↔chérie, mon beau↔ma belle…). */
    private fun genderize(text: String): String {
        if ('é' !in text && 'e' !in text) return text
        var s = text
        val toFem = addressGender == "femme"
        for ((masc, fem) in genderPairs) s = if (toFem) flipWord(s, masc, fem) else flipWord(s, fem, masc)
        // « chéri » seul (sans mon/ma devant, déjà traités au-dessus).
        s = if (toFem) flipWord(s, "chéri", "chérie", blockAfterMonMa = true)
            else flipWord(s, "chérie", "chéri", blockAfterMonMa = true)
        return s
    }

    private fun file(context: Context) = File(context.applicationContext.filesDir, "conversation.json")

    /** Normalise un texte pour la comparaison : minuscules, sans accents ni ponctuation, espaces simples. */
    fun normalize(text: String): String =
        " " + Normalizer.normalize(text.lowercase().replace('’', '\''), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "").replace(Regex("[^a-z0-9' ]+"), " ").replace(Regex(" +"), " ").trim() + " "

    /** Charge le pack depuis le cache local (immédiat). Rend true si un pack est disponible. */
    fun loadCached(context: Context): Boolean {
        if (pack != null) return true
        val f = file(context)
        if (!f.exists()) return false
        return runCatching { pack = parse(f.readText()); pack != null }.getOrDefault(false)
    }

    /** Version du pack en cache (0 si aucun). */
    fun cachedVersion(context: Context): Int = pack?.version
        ?: runCatching { JSONObject(file(context).readText()).optInt("version") }.getOrDefault(0)

    /**
     * Télécharge le pack depuis le serveur et le met en cache (à appeler en arrière-plan). Rend true si un pack
     * est disponible à la fin (téléchargé ou déjà en cache).
     */
    fun download(context: Context, baseUrl: String, onProgress: (Int) -> Unit = {}): Boolean {
        runCatching {
            // Le pack peut être gros (quelques Mo) : téléchargement avec progression et délai plus large.
            val r = Net.fetchWithProgress("$baseUrl/v1/pack/conversation") { onProgress(it) }
            if (r.code in 200..299) {
                val text = r.text
                val p = parse(text)
                if (p != null) {
                    file(context).writeText(text)
                    pack = p
                    return true
                }
            }
        }
        return loadCached(context)
    }

    private fun parse(text: String): Pack? = runCatching {
        val o = JSONObject(text)
        val personas = o.optJSONArray("personas")?.let { a -> List(a.length()) { a.getString(it) } } ?: listOf("normal")
        val intentsArr = o.getJSONArray("intents")
        val intents = ArrayList<Intent>(intentsArr.length())
        for (i in 0 until intentsArr.length()) {
            val io = intentsArr.getJSONObject(i)
            val kw = io.getJSONArray("kw").let { a -> List(a.length()) { normalize(a.getString(it)).trim() } }
            val replies = repliesOf(io.getJSONObject("r"))
            intents.add(Intent(io.getString("id"), kw, replies))
        }
        val slots = o.optJSONObject("slots")?.let { repliesOf(it) } ?: emptyMap()
        val continuers = o.optJSONObject("continuers")?.let { repliesOf(it) } ?: emptyMap()
        Pack(o.optInt("version"), personas, intents, repliesOf(o.getJSONObject("fallback")), slots, continuers)
    }.getOrNull()

    private fun repliesOf(o: JSONObject): Map<String, List<String>> {
        val m = HashMap<String, List<String>>()
        for (key in o.keys()) {
            val a = o.getJSONArray(key)
            m[key] = List(a.length()) { a.getString(it) }
        }
        return m
    }

    // Langage familier : on transforme les tournures « écrites » en français parlé (« tu es » → « t'es », « je n'ai
    // pas » → « j'ai pas », « il y a » → « y a », « as-tu » → « t'as »…). Fait en code, donc valable pour tout pack.
    // Réglé par la séance avant chaque réponse (jamais pour le personnage « normal »).
    @Volatile var familiar = false

    private const val AP = "['’]"                       // apostrophe droite ou typographique
    private const val W0 = "(?<![\\p{L}\\p{N}])"        // début de mot
    private const val W1 = "(?![\\p{L}\\p{N}])"         // fin de mot
    // « ne … pas/plus/jamais/rien » : la négation doit suivre dans les quelques mots, sans ponctuation entre deux.
    private const val NEG = "(?=(?:[^\\s.!?,;:]+ ){0,4}?(?:pas|plus|jamais|rien)$W1)"
    private fun rule(p: String, r: (MatchResult) -> String): Pair<Regex, (MatchResult) -> String> = Regex(p, RegexOption.IGNORE_CASE) to r
    private val familiarRules: List<Pair<Regex, (MatchResult) -> String>> = listOf(
        rule("${W0}n${AP}est-ce pas$W1") { _ -> "hein" },
        // Questions avec inversion → tournure parlée (« qu'en penses-tu » → « t'en penses quoi », « as-tu » → « tu as »).
        rule("${W0}qu${AP}en (\\p{L}+)-tu$W1") { m -> "t'en ${m.groupValues[1].lowercase()} quoi" },
        rule("${W0}que (\\p{L}+)-tu$W1") { m -> "tu ${m.groupValues[1].lowercase()} quoi" },
        rule("$W0(\\p{L}+)-tu$W1") { m -> "tu ${m.groupValues[1].lowercase()}" },
        // Négation sans « ne ».
        rule("${W0}je n$AP$NEG") { _ -> "j'" },
        rule("${W0}ce n${AP}est$W1(?= (?:pas|plus|jamais|rien)$W1)") { _ -> "c'est" },
        rule("${W0}il n${AP}y a$W1") { _ -> "y a" },
        rule("${W0}il y a$W1") { _ -> "y a" },
        rule("$W0(?:ne |n$AP)$NEG") { _ -> "" },
        rule("${W0}il faut$W1") { _ -> "faut" },
        // « tu » devant une voyelle → « t' » (pas après un trait d'union, ni dans « tu ou moi »).
        rule("(?<![-\\p{L}\\p{N}])tu (?=[aeiouyhéèêàâîôû])(?!ou$W1|et$W1|à$W1)") { _ -> "t'" },
        // Vocabulaire.
        rule("${W0}cela$W1") { _ -> "ça" },
        rule("(^|[.!?…]\\s)oui$W1") { m -> m.groupValues[1] + "ouais" },
        rule("${W0}quelque chose$W1") { _ -> "un truc" },
        rule("${W0}travail$W1") { _ -> "boulot" },
        rule("${W0}fatigué(e?s?)$W1") { m -> "crevé" + m.groupValues[1] },
        rule("${W0}très$W1(?! bien$W1)") { _ -> "trop" },
        rule("${W0}bonjour$W1") { _ -> "salut" },
        rule("${W0}d${AP}accord(?=\\s*[,.!])") { _ -> "ok" },
        rule("${W0}bien sûr(?=\\s*[,.!])") { _ -> "carrément" },
    )
    private val sentenceStart = Regex("(^|[.!?…]\\s+)(\\p{Ll})")

    private fun familiarize(text: String): String {
        var s = text
        for ((regex, repl) in familiarRules) s = regex.replace(s, repl)
        // Les remplacements sont en minuscules : on remet la majuscule en début de phrase.
        return sentenceStart.replace(s) { it.groupValues[1] + it.groupValues[2].uppercase() }
    }

    // Petite correction de français des packs générés : « de un budget » → « d'un budget », « de une » → « d'une ».
    private val elision = Regex("${W0}de (une?)$W1", RegexOption.IGNORE_CASE)

    /** Corrige l'élision, met la phrase au genre de l'interlocuteur et, si demandé, en langage familier. */
    fun polish(text: String): String {
        val fixed = elision.replace(text) { (if (it.value.first() == 'D') "D'" else "d'") + it.groupValues[1] }
        return genderize(fixed).let { if (familiar) familiarize(it) else it }
    }

    /** Remplit les trous « {slot} » d'un modèle en piochant au hasard dans les listes du pack. */
    private fun render(template: String): String {
        val slots = pack?.slots
        val filled = if (slots == null || '{' !in template) template
            else Regex("\\{([a-z0-9_]+)\\}").replace(template) { m ->
                slots[m.groupValues[1]]?.takeIf { it.isNotEmpty() }?.random() ?: m.value
            }
        return polish(filled)
    }

    private fun noteSaid(s: String) {
        recent.addLast(s)
        while (recent.size > RECENT_MAX) recent.removeFirst()
    }

    /**
     * Une réplique du groupe, pour le personnage demandé (secours sur « normal »), trous remplis, en évitant de
     * répéter une phrase dite récemment (jusqu'à épuisement du stock de combinaisons).
     */
    private fun pick(replies: Map<String, List<String>>, persona: String): String? {
        val list = replies[persona]?.takeIf { it.isNotEmpty() } ?: replies["normal"]?.takeIf { it.isNotEmpty() } ?: return null
        var chosen = render(list.random())
        // Plusieurs essais : avec les modèles à trous, on tombe presque toujours sur une phrase inédite.
        var tries = 0
        while (chosen in recent && tries < 8) { chosen = render(list.random()); tries++ }
        noteSaid(chosen)
        return chosen
    }

    /**
     * Cherche une phrase préparée qui répond à [text] pour le [persona] donné. Rend null si aucune intention du pack
     * ne correspond (la séance de conduite a déjà tenté les réponses « vivantes » avant d'arriver ici).
     */
    fun staticReply(text: String, persona: String): String? {
        val p = pack ?: return null
        val t = normalize(text)
        val intent = p.intents.firstOrNull { intent -> intent.keywords.any { it.isNotEmpty() && " $it " in t } } ?: return null
        // Mémoire : si le conducteur lance (ou relance) un scénario, on retient le fil pour l'enchaîner ensuite ;
        // toute autre intention reconnue laisse le fil vivre encore un peu puis s'effacer.
        if (intent.id.startsWith("scenario_")) { thread = intent.id; threadStep = 1; threadLife = THREAD_LIFE }
        else if (threadLife > 0) threadLife--
        return pick(intent.replies, persona)
    }

    /**
     * Relance pour garder la conversation quand aucune intention précise ne correspond. Si un scénario est en cours,
     * on le fait avancer (ligne suivante de l'histoire) au lieu d'une relance générique : c'est ce qui donne
     * l'impression que Pilote « suit » la conversation sur plusieurs minutes.
     */
    fun continuerReply(persona: String): String? {
        val p = pack ?: return null
        val cur = thread
        if (cur != null && threadLife > 0) {
            threadLife--
            if (threadLife == 0) thread = null
            val intent = p.intents.firstOrNull { it.id == cur }
            val list = intent?.replies?.get(persona)?.takeIf { it.isNotEmpty() }
                ?: intent?.replies?.get("normal")?.takeIf { it.isNotEmpty() }
            if (list != null) {
                val line = render(list[threadStep % list.size]); threadStep++
                noteSaid(line)
                return line
            }
        }
        return pick(p.continuers, persona)
    }

    /** Phrase de secours quand rien n'est compris, pour le personnage. */
    fun fallbackReply(persona: String): String? = pack?.let { pick(it.fallback, persona) }
}
