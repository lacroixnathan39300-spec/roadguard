# ROADGUARD — SUIVI ET REPRISE
Date : 27 octobre 2026, heure de France. Révision du suivi : 4.
Propriétaire : Nathan Lacroix.
ÉTAT COURANT RÉVISION4 : GPS11.14.294, DRIVE1.10.1, Compagnon1.1.1, site15. Le journal final révision4 ci-dessous fait autorité pour la reprise ; les étapes antérieures restent conservées comme historique.
Ce document accompagne un point de reprise des sources. Le joindre dans une nouvelle conversation avec la consigne : « Lis ce suivi et les sources jointes, vérifie l’état actuel, puis continue les travaux non terminés sans supprimer les bases radars. »

## 1. État à ne pas confondre
Versions actuelles compilées et vérifiées :
- Autoradio : DRIVE 1.10.1, versionCode 2001, ARM32 + ARM64, minSdk 27 ; signatures V1/V2 et certificat conservés, 41 tests DRIVE/protocole réussis, lint API sans problème.
- Smartphone GPS : 11.14.294, versionCode494, minSdk26, Studio audio et lecteurs musicaux ajoutés ;48tests réussis.
- Téléphone compagnon : 1.1.1, versionCode 111, minSdk 29.
- Le lot1.10.1 a été finalisé et publié le27octobre. L’APK et les sources canoniques sont sauvegardés ; téléchargements du site vérifiés par SHA256/taille/Range. Les passages historiques du journal marqués « en cours » décrivent des étapes antérieures, désormais dépassées.
Ne pas présenter les sources en cours comme une nouvelle application installable.

Le poste réel est alps / 8227L_demo, matériel ac8227l, 2 Go RAM, ARM armeabi-v7a/armeabi. Son API déclarée aux applications est 27, malgré les mentions Android 12/13 du firmware. L’APK universel DRIVE doit conserver minSdk 27 et ARM32 ; ARM64 sert aux autres postes. Le compagnon s’installe sur le téléphone, pas sur cet autoradio API27.

## 2. Livraisons persistantes et identités à conserver
Site : https://roadguard-community.lacroixnathan39300.chatgpt.site
Archives privées du propriétaire : /sauvegardes.
Préserver toutes les anciennes versions ; ne pas remplacer les bases embarquées par les téléchargements.

| Fichier | Identité persistante | Version observée |
|---|---|---|
| ROADGUARD-DRIVE-1.10.1-UNIVERSEL-API27.apk | libfile_ef5a58064bb8819189e3bfd85629670a | 4 |
| ROADGUARD-DRIVE-1.10.1-SOURCES.zip | libfile_1895bb79ab108191a655a4f3133b023c | 15 |
| ROADGUARD-GPS-11.14.294.apk | libfile_29e8023600108191a642e3c63f3e422e | 3 |
| ROADGUARD-11.14.294-PROJET-COMPLET.zip | libfile_231802beb1d48191902a45379ea149db | 19 |
| ROADGUARD-COMPAGNON-1.1.1.apk | libfile_4f056178f3608191b475437535018112 | 3 |
| ROADGUARD-COMPAGNON-1.1.1-SOURCES.zip | libfile_588705208b7081918fe1565f0208bc80 | 2 |
| ROADGUARD-DRIVE-1.10.1-CORRECTIF-APPAIRAGE.zip | libfile_5e41fb728e5081918b0a448e466f77e1 | 0 au moment de sa création |

Ces versions sont celles constatées, à relire avant tout remplacement avec contrôle de version. Les sources DRIVE et Compagnon partagent un seul projet Gradle, avec modules :app et :companion. Le GPS autonome possède son propre projet.

Sauvegarde antérieure : ROADGUARD-AUDIO-POINT-DE-REPRISE.zip, libfile_6642aaa3052c819182d3ae6b0d58e60e, version1 ; elle précède le correctif de capture Compagnon 1.1.1. Préférer les sources complètes ci-dessus.

