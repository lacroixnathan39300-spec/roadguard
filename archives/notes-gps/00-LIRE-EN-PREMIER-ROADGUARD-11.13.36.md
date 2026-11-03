# ROADGUARD 11.13.36 — passation

## Modifié et testé
- `tools/prepare_france_pmtiles.sh`: accepte un chemin local PMTiles relatif ou absolu existant; refuse HTTP non sécurisé; exige une sortie .pmtiles; parse l'en-tête JSON après `pmtiles verify` et préserve le fichier existant en cas d'échec d'extraction.
- `tools/test_prepare_france_111336.py`: trois tests de validation des arguments réussis.
- `bash -n` réussi.

## Non réalisé
- Aucune extraction France + Corse réelle, aucune archive hébergée, aucune APK compilée ou testée sur Xiaomi.
- GPS bleu laissé inchangé.
- Ne pas prétendre que les cartes fonctionnent hors ligne.

## Prochaines priorités
1. Obtenir une URL de build Protomaps réellement accessible et exécuter le pipeline sur une machine disposant de la place et de la bande passante nécessaires.
2. Vérifier l'archive, la couverture et les couches réelles; publier l'archive et son catalogue JSON sur un hébergement HTTPS avec Range et CORS.
3. Intégrer et compiler l'APK; tester téléchargement, reprise, affichage et GPS sur Xiaomi en mode avion.

## Progression
42 % globale: estimation historique non auditée. Les pourcentages par chantier sont indicatifs. Intervention utilisateur seulement si une action devient réellement indispensable, signalée par une pastille rouge explicite.
