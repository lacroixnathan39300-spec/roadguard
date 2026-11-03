# ROADGUARD 11.14.205 — Fusion Lufop Europe

## Réalisé
- Le catalogue Lufop ASC embarqué est maintenant lu une seule fois et mis en cache en mémoire.
- Les radars fixes et feux rouges des 31 pays de l’overlay sont ajoutés au catalogue partagé utilisé par l’interface, le service d’alertes en arrière-plan et CarRouteData.
- Déduplication géographique par pays à 5 décimales, en complément des radars FR/CH déjà intégrés.
- Les emplacements mobiles hors FR/CH restent exclus par prudence, en l’absence d’un réglage par pays. Les réglages historiques FR/CH sont conservés.
- Les sept fichiers d’origine restent inchangés.

## À faire / blocages
- Le téléchargement ZIP automatique à la RadarDroid n’est toujours pas opérationnel. Aucun accès protégé n’est contourné.
- Ajouter une sélection explicite des pays et une gestion des catégories mobiles par pays.
- Vérifier le coût mémoire et les alertes transfrontalières sur Xiaomi.
- Compilation et tests Android non réalisés.

## Version
11.14.205 (source seulement)