## 3. Demandes et état réel
| Chantier | Développement constaté | Limites / reste |
|---|---|---|
| Boutons DRIVE | Actions câblées aux écrans/réglages natifs ; correctifs antérieurs conservés | Parcourir chaque bouton sur poste réel ; ne pas annoncer que tout fonctionne à partir d’une compilation |
| Personnalisation | 26 widgets, grille12x12, déplacement/taille, verrouillage, duplication hors carte, couleurs, polices, arrondis, opacité, image personnelle | Widgets intégrés seulement ; pas d’hébergement universel de widgets Android tiers ; essai tactile à faire |
| Dispositions | 4 dispositions, jusqu’à5 pages, portrait/paysage séparés, annuler/rétablir, import/export | Vérifier persistance après redémarrage et import sur appareil |
| Cartes locales d’abord | PMTiles et XYZ locaux prioritaires ; réseau pour les manques ; préchargement local et caches bornés | Réessayer hors ligne avec les cartes téléchargées de Nathan ; ne pas remplacer le moteur validé |
| Couleurs de carte | 6 palettes, 10 couleurs vectorielles, jour/nuit séparés ; accès DRIVE/GPS | Images raster : teinte globale seulement ; couleurs de Waze/Maps tiers non modifiables par ROADGUARD |
| APK universel | minSdk27, ARM32/64, signaturesV1/V2 | Installer sur 8227L ; ne pas se fier au numéro Android affiché |
| Bips progressifs | Départ zone1km ; intervalle se resserre ; son continu à50m ou moins ; tonalité aiguë si dépassement connu | Essai sonore réel et ducking avec MCU/Bluetooth à faire ; vitesse limite inconnue : aucune limite inventée |
| Voix dépassement | Tonalité distincte déjà intégrée | Annonce vocale explicite « dépassement de vitesse » non intégrée/confirmée dans ce lot ; demande disait son différent OU annonce |
| Égaliseur | AudioEffect réel sur sessions annoncées, 10 contrôles adaptés au nombre de bandes matériel, presets/basses/réserve de niveau | Pas de garantie DSP global ni qualité professionnelle sur MCU propriétaire ; matériel à tester |
| Musique téléphone | Source Téléphone et commandes appairées ; audio Bluetooth A2DP séparé | Accès notifications et capacités du lecteur requis ; vérifier sur téléphone/poste |
| Bulle Spotify | Mini lecteur déplaçable dans la carte GPS | Pas de bulle système au-dessus d’autres applications |
| Téléphone à côté carte | Capture MediaProjection consentie, JPEG480px, au plus3images/s, lien chiffré appairé, arrêt notification, masquage en déplacement | Aperçu à l’arrêt ; ni vidéo fluide ni contrôle tactile distant ; Netflix/surfaces protégées possiblement noirs |
| Premier appairage | Correctif source1.10.1 : autorisation relue dans la liste actuelle des pairs à chaque image | Compilation et3tests réussis dans1.10.1 ; essai réel encore à faire. Version1.10.0 : reconnecter après le premier appairage |
| Mise à jour applications | DRIVE et GPS interrogent le site, version disponible, téléchargement authentifié, SHA256/paquet/signature/version/minSDK vérifiés | Installation confirmée par Android ; Compagnon : APK fourni/partagé par DRIVE, pas de mise à jour autonome du site dans ce lot |
| Synchronisation radars | Signalements2–3min, inventaire~6h, jitter, recul exponentiel, Retry-After429/503, données locales conservées | Répartition des requêtes, pas de file centrale strictement un utilisateur après l’autre |
| Sauvegardes | Trois APK et sources en ligne ; anciennes versions conservées | Publier1.10.1 seulement après validation ; nouveau dossier de reprise en plus des sources canoniques |

Le tableau décrit le code récupéré et les rapports intégrés ; il ne remplace aucun essai utilisateur.

## 4. Correctif en cours : DRIVE1.10.1
Cause : PhoneLink.runSession reçoit un booléen isPending fixé au début de la connexion. Hello approuve le nouveau téléphone, mais l’ancien contrôle des images utilisait toujours !isPending et rejetait ses premières images jusqu’à reconnexion.
Correction :
- app/src/main/java/com/openauto/dash/PhoneScreenAuthorization.kt : recherche du pair dans la liste approuvée actuelle, comparaison de l’id ET du contenu du secret, refus des pairs oubliés.
- PhoneLink.kt : appel phoneScreenAllowed(phone, _phones.value) sur PhoneScreenFrame.
- app/build.gradle.kts : versionCode2001, versionName1.10.1-roadguard.
- Tests PhoneScreenAuthorizationTest : approbation pendant première connexion, oubli/suppression, id ou secret différents.
- roadguard/AUDIO-UPDATE-1.10.1.json décrit la modification.
Ne pas refaire ce correctif à partir de zéro : il est déjà inclus dans le point de reprise complet.

