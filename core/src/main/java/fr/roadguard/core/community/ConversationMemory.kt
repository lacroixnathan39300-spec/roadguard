package fr.roadguard.core.community

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.Normalizer

/**
 * Mémoire du mode conversation : ce que le conducteur dit de lui (prénom, âge, proches, goûts, notes à retenir),
 * les réponses qu'il apprend à Pilote (« quand je dis X, réponds Y ») et son vocabulaire (ses mots familiers).
 *
 * Tout reste dans un fichier privé de l'appli, sur le téléphone : rien n'est envoyé au serveur. Le conducteur peut
 * tout consulter et tout effacer (réglages, ou « oublie tout » à la voix).
 */
object ConversationMemory {
    private var loaded = false
    private var file: File? = null
    private var o = JSONObject()
    /** Phrase pour laquelle Pilote vient de demander « tu veux que je réponde quoi ? ». */
    private var pendingTeach: String? = null

    @Synchronized fun load(context: Context) {
        if (loaded) return
        file = File(context.applicationContext.filesDir, "conversation_memory.json")
        o = runCatching { JSONObject(file!!.readText()) }.getOrDefault(JSONObject())
        loaded = true
    }

    private fun save() { runCatching { file?.writeText(o.toString()) } }
    private fun obj(k: String): JSONObject = o.optJSONObject(k) ?: JSONObject().also { o.put(k, it) }
    private fun arr(k: String): JSONArray = o.optJSONArray(k) ?: JSONArray().also { o.put(k, it) }
    private fun fact(k: String): String? = obj("facts").optString(k).takeIf { it.isNotBlank() }
    private fun list(k: String): List<String> = arr(k).let { a -> List(a.length()) { a.getString(it) } }

    @Synchronized fun clear() { o = JSONObject(); pendingTeach = null; save() }

    /** Prénom déjà connu du compte Pilote : repris si le conducteur ne l'a pas encore dit à la voix. */
    @Synchronized fun seedName(name: String?) {
        if (!name.isNullOrBlank() && fact("prenom") == null) { obj("facts").put("prenom", name.trim()); save() }
    }

    /** Clé de comparaison : minuscules, sans accents, « œ » → « oe », tirets → espaces. */
    private fun key(s: String): String =
        Normalizer.normalize(s.lowercase().replace("œ", "oe").replace('’', '\''), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "").replace(Regex("[^a-z0-9' ]+"), " ").replace(Regex(" +"), " ").trim()

    private fun cap(s: String) = s.replaceFirstChar { it.uppercase() }
    /** Garde le début d'une valeur dictée : jusqu'à la première ponctuation, sans espaces autour. */
    private fun clean(s: String) = s.split(Regex("[.!?;,]")).first().trim()
    private fun yours(det: String) = if (det.lowercase() == "ma") "ta" else "ton"

    // ───────────── Apprendre : ce que le conducteur dit de lui ─────────────

    private const val REL = "oncle|tante|père|mère|papa|maman|frère|sœur|soeur|fils|fille|femme|mari|copine|copain|" +
        "meilleur ami|meilleure amie|pote|ami|amie|chien|chienne|chat|chatte|grand-mère|grand mère|grand-père|grand père|" +
        "cousin|cousine|patron|patronne|chef|collègue|voisin|voisine|tonton|tata|beau-frère|belle-sœur|beau-père|" +
        "belle-mère|neveu|nièce|parrain|marraine|bébé|petit-fils|petite-fille"
    // Prénom, avec un éventuel second mot seulement s'il commence par une majuscule (« Jean Pierre », pas « Michel et »).
    private const val NAME = "([\\p{L}][\\p{L}\\-]*(?: (?-i:\\p{Lu})[\\p{L}\\-]*)?)"
    private fun rx(p: String) = Regex(p, RegexOption.IGNORE_CASE)
    private val notNames = setOf("comment", "quoi", "qui", "pas", "plus", "ça", "ca")

