package fr.roadguard.core.community

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * « Intelligence » hors ligne du mode conversation, en plus des phrases du pack :
 *  - sécurité d'abord (fatigue, alcool, détresse) : une vraie réponse utile, jamais une blague ;
 *  - petites réponses calculées (date, calcul, pile ou face, dé, nombre au hasard) ;
 *  - suite logique quand le conducteur répond « oui / non / bof » à une question de Pilote ;
 *  - reformulation : Pilote reprend ce que le conducteur vient de dire pour lui répondre dessus
 *    (« j'ai envie de partir en vacances » → « Qu'est-ce qui te donne envie de partir en vacances ? »).
 *
 * Tout est fait par règles, sans Internet. Les phrases rendues sont des modèles « neutres » : le genre et le
 * langage familier sont appliqués ensuite par [Conversation.fresh].
 */
object ConversationBrain {
    private fun rx(p: String) = Regex(p, RegexOption.IGNORE_CASE)
    private fun cap(s: String) = s.replaceFirstChar { it.uppercase() }

    // ───────────── Sécurité ─────────────

    private val distress = Regex("(?:envie de|je vais|je veux|je pense a) (?:mourir|me tuer|en finir|me suicider|me foutre en l'air)|suicid|plus envie de vivre|envie d'en finir")
    private val alcohol = Regex("\\bj'ai (?:trop |beaucoup |un peu )?bu\\b|\\bje suis (?:bourre|bourree|ivre|pete|petee|defonce|defoncee|saoul|saoule|soul|soule)\\b|\\bj'ai fume un joint\\b|\\bj'ai pris (?:de la drogue|un rail)\\b")
    private val tired = Regex("\\b(?:fatigue|fatiguee|creve|crevee|epuise|epuisee|claque|claquee)\\b|\\bsommeil\\b|\\bje m'endors\\b|\\bje somnole\\b|\\bmes yeux se ferment\\b|\\bje pique du nez\\b|\\benvie de dormir\\b|\\bje baille\\b")

    /** Réponses de sécurité pour une phrase normalisée (sans accents), ou null. Passe avant tout le reste. */
    fun safety(n: String): List<String>? = when {
        distress.containsMatchIn(n) -> listOf(
            "Ce que tu dis là est sérieux, et tu comptes. Gare-toi dès que tu peux. Tu peux appeler le 31 14, le numéro national de prévention du suicide : c'est gratuit, jour et nuit, et quelqu'un t'écoute vraiment.",
            "Je te prends au sérieux. Arrête-toi dans un endroit sûr et appelle le 31 14, gratuit, jour et nuit. Et si tu es en danger tout de suite, appelle le 15.",
        )
        alcohol.containsMatchIn(n) -> listOf(
            "Là je ne rigole plus : si tu as bu ou pris quelque chose, tu ne conduis pas. Arrête-toi dès que c'est sûr et appelle quelqu'un ou un taxi.",
            "Alcool et volant, c'est non. Gare-toi au premier endroit sûr et fais-toi ramener. Je préfère te savoir en vie.",
        )
        tired.containsMatchIn(n) -> listOf(
            "Si tu fatigues, vise la prochaine aire et dors un quart d'heure. Je reste avec toi jusque-là.",
            "La fatigue au volant, ça ne se négocie pas : pause dès que possible, quelques pas, un café. Tu veux que je te parle en attendant ?",
            "Ouvre un peu la fenêtre et arrête-toi à la prochaine aire. Quinze minutes de sieste et ça repart.",
        )
        else -> null
    }

    // ───────────── Petites réponses calculées ─────────────

    private val qDay = Regex("quel jour|on est le combien|quelle date|la date d'aujourd'hui|on est quel jour")
    private val qCalc = Regex("(\\d+(?:[.,]\\d+)?) ?(plus|\\+|moins|-|fois|x|\\*|multiplie par|multiplié par|divise par|divisé par|sur|/) ?(\\d+(?:[.,]\\d+)?)")
    private val qNumber = Regex("(?:choisis|donne|dis|tire)(?:[- ]moi)? un (?:nombre|chiffre)(?: entre (\\d+) et (\\d+))?")
    private val qDice = Regex("(?:lance|jette|tire) (?:un|le) de\\b|(?:lance|jette|tire) (?:un|le) dé\\b")