## 5. Preuves et tests
Rapport livré : verification/release-1.10.0/VERIFICATION.json et NOTES.md dans le projet DRIVE.
Dernier lot livré : 30 tests réussis, 0 échec/erreur :
- DRIVE ProgressiveRadarRuntimeTest : 8.
- DRIVE OreoMediaCompatibilityTest : 3.
- Link PhoneScreenProtocolTest : 3.
- GPS : Progressive8 + MapPalette6 + LocalFirstBootstrap2 =16.
Les3tests PhoneScreenAuthorizationTest ont réussi. Ce nouveau lot compte41tests DRIVE/protocole (dont18dispositions et6palettes), tous sans échec/erreur/ignoré. Les16tests GPS du lot précédent restent historiques, non relancés ici. Voir preuves/VERIFICATION-DRIVE-1.10.1.json dans le dossier complet.
Les rapports de version1.9.0 concernent les palettes/local-first et restent présents : ne pas mélanger les compteurs ni les attribuer à1.10.1.

Contrôles précédents : lint API DRIVE sans problème ; certificats conservés ; ZIPCRC ; ELF32/64 ; DEX compatibles ; compagnon embarqué identique à l’APK Compagnon111 ; vérification de155ressources de référence.
Ancien test statique PMTiles avec assertions littérales obsolètes : ne pas le compter comme réussi ; corriger sa méthode s’il est repris.
Jamais réalisé pour ce lot : installation réelle poste/phone, audio/DSP/ducking physique, capture vidéo Netflix, essais routiers.

## 6. Reprise technique
Sources dans ce dossier :
- sources/ROADGUARD-DRIVE-1.10.1 : projet commun complet, correctif appliqué.
- sources/ROADGUARD-11.14.294 : GPS autonome avec les ajouts audio mobiles.
Ne pas utiliser l’ancien projet1.6.2 éventuellement encore présent dans un espace de travail.

Outils : JDK21, Gradle8.13, AndroidSDK36, build-tools35.
AGP8.13.2, Kotlin2.1.0.
DRIVE : :app:assembleGithubDebug
Tests ciblés : :app:testGithubDebugUnitTest --tests com.openauto.dash.PhoneScreenAuthorizationTest --tests com.openauto.dash.OreoMediaCompatibilityTest --tests fr.roadguard.app.ProgressiveRadarRuntimeTest
Protocole : :link:test --tests com.openauto.dash.link.PhoneScreenProtocolTest
API : :app:lintGithubDebug -ProadguardApiLint
GPS : :app:assembleDebug
Donner à AAPT2 le chemin SDK35 via -Pandroid.aapt2FromMavenOverride si le binaire Maven est indisponible.
Sur environnement synchronisé : --max-workers=1 -Pkotlin.incremental=false --no-watch-fs. Ne pas appliquer doNotTrackState au packaging : cela casse InputChanges.
Les journaux ouverts peuvent tarder à refléter les écritures. Consulter aussi la sortie du processus et ses résultats finaux.
Si un fichier temporaire disparaît pendant le packaging, attendre l’arrêt puis nettoyer seulement le dossier de sortie/incrémental de la tâche concernée. Ne pas supprimer toute la base de sources.

Signature : les clés privées ne sont PAS incluses dans ce dossier. Ne jamais les exporter dans les archives publiques ni les recopier dans les sources. Les restaurer uniquement localement depuis les sauvegardes privées autorisées si nécessaire pour préserver les mises à jour. ROADGUARD_DEBUG_KEYSTORE et ROADGUARD_GPS_DEBUG_KEYSTORE sont les variables prévues. Ne pas fabriquer un APK de remplacement signé avec une nouvelle clé.

## 7. Site et publication
Projet Sites connu : appgprj_6ad30cbe059c8191bdc679d1ad6832c2.
Dernière version publiée :14, identifiant appgprj_6ad30cbe059c8191bdc679d1ad6832c2~appgver_aed243656720819183334d942ca0ce42.
Commit source36bcce7194f5ce78b3f95dd492ee6b01d79b0e42.
Audience publique du site à préserver ; téléchargements de sauvegarde protégés propriétaire.
API applications : /api/updates/latest?application=gps|drive|companion et /api/updates/download/<id>, compte natif requis.
DB : binding DB, table application_archives.
Dernier état rapporté :20archives complètes vérifiées par hash/taille et requête Range ; relire le backend avant nouvelle publication.
Ne pas mettre de jeton, mot de passe ni clé dans le suivi.