    private val rName = rx("\\b(?:je m'appelle|mon prénom(?:,)? c'est|mon prénom est|moi c'est) $NAME")
    private val rAge = rx("\\bj'ai (\\d{1,3}) ans\\b")
    private val rCity = rx("\\bj'habite ((?:à|a|au|aux|en|dans|vers|près de|pres de) .{2,40})")
    private val rJob = rx("\\b(?:je (?:travaille|bosse) (?:comme|en tant qu[e'] ?)|mon (?:métier|boulot|travail)(?:,)? c'est (?:d'être )?)(.{2,40})")
    private val rPerson = rx("\\b(mon|ma) ($REL) s'appelle $NAME")
    private val rFav = rx("\\b(mon|ma) (\\p{L}+ préférée?)(?:,)?(?: c'est| est) (.{2,40})")
    private val rCar = rx("\\bma (?:voiture|caisse|bagnole)(?:,)?(?: c'est| est) (.{2,40})")
    private val rBirthday = rx("\\bmon anniversaire(?:,)?(?: c'est| est)(?: le)? (.{3,30})")
    private val rNote = rx("\\b(?:retiens|note|souviens[- ]toi|rappelle[- ]toi|n'oublie pas|oublie pas)(?: bien)? (?:que |qu')(.{3,140})")
    private val rLike = rx("\\b(?:j'(?:aime|adore|kiffe)|je kiffe)(?: bien| beaucoup| trop| vraiment)? (.{2,50})")
    private val rDislike = rx("\\bje (?:déteste|hais|n'aime pas|aime pas|ne supporte pas|supporte pas) (.{2,50})")
    private val badLikeStart = Regex("^(quand|que|qu'|ça|ca|pas|plus|te |t'|bien$|beaucoup$|trop$|vraiment$)", RegexOption.IGNORE_CASE)

    private fun addTo(k: String, v: String, max: Int): Boolean {
        val a = arr(k)
        for (i in 0 until a.length()) if (key(a.getString(i)) == key(v)) return false
        a.put(v)
        while (a.length() > max) a.remove(0)
        return true
    }

    /** Retient ce que la phrase dit du conducteur ; rend la confirmation à dire, ou null s'il n'y a rien à retenir. */
    private fun learn(t: String): String? {
        rPerson.find(t)?.let { m ->
            val name = m.groupValues[3]
            if (key(name) !in notNames) {
                val rel = m.groupValues[2].lowercase()
                obj("people").put(key(rel), JSONObject().put("det", yours(m.groupValues[1])).put("rel", rel).put("name", cap(name)))
                return "C'est noté : ${yours(m.groupValues[1])} $rel s'appelle ${cap(name)}."
            }
        }
        rName.find(t)?.let { m ->
            val name = m.groupValues[1]
            if (key(name) !in notNames) { obj("facts").put("prenom", cap(name)); return "C'est noté, ${cap(name)}. Je m'en souviendrai." }
        }
        rAge.find(t)?.let { m -> obj("facts").put("age", m.groupValues[1]); return "${m.groupValues[1]} ans, c'est noté." }
        rCity.find(t)?.let { m -> clean(m.groupValues[1]).takeIf { it.length > 3 }?.let { obj("facts").put("ville", it); return "Ok, tu habites $it. Je retiens." } }
        rJob.find(t)?.let { m -> clean(m.groupValues[1]).takeIf { it.isNotEmpty() }?.let { obj("facts").put("metier", it); return "Ok, tu bosses comme $it. Je retiens." } }
        rFav.find(t)?.let { m ->
            val value = clean(m.groupValues[3])
            if (value.isNotEmpty()) {
                val what = m.groupValues[2].lowercase()
                obj("favorites").put(key(what), JSONObject().put("det", yours(m.groupValues[1])).put("what", what).put("value", value))
                return "Ok, ${yours(m.groupValues[1])} $what, c'est $value. Je retiens."
            }
        }
        rCar.find(t)?.let { m -> clean(m.groupValues[1]).takeIf { it.isNotEmpty() }?.let { obj("facts").put("voiture", it); return "Ta voiture, c'est $it. C'est noté." } }
        rBirthday.find(t)?.let { m -> clean(m.groupValues[1]).takeIf { it.isNotEmpty() }?.let { obj("facts").put("anniversaire", it); return "Ton anniversaire, le $it. Je m'en souviendrai." } }
        rNote.find(t)?.let { m ->
            val note = m.groupValues[1].trim().trimEnd('.', '!')
            if (note.isNotEmpty()) { addTo("notes", note, 50); return "C'est noté, je m'en souviendrai." }
        }
        rDislike.find(t)?.let { m ->
            val x = clean(m.groupValues[1])
            if (x.isNotEmpty() && !badLikeStart.containsMatchIn(x)) { addTo("dislikes", x, 30); return "Ok, je retiens que tu n'aimes pas $x." }
        }
        rLike.find(t)?.let { m ->
            val x = clean(m.groupValues[1])
            if (x.isNotEmpty() && !badLikeStart.containsMatchIn(x)) { addTo("likes", x, 30); return "Je retiens que tu aimes $x." }
        }
        return null
    }

