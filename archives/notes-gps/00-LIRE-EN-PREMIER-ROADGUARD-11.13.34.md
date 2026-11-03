# ROADGUARD 11.13.34 — passation

## Réalisé
- Ajout `tools/produire_france_et_catalogue.sh` : enchaîne extraction Protomaps, `pmtiles verify`, SHA-256 et génération du catalogue JSON existant.
- Aucun changement au GPS bleu ni aux classes Android.
- Vérifications : `bash -n`, invocation invalide, intégrité ZIP.

## Non réalisé / bloqué
- Aucun PMTiles France produit : archive source distante de plusieurs Go non récupérée.
- Aucun hébergement HTTPS disponible et vérifié.
- Aucun téléchargement France fonctionnel dans l'APK.
- Aucune compilation ni recette Xiaomi.

## Suite par priorité
1. Choisir un build officiel Protomaps et produire l'archive France + Corse avec `pmtiles extract`.
2. Héberger l'archive sur un stockage HTTPS avec HTTP Range et CORS, vérifier disponibilité et SHA-256.
3. Publier le catalogue et configurer sa source par défaut dans ROADGUARD (sans saisie manuelle).
4. Compiler une APK, tester téléchargement interrompu/repris et carte en mode avion sur Xiaomi.
5. Auditer les autres chantiers du cahier des charges.

## Suivi
- Estimation historique globale : 42 % (non auditée, inchangée).
- GPS bleu : validé par retour utilisateur ; ne pas modifier.
- Pastille rouge uniquement si action utilisateur indispensable. Pour l'instant aucune action demandée.
