# ROADGUARD 11.14.244 — ouverture de la carte, reprise des tuiles et pack graphique

21 octobre 2026 — versionCode 444 — package `fr.roadguard.app` — référence : 11.14.243.

Livraison complète en **un seul ZIP**, avec sources CodeAssist, ressources, données radar d’origine, historique WORK, suivi, outils et résultats des contrôles. Aucun APK 244 n’est présenté comme produit ou validé. Le vieil APK 197 conservé dans `LIVRAISON/` reste une référence historique.

## Retour utilisateur et diagnostic

Quatre captures reçues le 21 octobre montrent une archive FR vérifiée mais non importée, un écran de démarrage portant le texte « Téléchargement des signalements de votre secteur… », un cache de 278 tuiles avec 204 lectures locales et zéro tuile reçue depuis l’ouverture, et les réglages WazeAPI/OpenWeb Ninja. La version installée n’est pas visible : ces captures ne sont pas comptées comme une validation complète de la 243 ou de la 244.

L’archive reçue et les radars déjà embarqués sont deux états distincts. Le téléchargement 7z de la 243 ne réalise pas son extraction ; il ne peut donc pas activer ses points supplémentaires. Cela ne retire pas les radars d’origine.

Le compteur réseau était limité aux téléchargements de cette instance du cache. Zéro téléchargement avec 204 lectures locales est compatible avec des tuiles demandées déjà présentes ; ce chiffre seul ne démontre pas une panne réseau. En revanche, la reprise JavaScript dépendait surtout d’une transition hors ligne/en ligne et utilisait un `redraw()` global. Des échecs partiels pendant une connexion déclarée disponible pouvaient rester sans nouvelle tentative.

## Changements

### Carte disponible pendant le chargement des radars

L’illustration de démarrage laisse place à la carte après les premières trames et la restauration des préférences locales. Elle n’attend plus le cache communautaire, l’inventaire complet ou une réponse HTTP. Les catalogues et surcouches radar sont assemblés sur le worker dédié ; l’index courant reste utilisé jusqu’à réception d’un ensemble complet.

Le nouveau `RadarSupplementCatalog` lit les anciens fichiers pays et ajoute l’inventaire communautaire sans retirer les originaux. Les fichiers sont bornés, les coordonnées invalides refusées et la quarantaine historique conservée. L’index de proximité utilisé pour cette fusion est local au worker. Les points privés et signalements présents au moment de la publication sont conservés.

Une génération de requête et la révision des fichiers rejettent un résultat périmé. Les demandes reçues pendant un chargement sont regroupées. Un échec laisse l’ensemble précédent en place et propose une nouvelle tentative. La carte montre « Radars : chargement en arrière-plan… » ; ce message disparaît après chargement. Au premier lancement, les alertes du catalogue complet ne sont disponibles qu’après cette étape, sans bloquer la carte.

La synchronisation communautaire part séparément après l’ouverture. Elle ne modifie plus le texte de l’ancien écran de démarrage. Les nouveaux signalements continuent d’arriver avec le GPS selon le fonctionnement existant. Le chargement en arrière-plan décrit ici est un travail non bloquant pendant l’exécution de l’application ; ce lot n’ajoute pas un service de téléchargement continu après fermeture forcée de l’application.

### Reprise des tuiles et compteurs compréhensibles

`tile_recovery.js` suit les tuiles visibles en échec et programme au maximum quatre reprises espacées. Les bonnes tuiles ne sont pas effacées. La reprise se suspend hors réseau ou lorsque la page est masquée ; un retour de connexion ou une action explicite peut rouvrir le budget de tentatives. Les tuiles sorties du champ sont abandonnées. Le branchement utilise les méthodes de Leaflet 1.9.4, version déjà embarquée et épinglée.

Un callback Android suit les changements de réseau et est retiré à la destruction de l’activité. Le retour du réseau libère le délai de repli des erreurs du cache. Le cache natif contrôle les accès réseau des tuiles interceptées ; la WebView ne reste plus verrouillée en `LOAD_CACHE_ONLY` pour ces demandes. Aucun fichier de cache existant n’est purgé par la correction ou le bouton de reprise.