    // ───────────── Répondre : questions sur ce que Pilote a retenu ─────────────

    private val relKeys = REL.split('|').map { key(it) }.distinct().sortedByDescending { it.length }.joinToString("|")
    private val qPerson = Regex("(?:comment s[' ]appelle|c[' ]est qui|tu te souviens de|tu te rappelles de|tu connais|parle moi de|c[' ]est quoi le (?:nom|prenom) de) (?:mon|ma) ($relKeys)\\b|(?:mon|ma) ($relKeys) (?:s[' ]appelle|c[' ]est) (?:comment|qui)\\b")
    private val qName = Regex("comment (?:je m[' ]appelle|est ce que je m[' ]appelle)|je m[' ]appelle comment|quoi mon (?:prenom|nom)|mon (?:prenom|nom) c[' ]est quoi|tu (?:connais|sais|te souviens de|te rappelles de) mon (?:prenom|nom)|quel est mon (?:prenom|nom)")
    private val qAge = Regex("quel age j[' ]ai|j[' ]ai quel age|mon age c[' ]est quoi|tu (?:connais|sais|te souviens de) mon age|quel est mon age")
    private val qCity = Regex("ou (?:est ce que )?j[' ]habite|j[' ]habite ou|tu sais ou j[' ]habite")
    private val qJob = Regex("(?:quoi|quel est) mon (?:metier|boulot|travail)|mon (?:metier|boulot|travail) c[' ]est quoi|je (?:fais|bosse|travaille) (?:dans )?quoi")
    private val qAll = Regex("qu[' ]est ce que tu (?:sais|connais|retiens|as retenu)(?: (?:sur|de) moi)?\\b|tu sais quoi (?:sur|de) moi|tu te souviens de quoi|tu me connais|tu as retenu quoi|t[' ]as retenu quoi")
    private val qLikes = Regex("qu[' ]est ce que j[' ]aime|j[' ]aime quoi|tu sais ce que j[' ]aime")
    private val qNotes = Regex("qu[' ]est ce que je t[' ]ai (?:dit|demande) de retenir|rappelle moi (?:mes notes|ce que je t[' ]ai dit)|quoi mes notes|mes notes c[' ]est quoi|lis (?:moi )?mes notes")

