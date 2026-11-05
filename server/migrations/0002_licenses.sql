-- Comptes ROADGUARD (connexion) : essai d'un mois, puis accès accordé par l'administrateur.
CREATE TABLE users (
  username TEXT PRIMARY KEY,
  pass_hash TEXT NOT NULL,
  salt TEXT NOT NULL,
  created_at INTEGER NOT NULL,
  -- Fin de l'accès (essai ou accordé) ; 0 = illimité.
  access_until INTEGER NOT NULL,
  plan TEXT NOT NULL DEFAULT 'trial',
  blocked INTEGER NOT NULL DEFAULT 0,
  admin INTEGER NOT NULL DEFAULT 0,
  last_seen INTEGER NOT NULL DEFAULT 0
);
CREATE TABLE sessions (
  token_hash TEXT PRIMARY KEY,
  username TEXT NOT NULL,
  device TEXT NOT NULL,
  app TEXT NOT NULL,
  created_at INTEGER NOT NULL,
  last_seen INTEGER NOT NULL
);
CREATE INDEX sessions_user ON sessions(username);
-- Un seul essai gratuit par appareil (pas de nouvel essai en créant un autre compte).
CREATE TABLE trials (
  device TEXT PRIMARY KEY,
  username TEXT NOT NULL,
  at INTEGER NOT NULL
);
