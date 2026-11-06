-- Messages des utilisateurs à l'administrateur, et ses réponses.
CREATE TABLE messages (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  at INTEGER NOT NULL,
  username TEXT NOT NULL,
  app TEXT NOT NULL,
  text TEXT NOT NULL,
  reply TEXT,
  reply_at INTEGER,
  read_admin INTEGER NOT NULL DEFAULT 0,
  read_user INTEGER NOT NULL DEFAULT 1
);
CREATE INDEX messages_user ON messages(username, id);