    private fun num(v: Double): String = if (v == Math.floor(v) && Math.abs(v) < 1e12) v.toLong().toString() else String.format(Locale.FRANCE, "%.2f", v)

    /** Date, calcul, pile ou face, dé, nombre au hasard : la réponse, ou null si la phrase n'en demande pas. */
    fun quick(raw: String): String? {
        val t = raw.lowercase().replace('’', '\'')
        if ("pile ou face" in t) return if (Math.random() < 0.5) "Pile !" else "Face !"
        if (qDice.containsMatchIn(t)) return "Le dé dit ${(1..6).random()}."
        qNumber.find(t)?.let { m ->
            val a = m.groupValues[1].toIntOrNull() ?: 1
            val b = m.groupValues[2].toIntOrNull() ?: 10
            return "Je dis ${(minOf(a, b)..maxOf(a, b)).random()}."
        }
        if (qDay.containsMatchIn(t)) return "On est " + SimpleDateFormat("EEEE d MMMM", Locale.FRANCE).format(Date()) + "."
        if (Regex("combien (?:font|fait|ca fait|ça fait)|calcule|\\d ?(?:\\+|x|\\*|/) ?\\d|\\d (?:plus|moins|fois|divisé par|divise par|multiplié par|multiplie par) \\d").containsMatchIn(t)) {
            qCalc.find(t)?.let { m ->
                val a = m.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
                val b = m.groupValues[3].replace(',', '.').toDoubleOrNull() ?: return null
                val r = when (m.groupValues[2]) {
                    "plus", "+" -> a + b
                    "moins", "-" -> a - b
                    "fois", "x", "*", "multiplie par", "multiplié par" -> a * b
                    else -> if (b == 0.0) return "On ne divise pas par zéro, même moi je ne sais pas faire." else a / b
                }
                return "Ça fait ${num(r)}."
            }
        }
        return null
    }

    // ───────────── Suite d'une question de Pilote ─────────────

    private val yes = Regex("^(?:oui|ouais|ouaip|yes|grave|carrement|bien sur|exact|evidemment|un peu|pas mal|tout a fait|c'est ca|voila|oui oui|ah oui|ben oui|bah oui|de ouf|clairement|absolument)$")
    private val no = Regex("^(?:non|nan|pas du tout|jamais|pas vraiment|non merci|ah non|ben non|bah non|non non|pas trop|meme pas)$")
    private val meh = Regex("^(?:bof|je sais pas|j'sais pas|je ne sais pas|peut etre|ca depend|aucune idee|moyen|mouais|pas sur|je sais plus|aucune)$")

    /** Le conducteur répond court à une question de Pilote : la suite logique (phrases au choix), ou null. */
    fun followUp(n: String): List<String>? = when {
        yes.containsMatchIn(n) -> listOf("Ah ouais ? Raconte-moi.", "Je m'en doutais. Dis-m'en plus.", "Cool. Et c'était comment ?", "Vas-y, développe, ça m'intéresse.", "Et ça remonte à quand ?")
        no.containsMatchIn(n) -> listOf("Ah bon ? Pourquoi ?", "Non ? Qu'est-ce qui te retient ?", "Ok. Tu préfères qu'on parle d'autre chose ?", "Même pas un peu ?", "Jamais essayé, ou jamais envie ?")
        meh.containsMatchIn(n) -> listOf("Ça dépend de quoi ?", "Dis la première chose qui te vient.", "Pas de pression. On y revient plus tard ?", "Tu hésites entre quoi et quoi ?")
        else -> null
    }

    // ───────────── Reformulation ─────────────

