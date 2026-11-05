-- Les comptes créés avec l'ancien calcul du mot de passe (trop lourd pour l'offre gratuite) ne pouvaient
-- pas se connecter : on repart de zéro (aucun vrai utilisateur encore, seulement l'essai de l'administrateur).
DELETE FROM sessions;
DELETE FROM trials;
DELETE FROM users;
