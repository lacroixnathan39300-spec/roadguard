# ROADGUARD 11.13.24 — Passation

Base: ROADGUARD 11.13.23 (plus récente disponible), malgré le fichier 11.13.22 envoyé dans le dernier message.

## Effectivement réalisé
- Ajout de 51 fichiers de glyphes Noto Sans Bold et Italic provenant du projet fourni `maplibre-offline-pmtiles-main.zip`, sous `app/src/main/assets/glyphs/`.
- Les fichiers Regular existants, le GPS, MainActivity et les vues de carte n’ont pas été modifiés.
- Vérification de l’intégrité du ZIP et de la présence des glyphes.

## Important : limitations
- La bibliothèque MapLibre Offline PMTiles fournie n’est PAS intégrée comme moteur de téléchargement : son stockage OPFS dépend du contexte d’origine et nécessite une adaptation et des tests WebView Android.
- ROADGUARD dispose déjà d’un lecteur natif PMTiles via MainActivity/PmtilesReader et d’un pont `roadguard.local`. Éviter de superposer un second mécanisme OPFS sans tests.
- Aucun pack France + Corse n’est inclus. Aucun téléchargement réel ni APK compilée ou testée.
- Le statut 42% est une estimation historique, pas une métrique mesurée.

## Priorités
1. Obtenir un pack PMTiles France + Corse compatible avec les couches de la vue.
2. Vérifier l’hébergement HTTPS, la taille, le hash et le téléchargement avec reprise.
3. Tester affichage carte, labels et GPS en mode avion sur Xiaomi.
4. Compléter la validation de chaque chantier et le tableau de progression.

## Intervention utilisateur
Aucune pour la préparation du code; un test Xiaomi sera obligatoire à la validation finale.
