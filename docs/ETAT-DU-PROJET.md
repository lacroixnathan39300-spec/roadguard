# ROADGUARD : état du projet (pour reprendre dans une nouvelle conversation)

Dernière mise à jour : 5 novembre 2026, étape 2 (build 150+).

## Les applis
- **GPS** (`fr.roadguard.gps`, dossier `gps/`) : appli téléphone : carte, radars, guidage, Link, assistant caméra.
- **DRIVE** (`fr.roadguard.drive`, dossier `drive/`) : appli autoradio (alps 8227L, Android 8.1, 1024×600), accueil façon Car Launcher Pro.
- **core/** : code partagé (session de conduite, carte MapLibre, radars, comptes, écrans communs).
- **server/** : serveur Cloudflare (Worker + base D1 `roadguard-eu` + KV `roadguard-apps`) : communauté, comptes, mises à jour des applis, plantages.
- **link-protocol/** : ROADGUARD Link (téléphone ↔ autoradio).

## Règles à respecter
- Tout en français pour Nathan.
- **Une seule compilation par lot de changements** (budget GitHub Actions bloqué à 5 $). Les changements serveur seuls ne recompilent pas les applis.
- Ne jamais publier ni écrire en clair les clés (`keys/*.enc`, déchiffrées en CI avec le secret `RG_KEYS_PASSPHRASE`).
- Garder au plus 2 versions publiées (GitHub et serveur) : c'est automatique.
- Les essais émulateur (`emulator.yml`, lancement manuel) coûtent des minutes : seulement si utile.

## Publication
- Chaque envoi sur `main` → `android.yml` : compile, tests, version GitHub `build-N`, APK sur le serveur (mise à jour depuis les applis), sauvegarde ZIP des sources sur le serveur (2 gardées).
- `server.yml` : met le serveur en ligne et applique les migrations D1.
- `diag.yml` (manuel) : liste ou supprime les comptes, teste création/connexion.

## Comptes
- Premier compte créé = administrateur (`thepilot39`, Nathan).
- Essai 1 mois (un seul par appareil), puis appli bloquée ; l'administrateur gère tout dans Réglages → Gérer les comptes.

## Fait
Radars + bips (20 m après le radar), vitesse moyenne, guidage, communauté, Link, bulle, comptes, accueil DRIVE en pages/modules/widgets,
gestes sur les bords, tolérance de vitesse, annonce des limitations, signalement vocal, ordinateur de bord, carnet d'entretien,
rappel de pause, assistant caméra (distance, collision, départ au feu), rapports de plantage, position réseau en attendant le GPS,
style de carte ROADGUARD jour/nuit (tools/map/gen_style.py), carte Waze en direct (WebView, ouvrir/fermer), Waze en écran partagé,
bouchons détectés automatiquement (conducteurs au pas), alertes radar 30 s avant et 40 m après.

## À faire (prochaines étapes)
- **Reste de l'étape 2** : zones écoles, vitesse à tenir sur les tronçons, styles de compteur, partage de trajet, itinéraire qui évite les bouchons, bulle améliorée.
- **Étape 3** : indication des voies, prix des carburants, OBD2, commandes vocales « OK ROADGUARD », affichage tête haute, groupes d'amis.
- À valider en voiture : position (build 147+), accueil DRIVE et widgets, connexion sur l'autoradio, assistant caméra.
