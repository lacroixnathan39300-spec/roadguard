# ROADGUARD 11.13.78 — protection du planificateur contre les scans excessifs

## Modifications réellement effectuées
- `app/src/main/java/fr/roadguard/app/EmergencyTilePlanner.java` : arrêt du balayage aux niveaux de zoom où la surface candidate serait disproportionnée par rapport au plafond de tuiles. Évite les millions d'itérations et une potentielle saturation CPU.
- `tests/EmergencyTilePlannerTest.java` : test de performance bornée jusqu'au zoom 19, limite 300 tuiles, et validité des indices x à proximité de +/-180°.
- Test exécuté : `javac -d ... EmergencyTilePlanner.java EmergencyTilePlannerTest.java` puis `java ... EmergencyTilePlannerTest` : PASS.

## Limites honnêtes
- Ce changement ne télécharge AUCUNE tuile. Il n'existe pas encore de fournisseur configuré autorisant le préchargement. La source publique OSM actuelle ne doit pas être préchargée massivement.
- Le plafond de 300 tuiles peut empêcher la couverture complète de 50 km à plusieurs zooms : ne pas annoncer que la zone est entièrement disponible.
- Pas de compilation APK, ni de test Xiaomi. Pas de changement du GPS bleu ni de la caméra.
- Bibliothèque hors ligne permanente exclue du nouveau périmètre utilisateur.

## Priorités de reprise
1. Choisir un fournisseur de tuiles dont la licence/conditions permettent explicitement le cache temporaire anticipé et obtenir un endpoint utilisable.
2. Ajouter un cache de tuiles à durée de vie contrôlée et le raccorder au serveur local WebView `roadguard.local`.
3. Connecter le planificateur au téléchargeur, vérifier couverture réelle par niveau de zoom, puis tester coupure/retour réseau.
4. Compiler APK et tester sur Xiaomi.

## Avancement estimé (non validé sur téléphone)
GPS bleu 100%; carte en ligne 85%; bascule réseau 50%; planification 50 km 40% (+5); préchargement réel 0%; affichage pendant coupure 25%; reprise 35%; APK Xiaomi 0%. Avancement global : non recalculé après changement de périmètre. Ne pas réutiliser l'ancien 42%.
