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
- [ ] Carte hors ligne (packs du GPS).
- [x] Musique : lecteur en cours (MediaSession), pochette, commandes, égaliseur mémorisé (build 15).
- [ ] Waze ou autre appli dans une fenêtre à côté de la carte (écran partagé / superposition).
- [x] Navigation dans DRIVE : recherche d'adresse (Photon), itinéraire (Valhalla FOSSGIS, consignes en français), tracé, flèche + distance, voix à l'approche et au moment de tourner, recalcul automatique, destinations récentes (build 20).
- [x] Plein écran façon tableau de bord, bouton recentrer.
- [ ] Maison / Travail / favoris, choix entre plusieurs itinéraires, limites de vitesse affichées.
- [ ] Réglages, thèmes jour/nuit automatiques, couleurs, taille des boutons.
- [ ] Démarrage automatique avec le poste, retour à l'accueil.

## Phase 2 — Cœur partagé `core`
- [ ] Extraire du GPS : radars, vitesse moyenne, alertes, packs de cartes, recherche, favoris.
- [ ] Un seul exemplaire du code utilisé par GPS et DRIVE (fin de la copie `DRIVE/roadguard`).
- [ ] Découper `MainActivity.java` (460 Ko) en écrans et services lisibles, avec tests.

## Phase 3 — ROADGUARD Link : « notre Android Auto »
Le téléphone (appli GPS) fait tout le travail ; le poste (DRIVE) affiche et renvoie le tactile.
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
