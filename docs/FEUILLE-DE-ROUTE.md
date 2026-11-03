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
- [ ] Carte native MapLibre Android (pas de WebView) avec radars et position.
- [ ] Musique : lecteur en cours (MediaSession), pochette, commandes, égaliseur.
- [ ] Waze ou autre appli dans une fenêtre à côté de la carte (écran partagé / superposition).
- [ ] Réglages, thèmes jour/nuit automatiques, couleurs, taille des boutons.
- [ ] Démarrage automatique avec le poste, retour à l'accueil.

## Phase 2 — Cœur partagé `core`
- [ ] Extraire du GPS : radars, vitesse moyenne, alertes, packs de cartes, recherche, favoris.
- [ ] Un seul exemplaire du code utilisé par GPS et DRIVE (fin de la copie `DRIVE/roadguard`).
- [ ] Découper `MainActivity.java` (460 Ko) en écrans et services lisibles, avec tests.

## Phase 3 — ROADGUARD Link : l'écran du téléphone sur l'autoradio (type CarbitLink)
Pas d'Android Auto : le téléphone fait tourner les applis (Waze, GPS ROADGUARD, musique…),
le poste affiche et renvoie le tactile.
- [ ] Téléphone (appli GPS) : capture d'écran autorisée (MediaProjection), encodage vidéo H.264
      matériel, débit et images/s adaptés au Wi-Fi.
- [ ] Autoradio (DRIVE) : réception et décodage matériel plein écran, faible latence.
- [ ] Tactile inverse : les appuis et glissés sur le poste pilotent le téléphone
      (service d'accessibilité du téléphone, avec accord explicite).
- [ ] Connexion : partage de connexion du téléphone ou Wi-Fi du poste, association par QR,
      canal chiffré, reconnexion automatique au démarrage de la voiture.
- [ ] Son par le Bluetooth du poste ; orientation paysage forcée pendant la projection.
- [ ] Alertes radars ROADGUARD superposées par-dessus Waze projeté.
## Phase 4 — Navigation de haut niveau
- [ ] Calcul d'itinéraire hors ligne (moteur embarqué) au lieu du serveur de démonstration OSRM.
- [ ] Recherche d'adresses hors ligne, Maison/Travail, favoris.
- [ ] Voix de guidage, voies conseillées, limites de vitesse affichées, recalcul automatique.
- [ ] Vitesse moyenne sur les tronçons, indication bon/pas bon jusqu'à la fin de la zone.

## Phase 5 — Extensions
- [ ] Android Auto (Car App Library) dans l'appli GPS.
- [ ] Signalements communautaires synchronisés avec le site.
- [ ] Mises à jour des applis depuis le site.

## Phase 6 — Validation réelle
- [ ] Essais sur le poste et le téléphone : démarrage, GPS, carte, radars, liaison, musique.
- [ ] Mesures mémoire et fluidité sur 2 Go / ARM 32 bits, corrections.
