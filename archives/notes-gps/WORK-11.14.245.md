# ROADGUARD 11.14.245 — import 7z et fichiers publics inclus

21 octobre 2026 — versionCode 445 — package `fr.roadguard.app` — reprise intégrale de la 11.14.244.

Projet complet, cinq JAR, archives France/Suisse, WORK, suivi et preuves réunis dans un seul ZIP. Les sept fichiers précédemment demandés sont désormais présents. Aucun renvoi de ces fichiers ou d’APK Waze/Sygic n’est nécessaire pour cette étape.

## Récupération et provenance

L’accès HTTP a recommencé à fonctionner. Les archives `europe-202606` FR/CH ont été téléchargées depuis le dépôt public `hvdwolf/SpeedAlert_Databases` ; leurs tailles et SHA-256 correspondent aux valeurs déjà épinglées.

Les cinq JAR viennent de Maven Central : Commons Compress 1.28.0, IO 2.20.0, Lang3 3.18.0, Codec 1.19.0 et XZ 1.12. Les SHA-256 de Compress/XZ correspondent aussi à l’audit précédent. Les fichiers de contrôle disponibles sur Maven ont été comparés : SHA-1 pour les quatre bibliothèques Apache, SHA-512 pour XZ. Tous les SHA-256 calculés sont désormais épinglés dans le projet. Les classes ordinaires de ces JAR ciblent Java 8 (version de classe 52) ; cela ne constitue pas un essai Android.

Détails : `verification/dependencies-11.14.245.json`. Notices Apache extraites des JAR, licence XZ 0BSD et attribution des données CC BY 4.0 conservées dans les assets. Le dialogue Sources indique auteur, licence, dépôt et conversion ROADGUARD.

## Extraction et installation

`RadarSevenZipExtractor` utilise les vraies bibliothèques livrées. Il impose :

- Une archive structurellement valide, puis son SHA épinglé avant import automatique.
- Une unique entrée `<pays>-camera.sqlite`, sans répertoire, chemin absolu, traversée de chemin ou entrée supplémentaire.
- Une charge SQLite de 512 à 32 000 000 octets, avec taille réelle comptée, taille annoncée comparée, CRC et signature SQLite contrôlés.
- Une limite de décodage de 32 768 Kio ; codecs COPY/LZMA/LZMA2 uniquement ; récupération d’archive cassée désactivée.
- Un fichier temporaire unique choisi par l’application, synchronisé et supprimé en cas d’échec ou d’annulation.

Le chemin porté par l’archive n’est jamais utilisé comme destination d’écriture. Le test de mémoire construit une petite archive déclarant un dictionnaire supérieur à la limite, sans allouer un gros encodeur au banc de test.

`RadarArchiveInstaller` raccorde vérification, extraction, lecture SQLite en lecture seule et `CanonicalRadarStore`. La surcouche est validée avant remplacement atomique ; deux sauvegardes sont conservées. Comme dans la 243, un nouvel import remplace la précédente surcouche canonique du pays, sans remplacer les inventaires d’origine. La fusion complète par direction/fiabilité reste distincte.

Les archives FR/CH sont incluses dans `app/src/main/assets/radar_archives`, et leurs originaux avec attribution dans `radar_sources/speedalert-europe-202606`. Ces deux installations peuvent donc se faire sans réseau. Une archive reçue conforme est réutilisée ; la copie d’un asset est vérifiée avant remplacement atomique. Les cinq autres pays épinglés conservent le chemin réseau ; leurs vrais fichiers n’ont pas été téléchargés ni éprouvés dans ce lot.

Le bouton pays propose Installer/Importer et exécute réception éventuelle → extraction → lecture → installation sur le worker. La file distingue l’import de la seule réception. L’annulation de l’import est indépendante du transfert déjà terminé. Détruire l’activité demande l’annulation ; aucune poursuite après fermeture forcée n’est annoncée.

Après commit, la révision change et le service reçoit une demande de rechargement. Le retour à la carte recharge le catalogue. Le statut reste importé/à confirmer jusqu’au reçu d’un consommateur correspondant à la révision ; le bouton et l’extracteur ne créent pas de reçu actif.

`app/radar-dependencies.json` épingle les cinq JAR et les deux archives. Une tâche Gradle locale refuse un fichier manquant/altéré avant build. Les ressources META-INF conflictuelles sont exclues du paquet, leurs notices restant dans les assets.

## Vraies bases et déduplication

| Pays | Entrées cameras | Taille SQLite | Ajouts consommés pendant le test |
|---|---:|---:|---:|
| France | 3 824 | 258 048 octets | 2 197 |
| Suisse | 593 | 57 344 octets | 86 |

Ce résultat utilise les historiques FR/CH activés et les inventaires fournis. Les ajouts sont ceux conservés par la déduplication actuelle, pas une validation terrain de radars nouveaux ou présents. Les réglages et imports locaux peuvent modifier le résultat sur téléphone.

