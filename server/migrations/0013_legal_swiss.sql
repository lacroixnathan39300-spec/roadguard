-- Preuve d'acceptation : ajout du choix « mode Suisse coupé » (case dédiée), compris dans l'unicité.
ALTER TABLE legal_acceptances RENAME TO legal_acceptances_v1;
CREATE TABLE legal_acceptances (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  at INTEGER NOT NULL,
  received_at INTEGER NOT NULL,
  version INTEGER NOT NULL,
  device TEXT NOT NULL,
  username TEXT NOT NULL DEFAULT '',
  app TEXT NOT NULL,
  app_version TEXT NOT NULL,
  radars INTEGER NOT NULL,
  swiss INTEGER NOT NULL DEFAULT 0,
  UNIQUE (device, version, radars, swiss, username)
);
INSERT INTO legal_acceptances (at, received_at, version, device, username, app, app_version, radars, swiss)
  SELECT at, received_at, version, device, username, app, app_version, radars, 0 FROM legal_acceptances_v1;
DROP TABLE legal_acceptances_v1;
CREATE INDEX IF NOT EXISTS legal_acceptances_user2 ON legal_acceptances(username);
