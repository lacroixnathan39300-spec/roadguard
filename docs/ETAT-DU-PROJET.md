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
- Drive / Link (relecture complète) : trames limitées à 8 Ko avant authentification (`MAX_HANDSHAKE_FRAME`, essai réel OK),
  4 connexions au plus côté téléphone, tactile du miroir accepté seulement pendant un partage d'écran, adresses locales IPv6/CGNAT
  refusées par le relais Internet, message Link mal formé ignoré (plus de coupure), USB relancé après 20 s de silence,
  son du téléphone sans plantage possible (`AudioPlayer`), widgets Android libérés par « Par défaut ».
- Ton « Coquin XXL » (`VoicePack.XXL = 5`, section `xxl` de `script.json`, 58 groupes, 298 phrases sur 460 de la bibliothèque fournie,
  18 fautes d'accord corrigées). Variante conducteur / conductrice (`m` / `f`), accord de la voix d'homme (`speakerMale`).
  Phrase dite seulement si elle correspond à une donnée réelle : manœuvre Valhalla (droite, gauche, tout droit, sortie 1 à 5 de rond-point,
  demi-tour, sortie, insertion, arrivée), radar par type (+ limite toujours dite), limitation, écart chiffré exact, retour sous la limite,
  vitesse moyenne, signalements (police, radar mobile, bouchon, travaux, accident, danger confirmé), dangers OSM, météo, recalcul,
  distances restantes, fin de trajet, GPS perdu / retrouvé, batterie, surprises (25 à 45 min). Sinon : annonce habituelle.

## Ménage (07/11/2026)
- Petite appli `menage/` (`fr.roadguard.menage`, minSdk 30, sans bibliothèque) : doublons (empreinte SHA-1, original gardé),
  gros fichiers (> 50 Mo), rafales (< 2 s, même dossier), captures d'écran (cochées après 30 jours), messageries, vieux
  téléchargements (> 90 jours), APK. Corbeille d'Android (`MediaStore.createTrashRequest`, récupérable 30 jours) ; documents/APK
  (accès « tous les fichiers ») supprimés après confirmation.
- Compilée seule par `.github/workflows/menage.yml` (signée avec la clé du téléphone), publiée par `publish_updates.py`
  (contrôle de version appli par appli). Lien direct : `/menage`. Appli indépendante : PAS dans Pilote Suite ni dans les menus de Pilote (demande de Nathan).
- Corrections de carte (types `limite`, `rond_point`, `route`, `ReportType.mapFix`) : mêmes règles de confiance que les radars
  (placement, votes « c'est juste ? », attente pour les nouveaux). Limitation liée à la portion OSM (`edge.way_id`,
  `SpeedLimits.lastWay`), appliquée par `Community.limitFix` (validée, ou la sienne tout de suite). Confirmées (2 conducteurs ou
  validées d'office) : envoyées en note OpenStreetMap par la tâche planifiée (`forwardToOsm`, colonne `osm_note`, migration 0011).
- Sirène plus présente : `LoudnessEnhancer` +10 dB, musique en pause (focus transitoire), volume d'alarme au maximum pendant le réveil.

## Avertissement légal (07/11/2026, version 2 du texte)
- `core/ui/LegalUi.kt` : 4 écrans montrés AVANT tout (connexion comprise) dans `AccountGate`, puis à chaque nouvelle version
  du texte (`Legal.VERSION`, préférences `legal`) : la loi au volant ; les alertes radars selon les pays ; les autorisations
  (texte différent pour Drive) ; l'engagement (code de la route, téléphone, conditions lues — texte complet dans l'appli —,
  majorité ; « Je refuse et je quitte »). Relisible : Réglages → « Infos légales ».
- **Choix de Nathan : radars gardés, mais en opt-in.** Alertes radars COUPÉES par défaut ; elles ne s'allument que si
  l'utilisateur coche lui-même la case d'activation (texte : interdites en France, en Suisse…, il en assume les conséquences).
  `DriveSettings.radarsOn` = préférence ET `Legal.radarConsented` : aucun chemin (réglages, bouton de l'accueil, commande
  vocale, sauvegarde restaurée) ne peut les allumer sans cette case ; sinon `Legal.askRadars` affiche la confirmation
  (`RadarConsentScreen`). Couche radars d'Android Auto et menu « Ancienne version » (radars de l'ancienne appli) suivent aussi.
