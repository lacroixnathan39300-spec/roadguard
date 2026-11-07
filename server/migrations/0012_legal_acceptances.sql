-- Preuves d'acceptation de l'avertissement légal : date, version du texte, choix pour les alertes radars
-- (case d'activation cochée ou non), appareil, compte (vide avant la connexion), appli et version.
CREATE TABLE IF NOT EXISTS legal_acceptances (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  at INTEGER NOT NULL,
  received_at INTEGER NOT NULL,
  version INTEGER NOT NULL,
  device TEXT NOT NULL,
  username TEXT NOT NULL DEFAULT '',
  app TEXT NOT NULL,
  app_version TEXT NOT NULL,
  radars INTEGER NOT NULL,
  UNIQUE (device, version, radars, username)
);
CREATE INDEX IF NOT EXISTS legal_acceptances_user ON legal_acceptances(username);
