-- Corrections de carte par la communauté : portion de route (OpenStreetMap « way ») d'une limitation corrigée,
-- et numéro de la note OpenStreetMap envoyée une fois la correction confirmée.
ALTER TABLE reports ADD COLUMN way TEXT;
ALTER TABLE reports ADD COLUMN osm_note TEXT;