    private val fillers = Regex("^(?:(?:bah|ben|bon|euh|alors|en fait|du coup|franchement|écoute|ecoute|tu sais|tu vois|non mais|mais|et|donc|là|genre|dis-moi|dis moi|dis)[ ,]+)+")
    private val trailers = Regex("(?: (?:toi|hein|là|tu sais|tu vois))+$")
    /** Minuscules, apostrophes droites, sans ponctuation finale ni mots de remplissage au début ou à la fin. */
    private fun tidy(raw: String): String =
        raw.trim().lowercase().replace('’', '\'').trimEnd('.', '!', '?', ' ', '…').replace(fillers, "").replace(trailers, "")

    // Passage de « je » à « tu » (et inversement) dans le morceau repris, en un seul passage.
    private val swapMap = mapOf(
        "je suis" to "tu es", "j'ai" to "tu as", "je vais" to "tu vas", "je peux" to "tu peux", "je veux" to "tu veux",
        "je dois" to "tu dois", "je fais" to "tu fais", "je sais" to "tu sais", "j'étais" to "tu étais", "j'avais" to "tu avais",
        "je me" to "tu te", "je m'" to "tu t'", "je" to "tu", "me" to "te", "m'" to "t'", "moi" to "toi",
        "mon" to "ton", "ma" to "ta", "mes" to "tes",
        "tu es" to "je suis", "t'es" to "je suis", "tu as" to "j'ai", "t'as" to "j'ai", "tu vas" to "je vais", "tu peux" to "je peux",
        "tu veux" to "je veux", "tu dois" to "je dois", "tu fais" to "je fais", "tu sais" to "je sais",
        "tu te" to "je me", "tu t'" to "je m'", "tu" to "je", "te" to "me", "t'" to "m'", "toi" to "moi",
        "ton" to "mon", "ta" to "ma", "tes" to "mes",
    )
    private val swapWords = swapMap.keys.filter { !it.endsWith("'") && it != "je" && it != "tu" }
        .sortedByDescending { it.length }.joinToString("|") { Regex.escape(it) }
    private val swapRx = Regex(
        "(?<![\\p{L}'])(?:" +
            "(?:$swapWords)(?![\\p{L}'])" +
            "|je (?!me |te |le |la |ne |se |que |ce )\\p{L}+e(?![\\p{L}'])" +
            "|j'\\p{L}+e(?![\\p{L}'])" +
            "|tu \\p{L}{2,}es(?![\\p{L}'])" +
            "|(?:je|tu)(?![\\p{L}'])" +
            "|je m'|tu t'|m'|t'|j'" +
            ")", RegexOption.IGNORE_CASE)

    /** « je suis parti avec mon frère » → « tu es parti avec ton frère ». */
    private fun swap(x: String): String = swapRx.replace(x) { m ->
        val v = m.value.lowercase()
        swapMap[v] ?: when {
            v.startsWith("je ") -> "tu " + v.substring(3) + "s"
            v.startsWith("j'") && v.length > 2 -> "tu " + v.substring(2) + "s"
            v == "j'" -> "tu "
            v.startsWith("tu ") -> "je " + v.substring(3).dropLast(1)
            else -> v
        }
    }

    private fun startsVowel(x: String) = x.firstOrNull()?.let { it in "aeiouyhéèêàâîôûœ" } == true
    private fun fill(template: String, x: String): String = template
        .replace("{de}", if (startsVowel(x)) "d'$x" else "de $x")
        .replace("{que}", if (startsVowel(x)) "qu'$x" else "que $x")
        .replace("{X}", cap(x)).replace("{x}", x)

    /** [late] : règle « de secours », essayée seulement si le pack n'a rien de précis à répondre. */
    private class Rule(val re: Regex, val templates: List<String>, val late: Boolean = false)
    private fun rule(p: String, vararg t: String) = Rule(rx(p), t.toList())
    private fun lateRule(p: String, vararg t: String) = Rule(rx(p), t.toList(), late = true)

