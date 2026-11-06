package fr.roadguard.app.next

import fr.roadguard.core.DrivingSession
import fr.roadguard.core.ui.reportFromSpeech

/** Ce que l'accueil sait ouvrir à la voix. */
interface VoiceActions {
    fun openParking(); fun openNearby(); fun openSearch(query: String?); fun openTrips(); fun openHud()
}

/**
 * Commandes vocales (« Pilote, … ») : signaler, couper ou remettre les radars et les limitations,
 * rentrer à la maison, chercher un lieu, carburant le moins cher, où est ma voiture, arrêter le guidage…
 * Renvoie la réponse dite à voix haute.
 */
object VoiceCommands {
    private fun norm(s: String) = java.text.Normalizer.normalize(s.lowercase(), java.text.Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").trim()

    fun handle(raw: String, s: DrivingSession, a: VoiceActions): String {
        val t = norm(raw).removePrefix("pilote").trim(' ', ',')
        val off = listOf("coupe", "desactive", "arrete", "stop", "enleve", "silence").any { it in t }
        val on = listOf("active", "remet", "rallume", "allume", "reactive").any { it in t }
        val st = s.settings
        return when {
            "radar" in t && (off || on) && !("signal" in t) -> { st.changeRadarsOn(on && !off); if (st.radarsOn) "Alertes radars activées" else "Alertes radars coupées" }
            ("limit" in t || "vitesse" in t) && (off || on) -> { st.changeLimitVoice(on && !off); if (st.limitVoiceOn) "Annonces de limitation activées" else "Annonces de limitation coupées" }
            ("guidage" in t || "navigation" in t || "itineraire" in t) && off -> { s.stopNavigation(); "Guidage arrêté" }
            "signal" in t || listOf("police", "accident", "bouchon", "travaux", "danger", "flash").any { it in t } -> {
                val type = reportFromSpeech(t) ?: return "Je n'ai pas compris quoi signaler"
                s.community.report(type); "${type.label} signalé"
            }
            "voiture" in t && ("ou" in t || "garee" in t || "retrouve" in t) -> { a.openParking(); "Voici où est garée ta voiture" }
            listOf("essence", "carburant", "gasoil", "gazole", "station", "plein").any { it in t } -> { a.openNearby(); "Voici les carburants les moins chers autour de toi" }
            "maison" in t || "chez moi" in t -> s.home?.let { s.startNavigation(it); "En route vers la maison" } ?: "Enregistre d'abord ton adresse de domicile"
            "travail" in t || "boulot" in t -> s.work?.let { s.startNavigation(it); "En route vers le travail" } ?: "Enregistre d'abord ton adresse de travail"
            t.startsWith("va a ") || t.startsWith("aller a ") || t.startsWith("emmene moi a ") || t.startsWith("direction ") -> {
                val q = raw.substringAfter(" a ", raw.substringAfter("direction ", raw)).trim()
                a.openSearch(q); "Je cherche $q"
            }
            "trajet" in t || "historique" in t -> { a.openTrips(); "Voici tes trajets" }
            "tete haute" in t || "hud" in t -> { a.openHud(); "Affichage tête haute" }
            "heure" in t -> "Il est " + java.text.SimpleDateFormat("HH 'heures' mm", java.util.Locale.FRANCE).format(java.util.Date())
            else -> "Je n'ai pas compris. Dis par exemple : signale la police, coupe les radars, rentre à la maison, où est ma voiture."
        }
    }
}
