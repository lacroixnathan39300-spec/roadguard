-- Convois entre amis, partage de trajet en direct, sauvegarde des réglages du compte.
CREATE TABLE convoy (
  code TEXT NOT NULL,
  account_id TEXT NOT NULL,
  name TEXT NOT NULL,
  lat REAL, lon REAL, speed INTEGER,
  message TEXT, message_at INTEGER,
  at INTEGER NOT NULL,
  PRIMARY KEY (code, account_id)
);
CREATE INDEX convoy_at ON convoy(at);
CREATE TABLE shares (
  token TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  lat REAL, lon REAL, speed INTEGER,
  dest TEXT, eta INTEGER,
  at INTEGER NOT NULL,
  expires_at INTEGER NOT NULL
);
CREATE TABLE user_settings (
  username TEXT PRIMARY KEY,
  data TEXT NOT NULL,
  at INTEGER NOT NULL
);
