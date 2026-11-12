package fr.roadguard.core.radar

/**
 * Niveaux du bouton son (haut-parleur de l'accueil) : chaque niveau garde ceux du dessous.
 *  - [ALL] tout : guidage, limitations, radars, signalements et le reste ;
 *  - [ALERTS] alertes : radars, limitations et signalements, sans le guidage ni les petites phrases ;
 *  - [RADARS] radars seuls (et contrôles signalés) ;
 *  - [MUTE] muet : plus aucun son, sauf la sécurité (choc, somnolence, oubli à l'arrière) et les réponses demandées.
 */
object SoundLevel {
    const val MUTE = 0
    const val RADARS = 1
    const val ALERTS = 2
    const val ALL = 3

    fun emoji(level: Int) = when (level) { ALL -> "🔊"; ALERTS -> "🔉"; RADARS -> "🔈"; else -> "🔇" }
    fun caption(level: Int) = when (level) { ALL -> "Tout"; ALERTS -> "Alertes"; RADARS -> "Radars"; else -> "Muet" }
    fun title(level: Int) = when (level) { ALL -> "Tout"; ALERTS -> "Alertes seulement"; RADARS -> "Radars seuls"; else -> "Tout couper" }

    /** Ce qu'on entend à ce niveau ([radars] : alertes radars activées). */
    fun detail(level: Int, radars: Boolean) = when (level) {
        ALL -> "Guidage, limitations, " + (if (radars) "radars " else "") + "et signalements"
        ALERTS -> (if (radars) "Radars, limitations" else "Limitations") + " et signalements, sans le guidage"
        RADARS -> "Seulement les radars et les contrôles"
        else -> "Plus aucun son (sauf choc et somnolence)"
    }

    /** Niveaux proposés : « radars seuls » une fois les alertes radars activées (et acceptées). */
    fun choices(radars: Boolean) = if (radars) listOf(ALL, ALERTS, RADARS, MUTE) else listOf(ALL, ALERTS, MUTE)
}
