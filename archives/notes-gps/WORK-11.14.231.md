# ROADGUARD 11.14.231 — Anti-doublon des alertes

- BackgroundAlertService.check(): le service ne génère plus d'alerte sonore/vocale lorsque la carte ROADGUARD est visible, même si le heartbeat GPS est momentanément absent.
- Lors du passage avant-plan/arrière-plan, délai de grâce GPS de 7 secondes maintenu ; protection supplémentaire contre horodatage futur.
- Arrêt des annonces vocales du service lors de la reprise par la carte.
- Aucune modification du moteur d'alertes de MainActivity, des coordonnées ou des bases radar.
- Limite : les distances différentes entre l'alerte de la carte et les annonces déjà affichées peuvent persister ; non validé sur téléphone.
- Compilation Android non effectuée.
