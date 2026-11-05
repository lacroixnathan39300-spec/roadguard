-- Compte créé deux fois par une requête renvoyée (réponse perdue) : on repart de zéro (pas encore de vrais utilisateurs).
DELETE FROM sessions;
DELETE FROM trials;
DELETE FROM users;
