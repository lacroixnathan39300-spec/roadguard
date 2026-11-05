package fr.roadguard.app.car

import android.content.Intent
import androidx.car.app.CarAppService
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

/**
 * ROADGUARD dans Android Auto : même cœur de conduite que l'appli (radars, vitesse moyenne,
 * limites, guidage). Le service en arrière-plan du téléphone fait le travail, l'écran de la voiture l'affiche.
 */
class RoadguardCarService : CarAppService() {
    // Appli installée hors Play Store : tout hôte Android Auto est accepté.
    override fun createHostValidator(): HostValidator = HostValidator.ALLOW_ALL_HOSTS_VALIDATOR

    override fun onCreateSession(): Session = object : Session() {
        override fun onCreateScreen(intent: Intent): Screen =
            if (fr.roadguard.core.account.AccountManager.get(carContext).allowed) CarHomeScreen(carContext) else LockedCarScreen(carContext)
    }
}

/** Compte ROADGUARD sans accès (essai fini, bloqué, pas connecté) : message à la place de l'appli. */
class LockedCarScreen(context: androidx.car.app.CarContext) : Screen(context) {
    override fun onGetTemplate(): androidx.car.app.model.Template {
        val s = fr.roadguard.core.account.AccountManager.get(carContext).status
        val text = if (s == null) "Connecte-toi à ton compte ROADGUARD dans l'appli du téléphone."
            else "${s.label}. Demande à Nathan Lacroix d'activer ton compte" + (if (s.contact.isNotBlank()) " (${s.contact})." else ".")
        return androidx.car.app.model.MessageTemplate.Builder(text).setTitle("ROADGUARD")
            .setHeaderAction(androidx.car.app.model.Action.APP_ICON).build()
    }
}
