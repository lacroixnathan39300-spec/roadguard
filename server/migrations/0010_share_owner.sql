-- Partage de trajet : seul le compte qui l'a lancé peut le mettre à jour ou l'arrêter
-- (le jeton du lien public ne suffit plus à déplacer la position affichée).
ALTER TABLE shares ADD COLUMN owner TEXT;
