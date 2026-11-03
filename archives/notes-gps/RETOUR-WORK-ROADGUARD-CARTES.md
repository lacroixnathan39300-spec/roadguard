# Rapport Work — ROADGUARD 11.13.10, rendu PMTiles hors ligne

Date : 10 octobre 2026  
Source d'entrée : ZIP 11.13.10 avec `versionName 11.13.9` / `versionCode 170`, tests encore attendus en 11.13.8 / 169.  
Version de correction source : `11.13.10` / `versionCode 171`.

## Changements intégrés

- MapLibre GL JS 4.7.1 et CSS sont maintenant inclus dans les assets du projet (`app/src/main/assets/vendor/`) et prévus pour être packagés dans l’APK au build. SHA-256 épinglés et contrôlés par le script d'obtention et le build; notices BSD-3-Clause fournies.
- Trois plages de glyphes MapLibre Noto Sans Regular (`0-255`, `256-511`, `512-767`) sont incluses pour les caractères français usuels, avec la licence OFL 1.1.
- Le décodeur Brotli Java `org.brotli:dec 0.1.2` est embarqué avec sa licence MIT. `PmtilesReader` prend en charge les sections et tuiles sans compression, gzip et Brotli; zstd reste refusé explicitement.
- Les couches MapLibre montrent les routes OpenMapTiles, noms de routes (`transportation_name`) et lieux (`place`), avec glyphes depuis l'endpoint local. Les réponses PMTiles/glyphes ont des en-têtes CORS; les erreurs locales répondent 404 sans basculer vers Internet.
- Au démarrage, ROADGUARD recherche un PMTiles vectoriel France déjà stocké dans `getFilesDir()/pmtiles/`, le sélectionne sans le télécharger et reprend la carte MapLibre si les assets sont présents. Bouton visible pour revenir à la carte en ligne.
- Ajout d'un sélecteur Android permettant de reprendre une archive PMTiles depuis le stockage du téléphone, sans réseau. Import privé avec progression/annulation, contrôle de taille (8 Gio), signature/version/sections, type, couverture d'en-tête France-Corse pour le vectoriel et calcul SHA-256. Le fichier original n'est pas supprimé. Une source située hors du dossier privé est copiée et exige autant d'espace libre; un fichier déjà installé est réutilisé sans duplication.
- GPS, caméra principale, itinéraires et cartes d'origine n'ont pas été modifiés intentionnellement; le rendu MapLibre constitue toujours une vue distincte qui demande essai sur Xiaomi.
- Contrôles de version alignés sur 11.13.10 / 171.

## Résultats locaux

| Vérification | État | Résultat / limite |
|---|---|---|
| Tests Python | PASS | `python -m unittest discover -s tools -p 'test_*.py'` — 67 tests, `OK` |
| Lecteur PMTiles raster/vectoriel | PASS | Fixtures synthétiques gzip et Brotli; le test Brotli décompresse une tuile vectorielle avec le JAR fourni |
| MapLibre et licences | PASS source | JS/CSS et glyphes présents dans le projet, empreintes connues; licences MapLibre BSD, Brotli MIT, Noto OFL jointes. APK non compilée |
| Glyphes et style | PASS statique | Trois plages présentes; couches `place`, `transportation_name` et fallback français déclarés |
| Intercepteur/CORS | PASS statique | URLs `roadguard.local` servies localement; CORS configuré; inconnues 404 locales |
| GPS/caméra | PASS statique seulement | Suite existante GPS/caméra passe; pas d'essai conduite/Xiaomi après activation MapLibre |
| PMTiles France réel ~2,76 Go | NON ACCESSIBLE ICI | L'archive n'est pas dans le ZIP reçu ni dans ce workspace; observation de taille rapportée par l'utilisateur. Sélecteur local et auto-détection ajoutés pour réutiliser le fichier du téléphone sans le retélécharger |
| Métadonnées/couches du vrai PMTiles | BLOQUÉ | Besoin du fichier réel pour contrôler schema/vector_layers, compression, vraie tuile et emprise. Le style actuel cible OpenMapTiles |
| Build APK Android/CodeAssist | BLOQUÉ | Pas de `gradle`, `gradlew` ni Android SDK ici. Les assets et JAR nécessaires sont maintenant dans le projet |
| Mode avion Xiaomi, routes/villes, GPS/caméra | NON TESTÉ | Aucun appareil Android accessible; ne pas annoncer cette version comme recette validée |
| Réseau de compilation | PASS partiel | MapLibre, glyphes, licence et JAR récupérés et vérifiés; compilation complète non exécutée |

## Utilisation prévue sur l'appareil

1. Installer l'APK construit à partir de cette source par-dessus ROADGUARD existant, sans désinstaller l'application (le même `applicationId` conserve les fichiers privés lors d'une mise à jour normale).
2. Dans Bibliothèque/Hors ligne, si l'archive France est déjà dans le stockage privé ROADGUARD, elle doit être détectée automatiquement. Si elle est dans Téléchargements, toucher **Importer une carte PMTiles déjà présente sur le téléphone · sans Internet** puis sélectionner le fichier. L'import le copie dans l'espace privé, donc vérifier l'espace libre. Aucun transfert réseau de la carte ne doit démarrer.
3. Ouvrir Carte puis activer le mode avion. Vérifier rues, noms de villes, position GPS, rotation et fluidité; retour carte en ligne reste disponible.

## Blocages de fin

La mission n'est **pas terminée** tant que le vrai PMTiles n'a pas été repris sur le Xiaomi, que ses couches et ses tuiles ne sont pas lues, et que les noms/routes ne sont pas visibles sans réseau sur l'appareil. Les 2,76 Go n'ont pas été téléchargés à nouveau. L'import depuis un fichier externe fait une copie locale, tandis qu'un PMTiles déjà dans `files/pmtiles` reste en place et est détecté. Le style dépend du schéma OpenMapTiles; confirmer celui de l'archive réelle avant toute extension à Suisse/Allemagne.

## Dépendances incluses

- MapLibre GL JS 4.7.1 : https://unpkg.com/maplibre-gl@4.7.1/dist/ (BSD-3-Clause; licence dans `app/src/main/assets/vendor/`).
- Brotli decoder 0.1.2 : jar local `app/libs/brotli-dec-0.1.2.jar` (MIT; notice jointe).
- Glyphes Noto Sans Regular locaux : licences OFL 1.1 incluses.
