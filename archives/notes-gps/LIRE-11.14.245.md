# ROADGUARD 11.14.245 — projet complet, import 7z intégré

Ce ZIP reprend intégralement la 244 et contient les cinq JAR dans `app/libs`, les deux archives FR/CH intégrées aux assets et leurs copies originales dans `radar_sources`.

Le bouton pays extrait l’archive, lit le SQLite et installe sa surcouche. FR/CH peuvent utiliser leurs archives intégrées sans réseau. Les originaux restent conservés. Le statut actif dépend du chargement par un consommateur, après retour à la carte.

Lire `WORK-11.14.245.md`, `AVANCEMENT-11.14.245.md` et `FICHIERS-POUR-IMPORT-7Z.md`. Sources, historique, Gradle, variantes, ressources, dépendances et résultats sont tous dans ce ZIP.

25 scénarios d’extraction/import réels passent, avec les régressions 244. Compilation Android, Robolectric et matériel non validés. Aucun APK 245 fourni ; l’APK 197 est historique.

Le contrôle automatique a refusé le téléchargement des outils Google/Gradle ; une autorisation explicite est nécessaire pour le retenter ici. Aucun des sept fichiers radar ne manque plus. Aucun APK Waze/Sygic n’est nécessaire pour cette intégration.