    private fun answer(n: String): String? {
        qPerson.find(n)?.let { m ->
            val rel = m.groupValues[1].ifEmpty { m.groupValues[2] }
            val p = obj("people").optJSONObject(rel)
            return if (p != null) "${cap(p.getString("det"))} ${p.getString("rel")} s'appelle ${p.getString("name")}."
                else "Tu ne me l'as pas encore dit. Comment s'appelle-t-il ?"
        }
        if (qName.containsMatchIn(n)) return fact("prenom")?.let { "Tu t'appelles $it." } ?: "Tu ne me l'as pas encore dit. Tu t'appelles comment ?"
        if (qAge.containsMatchIn(n)) return fact("age")?.let { "Tu as $it ans." } ?: "Tu ne me l'as pas encore dit. Tu as quel âge ?"
        if (qCity.containsMatchIn(n)) return fact("ville")?.let { "Tu habites $it." } ?: "Tu ne me l'as pas encore dit. Tu habites où ?"
        if (qJob.containsMatchIn(n)) return fact("metier")?.let { "Tu bosses comme $it." } ?: "Tu ne me l'as pas encore dit. Tu fais quoi comme boulot ?"
        if (qLikes.containsMatchIn(n)) return list("likes").takeIf { it.isNotEmpty() }?.let { "Tu aimes ${it.takeLast(4).joinToString(", ")}." } ?: "Tu ne m'as pas encore dit ce que tu aimes. Raconte !"
        if (qNotes.containsMatchIn(n)) return list("notes").takeIf { it.isNotEmpty() }?.let { "Tu m'as dit de retenir : ${it.takeLast(3).joinToString(". ")}." } ?: "Tu ne m'as rien demandé de retenir pour l'instant."
        if (qAll.containsMatchIn(n)) return summaryLines().take(6).takeIf { it.isNotEmpty() }?.joinToString(" ")
            ?: "Pas grand-chose encore. Dis-moi ton prénom, ce que tu aimes, comment s'appellent tes proches : je retiens tout."
        return null
    }

    /** Tout ce que Pilote a retenu, en phrases (lu à la voix et affiché dans les réglages). */
    @Synchronized fun summaryLines(): List<String> {
        val out = ArrayList<String>()
        fact("prenom")?.let { out += "Tu t'appelles $it." }
        fact("age")?.let { out += "Tu as $it ans." }
        fact("ville")?.let { out += "Tu habites $it." }
        fact("metier")?.let { out += "Tu bosses comme $it." }
        fact("voiture")?.let { out += "Ta voiture, c'est $it." }
        fact("anniversaire")?.let { out += "Ton anniversaire : le $it." }
        obj("people").let { p -> for (k in p.keys()) p.optJSONObject(k)?.let { out += "${cap(it.getString("det"))} ${it.getString("rel")} s'appelle ${it.getString("name")}." } }
        obj("favorites").let { f -> for (k in f.keys()) f.optJSONObject(k)?.let { out += "${cap(it.getString("det"))} ${it.getString("what")} : ${it.getString("value")}." } }
        list("likes").takeIf { it.isNotEmpty() }?.let { out += "Tu aimes ${it.joinToString(", ")}." }
        list("dislikes").takeIf { it.isNotEmpty() }?.let { out += "Tu n'aimes pas ${it.joinToString(", ")}." }
        list("notes").forEach { out += "À retenir : $it." }
        obj("taught").let { t -> for (k in t.keys()) out += "Quand tu dis « $k », je réponds « ${t.getString(k)} »." }
        val words = (addressWords + reactionWords + greetWords).filter { count(it.first) >= 2 }.map { it.second }
        if (words.isNotEmpty()) out += "Tes mots que je reprends : ${words.joinToString(", ")}."
        return out
    }

    // ───────────── Apprendre des réponses ─────────────

    private val rTeach = rx("\\b(?:quand|si) je (?:te )?dis (.{2,60}?),? (?:tu )?(?:me )?(?:réponds|répond|reponds|dis)(?:[- ]moi)? (.{1,140})")
    private val rForgetAll = Regex("\\boublie tout\\b|\\befface (?:tout|ta memoire)\\b|\\boublie ce que tu sais\\b")
    private val rForgetPerson = Regex("\\boublie (mon|ma) ($relKeys)\\b")
    private val rCancel = Regex("^(?:rien|non|laisse|oublie|annule|stop|pas besoin|laisse tomber)\\b")