    // L'ordre compte : la première règle qui correspond l'emporte. {x} = le morceau repris (passé de « je » à « tu »),
    // {de} / {que} = le même, précédé de « de » ou « que » avec l'élision.
    private val rules = listOf(
        // Questions posées à Pilote.
        rule("^(?:est-ce que )?tu (?:aimes|kiffes|adores)(?: bien)? (.+)",
            "{X} ? Ouais, j'aime bien. Et toi, tu aimes ?", "Franchement, {x}, ça dépend des jours. Toi, tu aimes ça ?",
            "J'aime surtout quand c'est toi qui m'en parles. Pourquoi, tu aimes {x} ?"),
        rule("^(?:tu (?:penses|dis) quoi|qu'est-ce que tu (?:penses|dis)|tu penses quoi) (?:de |du |des |d')(.+)",
            "J'ai mon avis là-dessus, mais donne-moi le tien d'abord.", "Je pense que ça mérite qu'on en parle. Tu en penses quoi, toi ?",
            "Honnêtement ? Ça dépend. Dis-moi ce que toi tu en penses."),
        lateRule("^(?:est-ce que )?tu (?:connais|as déjà vu|as vu|as entendu parler de) (.+)",
            "De nom, ouais. Raconte-moi ce que tu en sais.", "Pas autant que toi, je parie. Tu me racontes ?", "Un peu. C'est quoi que tu aimes là-dedans ?"),
        rule("^(?:tu fais quoi|qu'est-ce que tu fais|tu fous quoi)",
            "Je roule avec toi, c'est déjà pas mal. Et toi, tu penses à quoi ?", "Je t'écoute et je surveille la route. Raconte-moi un truc."),
        lateRule("^(?:est-ce que )?tu (?:peux|sais|pourrais|veux bien) (.+)",
            "Je fais de mon mieux depuis ton téléphone. Tu veux quoi exactement ?", "Je peux toujours essayer. Explique-moi."),
        rule("^(?:tu es|t'es) (.+)",
            "Tu trouves que je suis {x} ?", "Je suis {x}, moi ? Qu'est-ce qui te fait dire ça ?", "{X}… je le prends comment, moi ?"),
        rule("^pourquoi (.+)",
            "Bonne question. Tu en penses quoi, toi ?", "Pourquoi {x}… j'ai ma petite idée, mais dis-moi la tienne d'abord.", "Je me le demande aussi. Ton avis ?"),
        // Ce que le conducteur dit de lui.
        rule("^(?:j'en ai marre|j'en peux plus|ça me saoule|ça m'énerve|ça me gonfle|ras le bol|j'en ai ras le bol)",
            "Qu'est-ce qui te saoule le plus là-dedans ?", "Vas-y, lâche tout, je t'écoute.", "Ça fait longtemps que ça dure ?", "Et qu'est-ce qui te ferait du bien, là, tout de suite ?"),
        rule("^je suis (?:allé|allée|parti|partie|passé|passée|rentré|rentrée|sorti|sortie|resté|restée|venu|venue) (.+)",
            "Et c'était comment ?", "Ah ouais ? Tu y vas souvent ?", "Raconte, il s'est passé quoi ?"),
        rule("^(?:je me sens|je suis) (.+)",
            "Depuis quand tu es {x} ?", "Qu'est-ce qui fait que tu es {x} ?", "Tu es {x}… raconte-moi pourquoi."),
        rule("^j'ai (?:trop |vraiment |grave )?(?:envie|besoin) (?:de |d')(.+)",
            "Qu'est-ce qui te donne envie {de} ?", "Et qu'est-ce qui t'empêche {de} ?", "Si tu pouvais, là, tout de suite, tu le ferais ?"),
        rule("^(?:je veux|je voudrais|j'aimerais bien|j'aimerais) (.+)",
            "Qu'est-ce qui t'empêche {de} ?", "Pourquoi tu veux {x} ?", "Et si tu le faisais vraiment, ça donnerait quoi ?"),
        rule("^(?:je pense|je crois|je trouve|je me dis) (?:que |qu')(.+)",
            "Qu'est-ce qui te fait dire {que} ?", "Ah ouais, {x} ? Développe.", "Tu as toujours pensé ça ?"),
        rule("^j'ai peur (?:de |d'|que |qu')(.+)",
            "Qu'est-ce qui te fait peur là-dedans ?", "Et si ça arrivait, tu ferais quoi ?", "C'est déjà arrivé ?"),
        rule("^j'ai mal (.+)",
            "Aïe. Ça fait longtemps que tu as mal {x} ?", "Mince. Tu as mal {x} depuis quand ?"),
        rule("^(?:je dois|il faut que je|faut que je) (.+)",
            "C'est pour quand ?", "Tu en as envie, ou c'est une corvée ?", "Et si tu ne le faisais pas, il se passerait quoi ?"),
        rule("^je vais (.+)",
            "Ah ouais, tu vas {x} ? C'est prévu pour quand ?", "Tu vas {x}… avec qui ?", "Bonne idée. Tu fais ça souvent ?"),
        rule("^(?:je ne sais (?:pas|plus)|je sais (?:pas|plus)|j'sais pas|aucune idée|j'en sais rien)",
            "Qu'est-ce qui te fait hésiter ?", "Dis la première chose qui te vient.", "On a le temps. Réfléchis à voix haute."),
        rule("^j'ai ((?:un|une|des|du|de la|deux|trois|plein de|trop de|pas mal de) .+)",
            "Ah, tu as {x} ? Raconte.", "{X} ? Et c'est plutôt une bonne nouvelle ?"),
        rule("^j'ai (\\p{L}+é(?![\\p{L}]).*)",
            "Tu as {x} ? Et alors, c'était comment ?", "Ah ouais, tu as {x} ? Raconte-moi la suite."),
        rule("^parce (?:que |qu')(.+)",
            "Ah, c'est pour ça. Et ça change quoi pour toi ?", "Je comprends mieux. Et maintenant ?"),
    )

