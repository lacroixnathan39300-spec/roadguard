# ROADGUARD 11.14.221 — Verrouillage import local

## Modifications
- Rejet explicite si le répertoire privé de mises à jour ne peut pas être créé.
- Contrôle du flux ASC ouvert avant conversion.
- Erreurs numériques CSV converties en messages compréhensibles avec numéro de ligne.
- Remise à zéro de la barre après échec d’import.

## Validations réalisées
- Archive ZIP générée et vérifiée (CRC).
- Comparaison SHA-256 de tous les fichiers de données CSV/ASC entre 11.14.220 et 11.14.221.
- Contrôle statique des modifications de code.

## Non validé
- Compilation Android complète et tests sur Xiaomi.
- Téléchargement automatique par pays façon RadarDroid.
- Navigation Android Auto.

## Prochaines tâches
- Identifier une source autorisée de ZIP par pays, accessible sans session privée RadarDroid.
- Ajouter catalogue de sources et téléchargement en un clic, avec vérification de l’archive.
- Tester import, restauration et rechargement d’alertes sur appareil.

## Intervention utilisateur
Aucune immédiatement. Une source de téléchargement autorisée ou une documentation de fournisseur sera nécessaire pour le téléchargement automatique. Compilation à demander à la fin.
