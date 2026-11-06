# ROADGUARD : état du projet (pour reprendre dans une nouvelle conversation)

Dernière mise à jour : 5 novembre 2026, étape 2 (build 150+).

## Les applis (nom public : Pilote)
- **Pilote Suite** (`fr.roadguard.suite`, dossier `suite/`) : installateur sans Play Store, lien direct `https://roadguard.roadguard-app.workers.dev/suite`.
- **Pilote / GPS** (`fr.roadguard.gps`, dossier `gps/`) : appli téléphone : carte, radars, guidage, Link, assistant caméra.
- **Pilote Drive** (`fr.roadguard.drive`, dossier `drive/`) : appli autoradio (alps 8227L, Android 8.1, 1024×600), accueil façon Car Launcher Pro.
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
style de carte ROADGUARD jour/nuit (tools/map/gen_style.py),
bouchons détectés automatiquement, messages 💬 de 24 h, tutoriel au premier lancement, logos Pilote (tools/icon_gen.py) (conducteurs au pas), alertes radar 30 s avant, arrêt pile au radar (retard GPS compensé), alarme forte si excès devant un radar.

## À faire (prochaines étapes)
- **Reste de l'étape 2** : zones écoles, vitesse à tenir sur les tronçons, styles de compteur, partage de trajet, itinéraire qui évite les bouchons, bulle améliorée.
- **Étape 3** : indication des voies, prix des carburants, OBD2, commandes vocales « OK ROADGUARD », affichage tête haute, groupes d'amis.
- À valider en voiture : position (build 147+), accueil DRIVE et widgets, connexion sur l'autoradio, assistant caméra.

## Session du 06/11/2026 (builds 170 → 190 env.)
- Limitations interrogées toutes les 3 s ; recherche avec suggestions pendant la frappe.
- Accueil Pilote : interrupteurs 📷 Radars / 🚦 Limites (cachés en mode épuré), plein écran (barres Android masquées), mode épuré auto après 8 s en roulant.
- Signalements : pseudo communauté auto (identifiant Pilote), affichage immédiat.
- Bulle par-dessus Waze : démarrage auto dès 20 km/h (`AutoStart.kt`, position passive + réseau, permission arrière-plan), cachée à l'arrêt, carte d'activation sur l'accueil.
- Radars : `AlertEngine` = couloir + rapprochement réel + verrouillage, suivi du tracé en guidage, alerte à 1 km ; signalements à 1 km dans le couloir.
- Radars par pays : 100+ pays, MapAtlas + OpenStreetMap (Overpass) fusionnés.
- Voix : tons 0 classique → 4 extrême (`core/src/main/assets/voices/script.json`, cumulatifs), guidage au ton (`AlertVoice.guide`), réprimandes, voix d'autres langues. Voix réaliste/« Ma voix » retirées (code `VoicePack` conservé, `realistic=false`).
- Alarme : un seul ToneGenerator ; option alarme hors radar.
- Anti-somnolence invisible (caméra avant, MediaPipe Face Landmarker, `DrowsinessGuard.kt`, modèle téléchargé en CI).
- Messagerie utilisateur ↔ administrateur (migration 0007, `/v1/auth/message(s)`, `/v1/admin/messages`, `InboxNotifier`).
- Tutoriel v2.

## Audit du 06/11/2026 (soir)
- Sécurité (commit 3af4770) : plus de réinitialisation de mot de passe par l'admin, plus d'admin automatique au premier compte,
  `allowBackup=false` (Pilote et Drive), `diag.yml` sans messages privés ni suppression de comptes.
- Partage de trajet : mise à jour réservée au compte qui l'a lancé (migration 0010 `shares.owner`) ; le lien public ne sert qu'à regarder.
- Convoi : au plus 3 codes différents par compte et par heure (anti-devinette).
- Conduite : fin de trajet après 3 min d'arrêt (et non 1 min : bouchons), météo vérifiée 20 km DEVANT sur le tracé (`Guidance.pointAhead`),
  calcul d'itinéraire périmé ignoré (`routeRequest`), guidage arrêté à l'arrivée (plus de recalcul pour revenir), étape validée à 150 m.
- Voix : langue des annonces appliquée même si choisie avant que la synthèse soit prête ; « oui / non » à la voix écouté seulement
  après la question (`AlertVoice.speakThen`), phrase avec « oui » et « non » ignorée (le micro entendait la question).
- Communauté : police / radar mobile coupés avec les radars (interrupteur, Suisse) ; messages 💬 lus avec leur texte.
- Drive : tableau de bord avec cadres colorés, titre et icône par bloc, textes vides explicites.
- Reste à faire : vérification des en-têtes d'identité du site (plateforme ChatGPT Sites), « mot de passe oublié », essais en voiture.