    private val choiceStart = Regex("^(?:(?:est-ce que )?tu (?:préfères|préférerais|aimes mieux|choisis|prends)|plutôt|c'est mieux|vaut mieux) ")
    private val firstPerson = Regex("(?<![\\p{L}'])(?:je|j'|me|m'|moi|mon|ma|mes|on|nous)(?![\\p{L}])")

    /**
     * Phrases possibles qui reprennent ce que le conducteur vient de dire (au choix), ou null si aucune règle ne
     * s'applique. [lastResort] : le pack n'a rien trouvé — on essaie aussi les règles de secours et, une fois sur
     * deux, le simple « miroir » (« ma copine est partie » → « Ta copine est partie ? »).
     */
    fun reflect(raw: String, lastResort: Boolean = false): List<String>? {
        val t = tidy(raw)
        val words = t.split(' ').size
        if (words < 2) return null
        // « A ou B ? » : Pilote choisit.
        val ou = t.indexOf(" ou ")
        if (ou > 0 && (choiceStart.containsMatchIn(t) || words <= 7)) {
            val a = swap(t.substring(0, ou).replace(choiceStart, "").trim())
            val b = swap(t.substring(ou + 4).trim())
            if (a.isNotEmpty() && b.isNotEmpty() && a.length <= 40 && b.length <= 40) {
                val pickA = Math.random() < 0.5
                val mine = if (pickA) a else b
                return listOf("Moi je dis $mine. Et toi ?", "Sans hésiter, $mine. Tu es d'accord ?", "Dur… allez, $mine. Tu choisis quoi, toi ?")
            }
        }
        for (r in rules) {
            if (r.late && !lastResort) continue
            val m = r.re.find(t) ?: continue
            val x = if (m.groupValues.size > 1) swap(m.groupValues[1].trim()) else ""
            // Morceau trop long à répéter (ou absent) : on ne garde que les phrases qui ne le reprennent pas.
            val usable = if (x.isEmpty() || x.split(' ').size > 9) r.templates.filter { '{' !in it } else r.templates
            if (usable.isNotEmpty()) return usable.map { fill(it, x) }
        }
        if (lastResort && Math.random() < 0.5 && words in 4..14 && firstPerson.containsMatchIn(t)) {
            val s = cap(swap(t))
            return listOf("$s ? Raconte-moi.", "$s… et tu le vis comment ?", "$s ? Et après, il s'est passé quoi ?")
        }
        return null
    }
}
