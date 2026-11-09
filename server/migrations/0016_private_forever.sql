-- Signalements privés : faits en Suisse (avertir publiquement des contrôles y est puni), visibles par leur auteur seul.
ALTER TABLE reports ADD COLUMN private INTEGER NOT NULL DEFAULT 0;
-- Personnes de confiance : leurs signalements restent (sauf bouchons, accidents, messages), y compris ceux encore en cours.
UPDATE reports SET permanent = 1
 WHERE author_id IN (SELECT id FROM accounts WHERE trusted = 1)
   AND type IN ('radar_fixe', 'radar_mobile', 'police', 'danger', 'travaux', 'limite', 'rond_point', 'route')
   AND status != 'hidden';
