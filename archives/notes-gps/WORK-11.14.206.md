# ROADGUARD 11.14.206 — inventaire radar Europe

## Fait
- Écran Mises à jour radar : sélecteur de 31 codes pays Lufop.
- Affichage du nombre de points Lufop déjà embarqués pour le pays choisi, calculé hors du thread UI.
- Message explicite : les bases Europe sont disponibles hors ligne et aucune source officielle RadarDroid n'est connectée.
- Garde-fou : le téléchargeur CSV vérifié existant n'accepte toujours que FR/CH ; il ne peut pas écrire sous un code pays non pris en charge.
- Les sept bases radar historiques et le fichier d'overlay Lufop n'ont pas été modifiés.

## À faire
- Liste des 31 codes pays contrôlée par rapport au CSV embarqué.
- Fournisseur de mises à jour autorisé, ZIP et extraction contrôlée ; UI de téléchargement automatique par pays.
- Compilation CodeAssist et test Xiaomi, non réalisés.

## Chantier actif
- Gestionnaire de mises à jour autorisées par pays. La fusion du moteur RadarDroid n'est pas faite.