## 8. Priorités et état du lot
1. TERMINÉ : build/test/lint1.10.1,41tests et certificat conservé ; baselines préservées.
2. TERMINÉ : APK universel minSdk27/ARM32+64/V1+V2, CRC, ELF, DEX038 et compagnon111 embarqué identique.
3. Sources canoniques DRIVE/Compagnon/GPS mises à jour ; publication des archives sur le site suivie dans le journal final. Conserver les anciennes versions.
4. Tester les dispositions et les actions du tableau de bord ; vérifier local-first et couleurs à travers les vues natives et WebView sans changer de moteur.
5. Recueillir un essai utilisateur sur le poste pour installation, capture, audio/EQ et commandes. N’exiger une compilation Xiaomi qu’en fin de lot si nécessaire.
6. Traiter les lacunes observées avant de proposer un nouveau chantier ; ne pas annoncer la totalité des souhaits comme terminée.

## 9. Règles du suivi
Nathan veut une continuité autonome et ce fichier réutilisable.
Après chaque lot : changer la révision, indiquer changements réels, tests effectués/non effectués, blocages, prochaine étape ; inclure le suivi dans les nouvelles sources et sauvegarder.
Avancement : pourcentages à quatre décimales et deltas uniquement avec un périmètre pondéré explicite. Aucun pourcentage global fiable n’est établi ici ; ne pas inventer95%/100% à partir d’une compilation.
Audit complet au plus une fois par heure ; contrôles ciblés selon changement.
Ne jamais supprimer les bases radar embarquées ni exporter les secrets.
Toute action indispensable de Nathan doit porter « 🔴 INTERVENTION OBLIGATOIRE » avec étapes exactes.
Le travail ne continue pas automatiquement une fois la conversation arrêtée : fournir le dernier fichier pour reprendre au prochain message.


## Journal du 27 octobre — reprise 1
- Sources complètes DRIVE1.10.0/GPS293 et correctif1.10.1 récupérés puis correctif appliqué.
- Outils JDK21/Gradle8.13/SDK36/build-tools35 réinstallés ; clés privées uniquement locales.
- Contrôle155ressources : réussi.
- test_local_map_pipeline.py : adaptateurs mis à jour pour DownloadedMapStore réel, puis extension des cas de lecture locale avec réseau activé/désactivé ;115 assertions réussies. Aucun code applicatif GPS modifié.
- test_map_startup.js :13 scénarios du script de carte simulé réussis, bootstrap local/palette sans rechargement réussis. test_map_native_network.js : priorité du réglage natif vérifiée.
- APK1.10.1 : compilation en cours ; ne pas annoncer sa validation à partir de ce journal.
- Le premier suivi est sauvegardé sous libfile_2fc04545b3c4819197f072f55e555ade, version0 ; ZIP sous libfile_6b3f09efd9788191bd3ffe6bab41920d, version0. Vérifier la version actuelle avant remplacement.

- Extension locale-first terminée :115assertions réussies, dont PMTiles en mode réseau disponible, XYZ importé prioritaire avec/sans réseau, erreur explicite pour une tuile absente hors ligne.
- Le Compagnon reconstruit est identique octet pour octet à l’APK111 déjà livré (82 993 282octets) ; aucun remplacement de son APK nécessaire.
- Le site reste en version14 publique ; configuration de maintenance mise à jour en révision10 et déploiement réussi. Aucun code du site modifié.

- Premier lancement des tests1.10.1 :3tests d’autorisation réussis ; deux suites Robolectric bloquées avant leurs cas par une erreur de certificat de téléchargement Maven. Configuration du certificat ajoutée aux JVM de test via JAVA_TOOL_OPTIONS ; relance en cours avec DashboardLayoutTest et MapPaletteRuntimeTest supplémentaires. L’APK a déjà été assemblé ; aucune erreur de compilation applicative constatée. Ne pas compter les suites bloquées comme réussies.

- Relance après correction du certificat :41tests DRIVE/protocole réussis,0échec (Radar8, médiaAPI27:3, dispositions18, autorisation3, palettes6, protocole3). Contrôle lint API encore en cours à ce point du journal.

## Résultat du lot DRIVE1.10.1 — révision2
- Compilation, tests ciblés et lint API terminés avec succès. 41 tests DRIVE/protocole réussis dans cette exécution ; les16tests GPS du lot293 précédent restent documentés séparément.
- APK2001/minSDK27/ARM32+64/signaturesV1+V2/certificat conservé/DEX/CRC/ELF vérifiés. Compagnon embarqué identique à111 livré.
- GPS : seul le banc de test local-first est actualisé ;115assertions exécutées. Aucun changement du code de production GPS ; APK293 conservé.
- Les mention « en cours » plus haut décrivent les points de reprise antérieurs. Cette section décrit le résultat logiciel le plus récent ; publication et essais matériels à vérifier dans le journal final.

