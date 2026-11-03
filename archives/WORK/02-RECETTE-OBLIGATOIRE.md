# WORK — Tests et critères d'acceptation

1. **Build** : `gradle assembleDebug` (ou variante compatible), logs et versionCode relevés ; `node --check` des scripts JS extraits si applicable ; tests Python tools ; archive ZIP testzip.
2. **Réutilisation locale** : confirmer le PMTiles France existant (~2,76 Go) reste présent avant/après installation APK ; ne pas lancer de téléchargement. Si le fichier est dans Téléchargements, l'importer par sélecteur local et vérifier espace libre; s'il est déjà dans `getFilesDir()/pmtiles`, vérifier détection sans copie. Valider taille, SHA, en-tête, emprise et lecture de vraie tuile.
3. **Cartographie hors ligne** : MapLibre JS/CSS embarqués dans APK; ouvrir France en mode avion après relance; voir routes, eau et noms de villes; inspecter erreurs WebView, CORS et glyphes. Ne pas confondre cache réseau et pack PMTiles.
4. **Réseau** : Wi-Fi et 4G/5G, confirmation taille/forfait, option Wi-Fi seul et perte de réseau. L'import du pack existant ne doit effectuer aucun appel réseau.
5. **Résilience** : interruption/reprise du téléchargement normal, annulation de l'import local, manque d'espace, mise à jour APK et conservation du fichier existant.
6. **Performance/caméra/GPS** : comparer la caméra, l'orientation et le suivi à 11.12.7/11.13.9; test conduite, fluidité et absence de flashs blancs.
7. **Catalogue** : autres pays bloqués jusqu'à validation France; pas de faux état « disponible ».
8. **Rapport** : PASS/FAIL/NON TESTÉ/BLOQUÉ par critère; build, Xiaomi et mode avion explicitement marqués.

RÈGLE : ne pas publier une nouvelle version comme fonctionnelle si aucun pack n'a été effectivement affiché hors ligne.
