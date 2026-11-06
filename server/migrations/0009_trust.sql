-- Confiance des signalements : note de fiabilité, niveaux (confiance, suspect, banni), signalements « à confirmer ».
ALTER TABLE accounts ADD COLUMN good INTEGER NOT NULL DEFAULT 0;
ALTER TABLE accounts ADD COLUMN bad INTEGER NOT NULL DEFAULT 0;
ALTER TABLE accounts ADD COLUMN trusted INTEGER NOT NULL DEFAULT 0;
ALTER TABLE accounts ADD COLUMN banned INTEGER NOT NULL DEFAULT 0;
ALTER TABLE accounts ADD COLUMN suspect_until INTEGER NOT NULL DEFAULT 0;
ALTER TABLE accounts ADD COLUMN owner TEXT;
ALTER TABLE accounts ADD COLUMN last_lat REAL;
ALTER TABLE accounts ADD COLUMN last_lon REAL;
ALTER TABLE accounts ADD COLUMN last_seen INTEGER NOT NULL DEFAULT 0;
ALTER TABLE accounts ADD COLUMN rep_lat REAL;
ALTER TABLE accounts ADD COLUMN rep_lon REAL;
CREATE INDEX accounts_seen ON accounts(last_seen);
ALTER TABLE reports ADD COLUMN status TEXT NOT NULL DEFAULT 'ok';
ALTER TABLE reports ADD COLUMN validated INTEGER NOT NULL DEFAULT 0;