## Signature et reprise après changement d’environnement
Certificat DRIVE/Compagnon attendu (SHA256) :3f294ccdd3e2e5c0b7b5791807aa8dfaf50f9e0809c1ca964474e5a9c34d35cc.
Les sauvegardes privées existantes peuvent être retrouvées par leur identité : ROADGUARD-DRIVE-SIGNATURE-ESSAI.zip / libfile_c3288a86411c8191a6c58982374e3397 ; ROADGUARD-GPS-SIGNATURE-RECONSTRUCTION.zip / libfile_f01d60e80284819191e4412fb19888e1. Ces archives ne sont pas incluses : ne pas publier leurs clés. Elles doivent rester privées et servir seulement à une restauration locale autorisée.
En cas de PKIX lors des tests Robolectric, transmettre la configuration du certificat de confiance aux JVM de test, pas seulement au daemon Gradle. Ce problème a été résolu dans cette exécution.

## Prochaine intervention sur appareils (après publication de1.10.1)
🔴 INTERVENTION OBLIGATOIRE : mettre à jour DRIVE vers1.10.1 sur l’autoradio ; conserver/installer Compagnon1.1.1 sur le téléphone. Tester à l’arrêt le premier appairage et le partage d’écran, puis les boutons, l’éditeur de widgets, la lecture des cartes téléchargées en mode avion, les couleurs, la musique Bluetooth et l’effet réel du Studio audio. Le GPS autonome reste en11.14.293 ; aucune recompilation Xiaomi demandée pour ce lot.

## Avancement du lot actuel (périmètre explicite)
Les5jalons du correctif sont : restauration des sources, application du correctif, compilation, vérifications logicielles/signature, archivage final sur le site. Ne marquer le dernier terminé qu’après le résultat de transfert et son hash. Ce suivi ne donne pas un pourcentage de finition globale des applications : les essais matériels et les limites fonctionnelles du tableau restent distincts.

## Journal final du lot — publication confirmée
- Les4objets du lot (APK DRIVE1.10.1 et sources DRIVE/GPS/Compagnon) sont présents sur le site et ont été vérifiés par téléchargement intégral SHA256/taille et lecture Range. Catalogue :25archives après ce lot, toutes les anciennes versions conservées. Le nouveau point de reprise révision2 sera ajouté en plus.
- Sources canoniques sauvegardées : DRIVE version15, GPS version18, Compagnon version2 ; APK DRIVE version4. Les APK GPS293 et Compagnon111 restent inchangés.
- Lot correctif appairage :5jalons sur5 terminés,100,0000%. Depuis le point initial limité à restauration+correctif source (2sur5), progression de+60,0000points sur ce périmètre. Aucun pourcentage de finition globale ROADGUARD n’est déduit de ce calcul.
- Prochaine étape : essais sur appareils listés ci-dessus ; si une erreur apparaît, reprendre les sources de ce ZIP et les preuves du dossier preuves/. Ne pas refaire les modifications déjà intégrées.

## Révision3 — téléphones administrateur sans plafond, site15
Demande de Nathan : supprimer le blocage « Cinq téléphones sont déjà associés » pour son compte administrateur.
Terminé et publié : connexion native et approbation d’association acceptent un nombre non plafonné d’appareils lorsque le rôle administrateur est vérifié. Les comptes ordinaires conservent leur règle précédente. Aucune nouvelle application Android à installer ; GPS293, DRIVE1.10.1 et Compagnon111 inchangés.
Cause : la limite5 était appliquée avant toute exemption du rôle administrateur. Correction dans lib/native-auth.ts et lib/devices.ts du site.
Validation serveur :41groupes API/données SQLite passent, dont administrateur avec plus de12appareils, mot de passe incorrect refusé, association supplémentaire autorisée pour administrateur, règle des membres préservée. TypeScript et build réussis. Ces41groupes serveur sont distincts des41tests DRIVE/protocole décrits plus haut.
Publication native réussie : site15 ; version appgprj_6ad30cbe059c8191bdc679d1ad6832c2~appgver_7f2182e553048191a1952005d20c564c ; commit522fbd1a391f6dd1eab44db2759e89ea59bbf4da ; déploiement appgdep_6ae0516fdab48191a3eb376dde1b7d33 ; configuration10 et audience publique conservées. Le code du site est conservé dans son dépôt Git, à récupérer via Sites ; il n’est pas dupliqué dans ce ZIP.
🔴 INTERVENTION OBLIGATOIRE : dans le GPS, réessayer « Se connecter ou créer mon compte » avec le numéro et le mot de passe administrateur habituels. Aucun effacement des données ni réinstallation demandé. Confirmation sur le téléphone de Nathan encore attendue.

