# ROADGUARD 11.13.56 — Préparation vérifiable de régions PMTiles

## État réel
- Projet source Android complet issu du ZIP 11.13.55; **pas d'APK compilée**.
- Ajout `tools/prepare_pmtiles_region.py` : extraction d'une bbox depuis un PMTiles local avec go-pmtiles, vérification PMTiles v3, manifeste JSON (bbox, taille, SHA-256, attribution), protection contre écrasement.
- Essai effectué sur le fichier de démonstration Florence `protomaps(vector)ODbL_firenze.pmtiles`, bbox `11.20,43.75,11.28,43.81` : sortie `demo-firenze.pmtiles` (environ 3,1 Mo), `pmtiles verify` réussi.
- Le ZIP inclut la **démonstration Florence uniquement** sous `samples/demo-firenze/`. Il ne contient **aucune carte France**, aucun fournisseur/hébergement de carte France connecté, ni téléchargement Android fonctionnel.
- Le binaire go-pmtiles Linux fourni par l'utilisateur est **non inclus** dans ce ZIP source : le script accepte son chemin via `--pmtiles-cli`.

## Prochaine priorité
1. Obtenir une source France + Corse PMTiles exploitable et redistribuable légalement (données, style, attribution, hébergement).
2. Définir une grille de paquets réellement téléchargeables et son manifeste distant; comparer aux cellules GPS 50 km du planificateur.
3. Implémenter le téléchargement Android (reprise, stockage, SHA-256, gestion du réseau, arrière-plan), puis relier le rendu MapLibre hors ligne.
4. Compiler l'APK et tester sur Xiaomi avec GPS bleu et caméra.

## Blocages
- Pas de source France PMTiles et d'URL d'hébergement fonctionnelle fournis ou déployés.
- Pas de validation Android/SDK dans cet environnement.

## Avancement
Global **42 % estimé, non audité**, inchangé. La préparation de régions a été validée sur échantillon, pas sur la France ni sur Android. Voir `SUIVI-AVANCEMENT.md` et `app/src/main/assets/roadguard_progress.json`.