L’écran Cache affiche maintenant les tentatives et échecs réseau, le nombre de tuiles reçues depuis l’ouverture, et la dernière erreur utile, notamment le code HTTP reçu. Il explique pourquoi un compteur réseau nul peut être normal. « Réessayer les tuiles manquantes » retourne à la carte et libère le délai de nouvelle tentative.

Le plafond de 4 Gio, la réserve de 512 Mio, les en-têtes HTTP de cache, les contrôles PNG et la capacité partagée téléphone/voiture sont conservés. La reprise est bornée ; elle ne peut pas garantir qu’un fournisseur joignable acceptera chaque requête ou qu’une zone jamais consultée sera disponible sans réseau.

### Pack graphique intégré

Apparence propose **Graphite**, **Lisibilité** et **Couleurs OSM**. Le choix est gardé sur le téléphone. Le pack CSS/JavaScript est livré dans les assets de l’application ; aucun fichier graphique externe n’est nécessaire. Graphite conserve le rendu par défaut validé.

Ce pack agit sur les pixels des tuiles raster. Il n’ajoute pas de routes, ne fabrique pas de tuiles manquantes et ne permet pas de modifier séparément chaque catégorie de route comme un style vectoriel. Les données géographiques continuent d’arriver par la carte en ligne et son cache. Le curseur GPS, les alertes et les calculs de caméra ne sont pas modifiés.

La source actuelle `tile.openstreetmap.org` interdit le téléchargement massif et les fonctions de préchargement. Sa politique a été relue le 21/10/2026 : <https://operations.osmfoundation.org/policies/tiles/>. La référence Leaflet 1.9.4 a également été consultée : <https://leafletjs.com/reference.html>. Ce lot n’active donc pas un pack France permanent ni un téléchargement automatique de toutes les tuiles dans un rayon de 50 km. Une couverture anticipée étendue nécessitera une source dont les conditions autorisent cet usage. Le cache des tuiles consultées et la reprise des échecs restent opérationnels dans le code.

### Radars et fournisseurs externes

Le gestionnaire affiche ensemble le nombre de points locaux et l’existence d’une archive reçue. Le bouton devient « Archive reçue » après vérification ; une réception ne se présente pas comme une activation. Le bouton global devient « Recevoir les archives » pour décrire son action réelle.

L’entrée Contrôles mémorisés explique d’abord que WazeAPI et OpenWeb Ninja sont deux services externes facultatifs, avec leur propre clé. L’accès aux lieux privés est direct ; les réglages techniques restent dans « Sources externes · facultatif ». Les préférences existantes sont conservées. Aucune collecte, inscription ou facturation n’est activée par ce lot. Les bases radar embarquées continuent de fonctionner sans ces clés.

## Vérifications exécutées

| Contrôle | Résultat réel |
|---|---|
| Cache de tuiles | 17 scénarios JVM avec fichiers réels et transport HTTP injecté : reprise après erreur, délais, redémarrage sans téléchargement inutile, PNG corrompu, ETag/304, `no-store`, `must-revalidate`, espace libre, éviction, écritures concurrentes et budget supérieur à 2 Gio. |
| Surcouches radar | 10 scénarios avec de vrais fichiers et les assets FR ; conservation des identifiants d’origine, doublons, coordonnées, quarantaine, historique CH, ligne de changement de date, annulation et résultat immuable. Les 24 440 assertions ne sont pas 24 440 essais matériels. |
| Tuiles / pack / écrans | 11 scénarios exécutant le JavaScript livré dans Node : reprise bornée, pause hors réseau ou page masquée, sortie du champ, maintien des bonnes tuiles, budget non réinitialisé par `tileunload`, sélection du pack, rendu des textes et échappement HTML. DOM et couche Leaflet adaptés ; aucun navigateur/WebView réel. |
| Régression SQLite | 35 scénarios avec 14 vrais fichiers SQLite synthétiques, ouverture Python en lecture seule derrière des adaptateurs JVM des frontières Android. Aucune extraction 7z. |
| Téléchargement radar | 27 scénarios de téléchargement/file réexécutés, HTTP simulé et fichiers réels. |
| Catalogues / alertes / GPS | 14 scénarios catalogue réexécutés sur les ressources livrées, plus règles d’alertes et mouvement GPS. |
| Syntaxe | 85 fichiers Java analysés et JavaScript des deux cartes analysé. Les classes métier ciblées compilent en Java 8 ; les types Android de toute l’application ne sont pas résolus par ce contrôle. |
| Sources d’origine | 59 fichiers CSV/ASC/JSON comparés à leurs empreintes de référence ; contrôle aussi appliqué au ZIP final, avec CRC de toutes les entrées. |

