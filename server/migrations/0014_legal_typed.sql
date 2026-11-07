-- Preuve d'acceptation : phrases écrites par l'utilisateur lui-même (« J'accepte », « J'active les radars à mes risques »…).
ALTER TABLE legal_acceptances ADD COLUMN typed TEXT NOT NULL DEFAULT '';
