-- ROADGUARD communauté : comptes, signalements, votes, présence des conducteurs, coucous.
CREATE TABLE accounts (
  id TEXT PRIMARY KEY,
  secret_hash TEXT NOT NULL,
  name TEXT NOT NULL,
  points INTEGER NOT NULL DEFAULT 0,
  created_at INTEGER NOT NULL,
  last_report_at INTEGER NOT NULL DEFAULT 0
);
CREATE TABLE reports (
  id TEXT PRIMARY KEY,
  type TEXT NOT NULL,
  lat REAL NOT NULL,
  lon REAL NOT NULL,
  heading INTEGER,
  author_id TEXT NOT NULL,
  created_at INTEGER NOT NULL,
  expires_at INTEGER NOT NULL,
  yes INTEGER NOT NULL DEFAULT 0,
  no INTEGER NOT NULL DEFAULT 0,
  last_yes_at INTEGER NOT NULL DEFAULT 0,
  permanent INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX reports_geo ON reports(lat, lon);
CREATE INDEX reports_exp ON reports(expires_at);
CREATE TABLE votes (
  report_id TEXT NOT NULL,
  account_id TEXT NOT NULL,
  still INTEGER NOT NULL,
  at INTEGER NOT NULL,
  PRIMARY KEY (report_id, account_id)
);
CREATE TABLE points_log (
  account_id TEXT NOT NULL,
  delta INTEGER NOT NULL,
  at INTEGER NOT NULL
);
CREATE INDEX points_log_at ON points_log(at);
CREATE TABLE presence (
  account_id TEXT PRIMARY KEY,
  lat REAL NOT NULL,
  lon REAL NOT NULL,
  heading INTEGER,
  speed INTEGER,
  at INTEGER NOT NULL
);
CREATE INDEX presence_geo ON presence(lat, lon);
CREATE TABLE waves (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  from_id TEXT NOT NULL,
  to_id TEXT NOT NULL,
  at INTEGER NOT NULL
);
CREATE INDEX waves_to ON waves(to_id);
