# Publier Pilote sur le Play Store — préparation

## Ce qui est prêt
- Politique de confidentialité en ligne : https://roadguard.roadguard-app.workers.dev/confidentialite
- Applis signées (clé GPS / clé Drive), numéros de version croissants, mises à jour automatiques hors Store (à couper pour la version Store : le Play Store interdit l'auto-installation d'APK).

## À faire par Nathan
1. Créer le compte développeur Google Play (25 $ une fois) : https://play.google.com/console
2. Vérification d'identité (pièce d'identité, adresse) demandée par Google.
3. Me donner le feu vert pour produire la version « Store » (fichier AAB) : je la prépare sans l'installateur intégré.

## Points sensibles de la politique Google (à déclarer ou adapter)
| Fonction | Exigence Play | Adaptation prévue |
|---|---|---|
| SMS d'urgence (accident) | Permission SMS réservée aux applis SMS par défaut | Version Store : le SMS s'ouvre prêt à envoyer (un appui), ou appel au contact |
| Lecture des messages | Accès aux notifications : déclaration + justification | Garder, déclarer « aide à la conduite » |
| Position en arrière-plan | Formulaire + vidéo de démonstration | Démarrage auto à 20 km/h / bulle |
| Affichage par-dessus les autres applis | Autorisé, à justifier | Bulle sur Waze |
| Alertes radars | Légal en France (zones de danger), interdit en Suisse et dans d'autres pays | Coupé auto en Suisse ; pays à exclure de la diffusion |
| Caméra (dashcam, anti-somnolence) | Déclaration « Sécurité des données » | Analyse locale, rien n'est envoyé |

## Fiche du Store (proposition)
**Nom** : Pilote — GPS, radars et communauté

**Description courte** : GPS, alertes de zones de danger, limitations, signalements entre conducteurs, anti-somnolence.

**Description** :
Pilote réunit tout ce qu'il faut pour conduire sereinement :
- Guidage GPS, avec étapes, hors ligne et sans péage.
- Zones de danger et limitations de vitesse, alertes sonores, mode pluie et jeune conducteur.
- Signalements de la communauté (police, accident, bouchon, travaux) avec note de fiabilité.
- Anti-somnolence par la caméra, détection d'accident, dashcam.
- Carburants les moins chers, bornes, parkings, aires.
- Historique des trajets, éco-conduite, journal kilométrique pro/perso.
- Bulle par-dessus Waze ou Google Maps, widget, Android Auto et autoradio (Pilote Drive).

**Catégorie** : Cartes et navigation · **Public** : 18 ans et plus (tons de voix « osé »).