## Chantier mobile294 en cours
Demande : conserver la version GPS actuelle sur le site et transférer les fonctions utiles du véhicule sans launcher. Comparaison des sources : cartes, palettes, bips progressifs, mises à jour et synchronisation déjà communs. Ajouts en cours : Studio audio réel sur sessions compatibles ; sélection de lecteurs musicaux ; mini lecteur lié au lecteur sélectionné. Interface GPS et moteur de carte conservés. Compte administrateur illimité déjà géré par le site15. Build et tests du mobile294 en cours ; ne pas annoncer le nouvel APK comme validé avant leurs résultats.

## Mobile294 — résultat logiciel
Application GPS autonome294/code494 :48tests réussis, lint API réussi, signature conservée. Studio audio réel sur sessions compatibles, sélection des lecteurs et mini lecteur lié ajoutés. Voir WORK-11.14.294.md dans les nouvelles sources. Carte/moteur/7bases radar conservés ; launcher, MCU et récepteur de projection restent dans DRIVE. APK294 et sources prêts à archiver ; vérification de publication indiquée dans le journal suivant.

## Révision4 — GPS mobile294 publié, sans launcher
Demande du lot : sauvegarder le GPS actuel et transférer les fonctions du véhicule utiles au mobile, sans launcher. Terminé : Studio audio sur sessions compatibles, courbe10bandes adaptée au matériel,5préréglages, basses/réserve ; choix Spotify, automatique, lecteurs actifs/installés ; mini lecteur associé à la source. Nettoyage des fenêtres à la fermeture. Interface et moteur de carte conservés ; sept bases radars identiques. Bips progressifs, cartes locales prioritaires, palettes et mises à jour étaient déjà communs et restent présents.
Le launcher personnalisable, le DSP/MCU propriétaire et le panneau récepteur de projection restent dans DRIVE. Ne pas écraser les nouvelles classes MusicSources/AudioCurve/MobileAudioStudio ni les adaptations SpotifyControls/MusicMapBubble en recopiant l’ancien module DRIVE sur le mobile.
Validation :48tests mobiles réussis (musique16, nouveaux scénarios audio16, radar8, palettes6, bootstrap local2),0échec/erreur/ignoré. Nouvel audio testé par simulation AndroidAPI26 et36, pas de test sonore physique. LintNewApi/InlinedApi sans problème. SignatureGPS conservée a78735412fc59bdaf681e43459b559be35d96228d181227d1b8500ea23334bdf, V1/V2, DEX038, CRC, identités/version494/API26 vérifiés. Voir preuves/VERIFICATION-GPS-11.14.294.json et WORK-11.14.294.md.
APK294 :12 489 863octets, SHA256 34f1feaec553538999424832ae750678a818ea8c37003dc19d0557c87ed4fc0b. Sources canoniques version19 et APK version3 sauvegardés. APK/source294 sur le site, SHA256/taille/Range vérifiés ; catalogue28archives avant ce nouveau point de reprise. APK293 conservé et revérifié intégralement sur le site.
Les autres versions restent DRIVE1.10.1 et Compagnon1.1.1. Aucun nouveau déploiement de code serveur requis ; site15 déjà publié pour les téléphones administrateur sans plafond.
Le dossier reprend désormais les sources mobiles sous sources/ROADGUARD-11.14.294. Les preuves293 précédentes restent historiques. Ne pas déduire la validation matérielle des48tests. Ne pas fusionner les compteurs serveur/DRIVE/GPS comme s’il s’agissait d’une même exécution.
🔴 INTERVENTION OBLIGATOIRE : installer l’APK GPS294 en mise à jour, sans désinstaller. Réglages → Plus d’options → Musique et égaliseur : choisir un lecteur, autoriser les commandes musicales si nécessaire, afficher le mini lecteur, ouvrir le Studio. Relancer une lecture si la session n’était pas annoncée. Vérifier son statut et l’effet sonore réel. Aucun nouvel APK DRIVE/Compagnon demandé pour ce lot.
