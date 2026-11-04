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
        override fun onCreateScreen(intent: Intent): Screen = CarHomeScreen(carContext)
    }
}