    /**
     * À appeler pour chaque phrase entendue (hors musique). Rend ce que Pilote doit répondre quand la phrase
     * concerne sa mémoire (il retient, il répond à une question sur le conducteur, il a appris une réponse), sinon null.
     */
    @Synchronized fun handle(raw: String): String? {
        val t = raw.trim().replace('’', '\'')
        val n = key(t)
        if (n.isEmpty()) return null
        // Réponse attendue à « tu veux que je réponde quoi ? ».
        pendingTeach?.let { q ->
            pendingTeach = null
            if (rCancel.containsMatchIn(n)) return "Ok, on laisse tomber."
            obj("taught").put(q, t); save()
            return "Ça marche. Quand tu diras « $q », je répondrai « $t »."
        }
        if (rForgetAll.containsMatchIn(n)) { clear(); return "C'est fait, j'ai tout oublié." }
        rForgetPerson.find(n)?.let { m ->
            if (obj("people").has(m.groupValues[2])) { obj("people").remove(m.groupValues[2]); save(); return "Ok, j'oublie ${yours(m.groupValues[1])} ${m.groupValues[2]}." }
        }
        rTeach.find(t)?.let { m ->
            val q = key(m.groupValues[1]); val a = m.groupValues[2].trim().trimEnd('.', '!')
            if (q.isNotEmpty() && a.isNotEmpty()) {
                obj("taught").put(q, a)
                while (obj("taught").length() > 100) obj("taught").remove(obj("taught").keys().next())
                save()
                return "Ça marche. Quand tu diras « ${m.groupValues[1].trim()} », je répondrai « $a »."
            }
        }
        // Réponse apprise (le déclencheur le plus long l'emporte).
        val taught = obj("taught")
        var best: String? = null
        for (k in taught.keys()) if (" $k " in " $n " && k.length > (best?.length ?: 0)) best = k
        best?.let { return taught.getString(it) }
        answer(n)?.let { return it }
        return learn(t)?.also { save() }
    }

    /**
     * Phrase que le pack ne connaît pas : au bout de trois fois, Pilote demande quoi répondre et l'apprend.
     * Rend la question à poser, ou null.
     */
    @Synchronized fun noteUnknown(raw: String): String? {
        val n = key(raw)
        val words = n.split(' ').size
        if (words !in 2..8 || obj("taught").has(n)) return null
        val u = obj("unknown")
        val c = u.optInt(n) + 1
        u.put(n, c)
        if (u.length() > 150) { val once = u.keys().asSequence().filter { u.optInt(it) <= 1 }.toList(); once.take(60).forEach { u.remove(it) } }
        save()
        if (c != 3) return null
        pendingTeach = n
        return "Tu dis souvent « ${raw.trim()} ». Tu veux que je réponde quoi quand tu dis ça ?"
    }

    // ───────────── Vocabulaire : les mots du conducteur, repris par Pilote ─────────────

    /** (forme entendue sans accents, forme à dire). */
    private val addressWords = listOf("mec" to "mec", "frerot" to "frérot", "frero" to "frérot", "poto" to "poto", "mon gars" to "mon gars",
        "ma gueule" to "ma gueule", "mon pote" to "mon pote", "bro" to "bro", "le sang" to "le sang", "mon reuf" to "mon reuf",
        "ma belle" to "ma belle", "ma poule" to "ma poule", "champion" to "champion", "l'ami" to "l'ami")
    private val reactionWords = listOf("grave" to "grave", "carrement" to "carrément", "de ouf" to "de ouf", "trop bien" to "trop bien",
        "nickel" to "nickel", "tranquille" to "tranquille", "ca marche" to "ça marche", "ca roule" to "ça roule", "c'est clair" to "c'est clair",
        "a fond" to "à fond", "trop fort" to "trop fort", "mortel" to "mortel", "chanme" to "chanmé", "impec" to "impec", "oklm" to "oklm",
        "tranquillou" to "tranquillou", "incroyable" to "incroyable")
    private val greetWords = listOf("wesh" to "wesh", "yo" to "yo", "coucou" to "coucou", "hey" to "hey", "hello" to "hello", "bien ou bien" to "bien ou bien")