Résultats : `verification/targeted-checks-11.14.244.log`, `targeted-results-11.14.244.json`, `release-contract-11.14.244.json` et `data-integrity-11.14.244.json`. Reproduction : `python3 tools/run_targeted_checks.py`, `python3 tools/update_progress_11_14_244.py`, `python3 tools/render_progress_report.py`.

Le test Android `StartupRuntimeTest` a été réécrit avec un catalogue volontairement bloqué pour vérifier que la carte reste ouverte et qu’un point privé ajouté pendant l’attente est conservé. **Il n’a pas été exécuté.** Les autres suites Robolectric restent à exécuter. Le SDK Android et la distribution Gradle ne sont pas disponibles ici ; les traces du blocage précédent sont conservées. **Aucun APK 244, aucun essai Xiaomi ni autoradio de cette version, aucun essai avec le vrai serveur de tuiles n’est déclaré réalisé.**

Les contrôles sont ciblés sur ce lot. Aucun audit caméra n’a été relancé ; les fichiers de géométrie et de mouvement de caméra restent inchangés. Android Auto reste configuré comme dans la base (`roadguardAndroidAuto=true`), avec possibilité d’isoler le téléphone par la propriété `false`.

## Avancement

Les 39 tâches et leurs 100 points sont conservés. Ces corrections concernent des tâches déjà codées : le développement reste **78,0000 % (+0,0000 point)**. La validation automatique du cache M6 apporte **+1,5000 point pondéré**, portant la validation à **29,0000 %**. Pour Carte/GPS, cela donne 100,0000 % de développement et 42,5000 % de validation, soit +7,5000 points de validation à l’échelle de ce module.

M1 (démarrage) ne reçoit pas de crédit Android natif pour un test ajouté mais non exécuté. Aucune hausse n’est attribuée à la compilation, au véhicule ou à l’extraction 7z. Le tableau complet et les preuves sont dans `AVANCEMENT-11.14.244.md`. Les 48,0000 % historiques restent une estimation non auditée distincte.

## Suite et interventions

**Chantier actif :** valider sur Android le démarrage, la reprise cartographique et le pack ; terminer ensuite la chaîne 7z → SQLite → import → activation. Les modifications de ce lot ne terminent pas l’extraction 7z.

**🔴 INTERVENTION OBLIGATOIRE — MAINTENANT POUR TERMINER LE 7z :** joindre les deux archives FR/CH et les cinq JAR indiqués dans `FICHIERS-POUR-IMPORT-7Z.md`. Un seul ZIP de ces sept fichiers suffit. Le projet complet est déjà fourni ; aucun renvoi du projet entier n’est demandé. L’archive FR visible sur le téléphone n’est pas un fichier accessible dans cette session.

**🔴 INTERVENTION OBLIGATOIRE — PLUS TARD, EN FIN DE LOT :** compiler une fois dans CodeAssist avec la signature habituelle, installer sans effacer les données, puis vérifier : carte utilisable pendant le chargement radar ; disparition du message après chargement ; déplacement de carte et tuiles manquantes ; coupure/rétablissement du réseau ; trois choix dans Apparence ; bases FR/CH et état des archives. Android Auto doit être essayé séparément. Aucun besoin de clé WazeAPI/OpenWeb Ninja pour ces essais des bases embarquées.
