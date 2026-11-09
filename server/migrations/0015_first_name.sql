-- Prénom du conducteur : le GPS l'utilise pour s'adresser à lui dans ses phrases.
ALTER TABLE users ADD COLUMN first_name TEXT NOT NULL DEFAULT '';
