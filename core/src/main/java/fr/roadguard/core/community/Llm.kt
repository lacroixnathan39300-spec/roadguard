package fr.roadguard.core.community

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Pont vers une IA qui tourne sur le téléphone (hors ligne). Le cœur (module core) ne dépend d'aucune bibliothèque
 * d'IA : c'est l'appli (gps) qui fournit un moteur via [engine]. Si aucun moteur n'est prêt, le mode conversation
 * reste sur ses réponses habituelles (pack + règles). Le moteur ne doit jamais faire planter l'appli : toute erreur
 * se traduit par un `null`, et Pilote retombe sur le pack.
 */
interface LlmEngine {
    /** Le modèle est chargé et prêt à répondre. */
    fun ready(): Boolean
    /** Réponse du modèle à la conversation, ou null si indisponible/échec. [messages] = (rôle "user"/"assistant", texte). */
    fun reply(system: String, messages: List<Pair<String, String>>): String?
}

object Llm {
    /** Moteur fourni par l'appli (null tant qu'aucune IA n'est installée). */
    @Volatile var engine: LlmEngine? = null
    /** Action fournie par l'appli pour télécharger/installer le modèle. */
    @Volatile var installHook: ((Context) -> Unit)? = null
    /** Message d'état affiché dans les réglages (téléchargement, prêt, erreur). */
    var status by mutableStateOf("")
    /** Vrai une fois le modèle chargé et prêt. */
    var installed by mutableStateOf(false)

    fun ready(): Boolean = engine?.ready() == true
    fun install(context: Context) { installHook?.invoke(context) }
}