- Preuve : `Legal.sync` → `POST /v1/legal/accept` (date, version, choix radars, appareil, compte, appli, version) → table
  `legal_acceptances` (migration 0012), une ligne par appareil/version/choix/compte ; renvoyée après connexion.
- Serveur : `/conditions` (conditions d'utilisation, mêmes textes que `TERMS` dans LegalUi.kt : les garder identiques),
  `/confidentialite` complétée ; interrupteur administrateur `/v1/mod/police` (config `police_off_until`) qui masque et
  refuse les signalements « police » (interdiction préfectorale, art. L130-11 / L130-12 : 2 ans, 30 000 €). Bouton dans l'appli :
  Communauté (modération) → onglet « ⚖ Préfecture » (2 h, 12 h, 24 h, jusqu'à nouvel ordre, rétablir).
- Risques expliqués à Nathan : utilisateurs en France (contravention 5e classe), relais « police » si la préfecture notifie
  une interdiction, Suisse (art. 98a LCR : avertir publiquement des contrôles est puni), Play Store. Proposé (refusé pour
  l'instant) : mode France « zones de danger ». Refusé : renommer « radar » en « danger » pour contourner la loi.
- **Suisse (choix de Nathan : activables mais protégé)** : « mode Suisse » activé par défaut (radars coupés en Suisse) ;
  le couper exige une case dédiée (`SwissConsentScreen`, `Legal.swissConsented`, preuve avec `swiss`, migration 0013).
  Les signalements police / radars de la communauté ne sont JAMAIS relayés en Suisse (art. 98a LCR vise celui qui avertit
  publiquement) : `radarAlertsAllowed` faux en Suisse, `Community.shownReports` (carte sans radars/contrôles quand non
  permis), signalement radar/police refusé et masqué dans la feuille. Annonce vocale à l'entrée en Suisse si gardés.
- **Version 3 du texte : phrases écrites par l'utilisateur** (preuve plus forte qu'une case) : « J'accepte » pour valider,
  « J'active les radars à mes risques » pour les radars, « Je coupe le mode Suisse à mes risques » pour la Suisse
  (`typedMatches` : sans tenir compte des majuscules, accents, apostrophes ; test `TypedConsentTest`). Phrases gardées
  dans les préférences `legal` et envoyées au serveur (colonne `typed`, migration 0014).
- **Version 4 du texte (choix de Nathan : « activés pour tous à J'accepte »)** : radars ACTIVÉS à l'acceptation, sauf si
  l'utilisateur coche « Je préfère les couper » à l'étape 2 ; s'ils restent activés, l'étape 4 exige une case de
  responsabilité (`RADAR_ACK`) en plus de « J'accepte » écrit. Réactivation plus tard (s'ils ont été coupés à
  l'acceptation) : case + « J'active les radars à mes risques ». Correctif : `DriveSettings` écoute aussi les préférences
  `legal` (l'accord s'applique tout de suite à la séance en cours, même si `radars_on` valait déjà vrai).

## Téléchargements plus visibles (07/11/2026)
- Pilote Suite : bloc « ⬇ En cours de téléchargement… » dans la carte de l'appli (grosse barre de 18 dp à la couleur de
  l'appli, pourcentage, Mo reçus sur le total), puis « Vérification du fichier » et « Installation en cours » ;
  progression au fil des octets (et non plus par morceau de 20 Mo), 3 essais par morceau, écran maintenu allumé,
  un seul téléchargement à la fois.
- Pilote / Pilote Drive (mise à jour intégrée) : `DownloadProgress` (même présentation) dans le bandeau et dans
  Réglages ; `Net.fetchWithProgress` (progression en direct ; par le relais du téléphone, morceau d'un coup).
