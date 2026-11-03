# ROADGUARD 11.13.72 — Passation

## Réalisé réellement
- Identifié le défaut de bout en bout : OfflinePackSync enregistre les PMTiles dans `files/offline-cells`, mais le WebView cherchait seulement dans `files/pmtiles`.
- `MainActivity.serveOfflineTile` peut maintenant servir les fichiers PMTiles de `OfflineCellStore` **après vérification SHA-256**; les packs manuels existants restent accessibles.
- Ajout `OfflineCellStore.verifiedFile(id)` : renvoie uniquement un fichier enregistré et vérifié.
- Ajout `InstalledPackSelector` : sélectionne uniquement une archive PMTiles vectorielle vérifiée couvrant la position GPS, sur la base du catalogue autorisé.
- Ajout activation sur position GPS (cadencée) et après un lot de téléchargements; passage au rendu MapLibre.
- Version Android `11.13.72` dans `app/build.gradle`.
- Test `tests/InstalledPackSelectorTest.java` sur **la vraie archive** `samples/demo-firenze/demo-firenze.pmtiles` (3,1 Mo) : import, SHA-256, lecture vectorielle, sélection GPS Florence, refus GPS Jura et catalogue vide.
- Test existant `tools/test_pmtiles_reader.py` réussi.

## Limites et blocages NON résolus
- **Aucun catalogue `app/src/main/assets/offline_pack_catalog.tsv` n'est présent**. Le système ne peut pas télécharger de pack régional automatiquement tant que les archives autorisées, hébergées et hachées n'existent pas.
- La carte de démonstration réelle est à Florence, **pas dans le Jura**; elle ne couvre pas l'utilisateur.
- Aucun pack Jura/France extrait, hébergé en HTTPS, téléchargé sur téléphone ou rendu sur Xiaomi.
- APK non compilée dans cette session (Gradle/Android SDK non disponibles).
- Le mécanisme de sélection vérifie le hash de chaque archive candidate : peut coûter des ressources pour des packs volumineux. Optimisation à prévoir.
- Le passage de carte via `render()` redémarre l'écran GPS : nécessite validation réelle.
- Aucune validation complète du rendu MapLibre avec les couches exactes du fournisseur VersaTiles.
- L'estimation historique 42 % ne représente pas le fonctionnement réel sur téléphone; ne pas augmenter artificiellement.

## Priorités
1. Obtenir un petit PMTiles réel du Jura, inspecter sa licence, son schéma vectoriel et son rendu.
2. Héberger le pack via un HTTPS stable, produire `offline_pack_catalog.tsv` SHA-256 et l'inclure dans l'APK.
3. Compiler APK avec SDK Android, tester carte en ligne/hors ligne et GPS bleu sur Xiaomi.
4. Optimiser vérification d'archives, rafraîchissement GPS, téléchargements arrière-plan et mémoire cumulative.

## Tests exécutés
- `python tools/test_pmtiles_reader.py` => PASS (3 contrôles)
- `javac -d /tmp/rg72-classes app/src/main/java/fr/roadguard/app/{PmtilesReader,OfflineCellStore,OfflinePackRegionSelector,NearbyOfflinePlanner,VerifiedMapDownloader,InstalledPackSelector}.java tests/InstalledPackSelectorTest.java` => PASS
- `java -cp /tmp/rg72-classes InstalledPackSelectorTest samples/demo-firenze/demo-firenze.pmtiles` => PASS

## Intervention utilisateur
Aucune intervention obligatoire pour ces changements de code. Hébergement et test APK ultérieurs nécessiteront des ressources ou une intervention.
