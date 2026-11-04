# ROADGUARD — feuille de route

Objectif : deux applis (GPS sur le téléphone, DRIVE sur l'autoradio), entièrement à Nathan,
fiables sur l'autoradio de référence (alps 8227L, 2 Go, API 27, ARM 32 bits) et le Xiaomi 15T Pro.

Règle : une fonction n'est « terminée » qu'après compilation par la CI, tests automatiques
et essai sur l'appareil réel.

## Phase 0 — Fondations
- [x] Dépôt unique : `gps/`, `drive/`, `site/`, archives séparées.
- [x] Compilation automatique (GitHub Actions) avec APK téléchargeables.
- [ ] Dépôt GitHub privé créé et poussé.
- [ ] Clés : récupérer la clé GPS d'origine, sinon en créer une neuve (une réinstallation).
      Nouvelle clé propre pour DRIVE 2.
- [ ] Versions finales (R8, sans débogage) pour les deux applis.

## Phase 1 — DRIVE 2, base du launcher (écrit de zéro)
- [x] Accueil paysage : zone carte, horloge, compteur rond (GPS du poste), musique, lanceur d'applis.
- [x] Carte native MapLibre Android (pas de WebView) avec radars et position (build 9).
- [x] Alertes radars à l’approche (bip + voix + bandeau), dans l’axe et dans le bon sens (build 14).
- [x] Vitesse moyenne reconnue en roulant, verdict OK / trop vite en fin de tronçon (build 14). Corrigé aussi dans le GPS : 0 tronçon français n’était reconnu.
- [x] Carte hors ligne native : packs PMTiles (téléchargés via « Plus »), style et polices embarqués, jour/nuit, choix du pack à la frontière (build 47).
- [x] Musique : lecteur en cours (MediaSession), pochette, commandes, égaliseur mémorisé (build 15).
- [ ] Waze ou autre appli dans une fenêtre à côté de la carte (écran partagé / superposition).
- [x] Navigation dans DRIVE : recherche d'adresse (Photon), itinéraire (Valhalla FOSSGIS, consignes en français), tracé, flèche + distance, voix à l'approche et au moment de tourner, recalcul automatique, destinations récentes (build 20).
- [x] Plein écran façon tableau de bord, bouton recentrer.
- [x] Maison / Travail, limite de vitesse de la route + alerte de dépassement, thème de nuit automatique (build 23).
- [x] Réglages : voix, bip, types d'alertes, dépassement, thème (build 25).
- [x] Choix entre 3 itinéraires (variantes grises sur la carte), éviter péages/autoroutes, couleur d'accent et taille des boutons.
- [ ] Favoris illimités.
- [x] Démarrage automatique avec le poste (démarrage normal et veille rapide Autochips), DRIVE peut être l'écran d'accueil.

## Phase 2 — Cœur partagé `core`
- [x] Cœur commun `core/` (Kotlin) : carte native, radars, alertes, vitesse moyenne, limites, navigation vocale, musique, réglages, et `DrivingSession` qui pilote tout. Utilisé par DRIVE et par le nouvel accueil du GPS (build 43).
- [x] Nouvel accueil de l'appli GPS sur ce cœur ; anciennes fonctions accessibles par « Plus ».
- [x] Alertes, vitesse moyenne et guidage vocal écran éteint (service en arrière-plan, build 47).
- [x] Écran « Hors ligne » natif (GPS et DRIVE) : 93 packs de cartes, téléchargement avec pause/reprise, vérification SHA-256, contrôle de l'espace, suppression ; le poste peut télécharger à travers le téléphone (build 50).
- [x] Radars de 20 pays supplémentaires (MapAtlas, CC BY 4.0), rechargés à chaud dans la carte et les alertes.
- [x] Radars perso : ajout à ma position, import CSV (iGO, tableur), liste, suppression.
- [ ] Signalements communautaires, compte du site.
- [ ] Découper `MainActivity.java` (460 Ko) en écrans et services lisibles, avec tests.

## Phase 3 — ROADGUARD Link : « notre Android Auto »
Le téléphone (appli GPS) fait tout le travail ; le poste (DRIVE) affiche et renvoie le tactile.
- [x] Protocole commun chiffré (association par code à 6 chiffres, AES-256-GCM, anti-rejeu), testé (build 30).
- [x] Téléphone : écran « ROADGUARD Link », service en arrière-plan, envoi position GPS et musique, commandes musique.
- [x] Autoradio : recherche du téléphone (partage de connexion / réseau local), reconnexion automatique, GPS et musique du téléphone utilisés par DRIVE.
- [x] Messages et appels du téléphone sur le poste, avec leurs boutons (répondre, refuser…), lecture vocale, destination partagée depuis Google Maps / Waze / Contacts.
- [x] Mode miroir : écran du téléphone (Waze…) en H.264 matériel, tactile renvoyé par service d'accessibilité, adaptation à la rotation (build 34).
- [x] Liaison par câble USB (mode accessoire Android, recharge du téléphone), priorité au câble, Wi-Fi en secours (build 38).
- [x] Internet du poste par le téléphone : carte, recherche, itinéraires, limites passent par la liaison si le poste n'a pas Internet.
- [x] Son du téléphone par la liaison (option « Écran + son »), Bluetooth en secours.
- [ ] Wi-Fi créé par le poste avec connexion automatique du téléphone (limité par Android 8.1 : identifiants changeants).
- [ ] Mode voiture ROADGUARD : écrans conçus pour le poste (paysage, gros boutons) dessinés par le
      téléphone sur un écran virtuel à la résolution du poste, puis envoyés en vidéo H.264.
      Le téléphone peut rester verrouillé dans la poche.
- [ ] Écrans : carte et guidage, radars et vitesse moyenne, musique de n'importe quelle appli
      (Spotify, YouTube Music… via la session média), appels, messages lus à voix haute.
- [ ] Tactile : les appuis sur le poste reviennent directement dans les écrans ROADGUARD.
- [ ] Mode miroir (secours) : l'écran du téléphone tel quel, pour Waze et les autres applis
      (capture d'écran autorisée + service d'accessibilité pour le tactile).
- [ ] Connexion : câble USB (protocole accessoire ouvert, recharge le téléphone) ou Wi-Fi
      (partage de connexion), association par QR, canal chiffré, reconnexion automatique.
- [ ] Son par le Bluetooth du poste ou dans le flux USB.

## Phase 4 — Navigation de haut niveau
- [x] Itinéraires en ligne (Valhalla FOSSGIS) et **hors ligne** (GraphHopper 5.3, graphe OSM fabriqué par GitHub Actions) avec repli automatique ; Franche-Comté embarquée, vérifiée par un vrai trajet Lons → Champagnole (35,4 km) (build 56).
- [ ] Autres régions hors ligne : dépôt public de données à créer pour les publier.
- [ ] Recherche d'adresses hors ligne, Maison/Travail, favoris.
- [ ] Voix de guidage, voies conseillées, limites de vitesse affichées, recalcul automatique.
- [ ] Vitesse moyenne sur les tronçons, indication bon/pas bon jusqu'à la fin de la zone.

## Phase 5 — Extensions
- [x] Android Auto (Car App Library) réécrit sur le cœur commun : carte MapLibre, guidage, radars, vitesse, Où aller (maison, travail, récents, recherche). À essayer en voiture (appli hors Play Store : activer « sources inconnues » dans les réglages développeur d Android Auto).
- [ ] Signalements communautaires synchronisés avec le site.
- [ ] Mises à jour des applis depuis le site.

## Phase 6 — Validation réelle
- [x] Essai automatique en émulateur (GitHub Actions, lancement manuel) : poste API 27 1024×600 et téléphone, trajet simulé vers un radar, captures et journal. A trouvé 2 plantages (DRIVE au lancement, carte au changement de style), corrigés.
- [ ] Essais sur le poste et le téléphone : démarrage, GPS, carte, radars, liaison, musique.
- [ ] Mesures mémoire et fluidité sur 2 Go / ARM 32 bits, corrections.
