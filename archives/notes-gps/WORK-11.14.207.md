# ROADGUARD 11.14.207 — activation des pays Lufop

## Changements
- Écran Alertes > Mises à jour radar : case à cocher pour activer ou désactiver les points Lufop de chacun des 31 pays.
- Préférences Android privées `radar_country_visibility`, activées par défaut pour conserver le comportement précédent.
- Le catalogue d'alertes ignore désormais les points Lufop des pays désactivés. Les sept bases radar originales ne sont ni effacées ni modifiées.
- Après changement, relancer la navigation pour reconstruire le catalogue en mémoire.

## À faire / limites
- Téléchargement automatique RadarDroid toujours non implémenté.
- Pas de test de compilation Android ni de validation sur Xiaomi dans cette livraison.
- Les données France/Suisse historiques restent gérées par les interrupteurs existants.
- Le filtre par pays ne modifie pas les autres fournisseurs de données éventuellement chargés séparément.

## Validation statique
- Comparer SHA-256 des assets radar avec la version 11.14.206.
- Vérifier l'écran et les préférences en compilation finale.