Les SQLite contiennent `cameras(id, lat, lon, type)` et le texte `speed` pour tous les points observés. Ce texte ne précise ni sous-type fixe/mobile, ni vitesse limite, ni direction. Il est conservé dans les métadonnées ; ROADGUARD utilise Radar · type non renseigné, vitesse inconnue et direction absente. La date du modèle est ici celle de publication de la release, pas une observation sur place.

Les originaux et leurs identifiants sont conservés. Les doublons supprimés ne sont pas comptés comme des ajouts au moteur.

## Vérifications exécutées et limites

**25 scénarios 7z/import** passent avec les vraies archives et bibliothèques : pays, taille/CRC, chemins, entrées multiples, archive vide/répertoire, charge petite/non SQLite, codec refusé, dictionnaire trop grand, charge trop grande, corruption/troncature, annulation/nettoyage, asset manquant/corrompu, copie précédente conservée, installation et consommation du catalogue.

Régressions 244 : 17 scénarios cache, 10 surcouches, 11 JavaScript, 35 SQLite/canonique, 27 téléchargement/file et 14 catalogue, plus politiques d’alertes et mouvement GPS. Classes métier ciblées compilées en Java 8 avec les JAR réels. **88 fichiers Java analysés pour la syntaxe** ; ce contrôle ne résout pas tous les types Android.

Le SQLite des tests est réel, ouvert en lecture seule par Python derrière les adaptateurs JVM des frontières Android. Ce n’est pas `SQLiteDatabase` exécuté sur Android. `RealSevenZipRuntimeTest` API 26/36 avec SQLite natif Robolectric est fourni mais **non exécuté**. Les autres suites Android restent également à exécuter.

Les 59 CSV/ASC/JSON d’origine sont inchangés ; le contrôle est appliqué aussi au ZIP final avec CRC de ses entrées. Résultats : `verification/targeted-checks-11.14.245.log`, `targeted-results-11.14.245.json`, `release-contract-11.14.245.json`, `data-integrity-11.14.245.json`. Reproduction : `python3 tools/run_targeted_checks.py`, `python3 tools/update_progress_11_14_245.py`, `python3 tools/render_progress_report.py`.

**Aucun APK 245 produit, aucune suite Robolectric exécutée, aucun essai Xiaomi/autoradio déclaré validé.** L’APK 197 conservé est historique.

## Refus de téléchargement des outils Android

Une tentative de récupération des outils Android a été interrompue par le contrôle d’approbation automatique. Motif : « La commande ne contacte ni github.com ni une URL GitHub ; cette autorisation réseau ne découle pas de son contenu, qui vise uniquement dl.google.com et services.gradle.org. » Les téléchargements GitHub/Maven décrits plus haut ont réussi. Aucun contournement du refus Google/Gradle n’a été utilisé.

Une autorisation explicite est nécessaire pour retenter les téléchargements Google/Gradle. Sources, dépendances et contrôles Java sont livrés indépendamment de cette approbation.

Version 11.14.245 / 445 ; SDK cible/compilation 36, minimum 26. Android Auto reste activé par `roadguardAndroidAuto=true` ; `false` isole le téléphone. Démarrage, cache, pack graphique et explication des services externes de la 244 sont conservés. Géométrie/mouvement de caméra inchangés, aucun nouvel audit caméra. Aucun téléchargement permanent de carte France réactivé.

## Avancement et suite

39 tâches, 100 points et poids inchangés. D5 est codé : **+3,0000 points de développement**. Son contrôle automatique Java avec vraies archives apporte **+2,0000 points de validation**, sans crédit Android natif ou matériel.

- Développement : **81,0000 %**, variation **+3,0000 points** contre la 244.
- Validation : **31,0000 %**, variation **+2,0000 points**.
- Sources/activation : 88,0000 % de développement (+12,0000 points du module), 48,0000 % de validation (+8,0000 points du module).

Tableau complet : `AVANCEMENT-11.14.245.md`. Les 48,0000 % historiques restent une estimation non auditée séparée. D7 (sens/fiabilité/fusion et confirmations multimodes), compilation et essais matériels restent incomplets.

**Chantier actif :** compilation/essais Android du lot, puis fusion directionnelle et confirmations téléphone/service/voiture.

**🔴 INTERVENTION OBLIGATOIRE — POUR UNE COMPILATION ICI :** autoriser explicitement les téléchargements des outils Android depuis Google et Gradle refusés par le contrôle automatique. Aucun renvoi des sept fichiers ou des APK Waze/Sygic n’est nécessaire.

**🔴 INTERVENTION OBLIGATOIRE — PLUS TARD, EN FIN DE LOT :** compiler dans CodeAssist avec la signature habituelle, installer sans effacer les données, installer FR/CH dans le gestionnaire, revenir à la carte et vérifier le statut consommé. Essayer alertes/cache/apparence sur Xiaomi, puis Android Auto séparément. Ces essais ne sont pas présentés comme faits.