    private fun count(w: String) = obj("words").optInt(w)
    private fun top(words: List<Pair<String, String>>): String? =
        words.filter { count(it.first) >= 2 }.maxByOrNull { count(it.first) }?.second

    /** Compte les mots familiers du conducteur (pour les reprendre ensuite). */
    @Synchronized fun observe(raw: String) {
        val n = " ${key(raw)} "
        var changed = false
        for ((w, _) in addressWords + reactionWords + greetWords) if (" $w " in n) { obj("words").put(w, count(w) + 1); changed = true }
        if (changed) save()
    }

    private val endPunct = Regex("\\s*([.!?…]+)\\s*$")
    private val greetStart = Regex("^(Salut|Bonjour|Coucou|Hello|Hey|Yo)\\b", RegexOption.IGNORE_CASE)

    /**
     * Donne à une phrase du pack la couleur du conducteur : son mot pour saluer, son « mec / frérot… » de temps en
     * temps, et sa façon de réagir (« grave », « carrément »…) devant une relance.
     */
    @Synchronized fun flavor(reply: String, heard: String, isContinuer: Boolean): String {
        var s = reply
        val n = " ${key(heard)} "
        greetWords.firstOrNull { " ${it.first} " in n }?.let { g -> s = greetStart.replace(s) { cap(g.second) } }
        top(addressWords)?.let { a ->
            if (Math.random() < 0.30 && !s.contains(a, ignoreCase = true)) {
                // « T'en penses quoi ? » → « T'en penses quoi, mec ? » ; « C'est noté. » → « C'est noté, mec. »
                val m = endPunct.find(s)
                s = if (m == null) "$s, $a" else {
                    val p = m.groupValues[1]
                    s.substring(0, m.range.first) + ", $a" + (if (p.startsWith("?") || p.startsWith("!")) " $p" else p)
                }
            }
        }
        if (isContinuer) top(reactionWords)?.let { r -> if (Math.random() < 0.25 && !s.contains(r, ignoreCase = true)) s = "${cap(r)} ! $s" }
        return s
    }

    /** Relance personnelle bâtie sur ce que Pilote sait du conducteur (proches, goûts, métier…), ou null. */
    @Synchronized fun personal(): String? {
        val c = ArrayList<String>()
        obj("people").let { p -> for (k in p.keys()) p.optJSONObject(k)?.let {
            c += "Au fait, comment va ${it.getString("det")} ${it.getString("rel")} ${it.getString("name")} ?"
            c += "Et ${it.getString("name")}, ${it.getString("det")} ${it.getString("rel")}, quoi de neuf ?"
        } }
        list("likes").forEach { c += "Toi qui aimes $it, tu en as profité récemment ?"; c += "Parle-moi un peu de $it, je sais que tu aimes ça." }
        fact("metier")?.let { c += "Ça se passe bien en ce moment, le boulot comme $it ?" }
        fact("ville")?.let { c += "C'est comment la vie $it en ce moment ?" }
        fact("voiture")?.let { c += "Elle roule bien en ce moment, ta $it ?" }
        obj("favorites").let { f -> for (k in f.keys()) f.optJSONObject(k)?.let { c += "${cap(it.getString("det"))} ${it.getString("what")}, c'est toujours ${it.getString("value")} ?" } }
        fact("prenom")?.let { c += "$it, je peux te poser une question ? C'est quoi ton meilleur souvenir de route ?" }
        return c.randomOrNull()
    }
}
