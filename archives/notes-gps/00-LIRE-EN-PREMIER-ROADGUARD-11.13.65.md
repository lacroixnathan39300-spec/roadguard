# ROADGUARD 11.13.65 — PASSATION

## Travaux effectivement réalisés
- Ajout `tools/plan_versatiles_regions.py`: produit des commandes VersaTiles CLI de conversion depuis la source mondiale officielle vers des packs régionaux PMTiles, sans téléchargement implicite.
- Ajout `tools/roadguard-first-regions.json`: France métropolitaine, Corse et Suisse (boîtes englobantes approximatives; les tuiles de bord peuvent déborder les frontières).
- Ajout `VERSATILES-PLAN-REGIONS.json`: commandes reproductibles et statut NOT_DOWNLOADED.
- Ajout `tools/test_plan_versatiles_regions.py`: quatre tests unitaires réussis.
- Mise à jour versionName Android vers 11.13.65.

## Limites et blocages
- Les packs ne sont PAS téléchargés, extraits, hébergés ni testés dans MapLibre.
- Aucune APK compilée ni testée sur Xiaomi.
- Les frontières précises et le découpage en secteurs de 50 km restent à construire; les trois régions sont de gros packs et peuvent dépasser la limite Android de 600 MiB.
- Un hébergement HTTPS des packs vérifiés est requis avant téléchargement Android.
- Les URLs de la source de données VersaTiles ne doivent pas être utilisées comme URLs de packs ROADGUARD.
- Respecter les crédits OpenStreetMap et les conditions de la source; éviter les serveurs de démonstration comme infrastructure de production.

## Prochaines priorités
1. Installer/valider le binaire VersaTiles CLI sur machine de préparation, exécuter un petit extrait réel (zoom réduit), inspecter le PMTiles.
2. Découper en secteurs de taille gérable, générer et vérifier SHA256 et catalogues HTTPS stables.
3. Héberger les packs et connecter au synchroniseur Android.
4. Intégrer la lecture MapLibre hors ligne, les ressources de style/fonts et tester l'APK sur Xiaomi.

## Pourcentages historiques estimés, non audités
- Global 42%; GPS 100%; carte en ligne 85%; planification GPS 70%; mémoire 55%; téléchargement réel 20%; arrière-plan 5%; affichage hors ligne 30%; APK/tests Xiaomi 10%.
- Ce jalon n'augmente pas ces pourcentages car aucun nouveau flux Android de bout en bout n'a été validé.
