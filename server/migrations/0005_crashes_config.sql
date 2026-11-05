-- Rapports de plantage envoyés par les applis, et réglages de l'administrateur (contact affiché…).
CREATE TABLE crashes (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  at INTEGER NOT NULL,
  app TEXT NOT NULL,
  version TEXT NOT NULL,
  device TEXT NOT NULL,
  username TEXT,
  text TEXT NOT NULL
);
CREATE INDEX crashes_at ON crashes(at);
CREATE TABLE config (
  key TEXT PRIMARY KEY,
  value TEXT NOT NULL
);
