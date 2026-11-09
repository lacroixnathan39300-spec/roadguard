import { test } from "node:test";
import assert from "node:assert/strict";
import { box, cleanMessage, cleanName, detectJams, distanceM, extendedExpiry, shouldBecomePermanent, shouldRemove, TYPES } from "../src/index.ts";

const H = 3_600_000, D = 24 * H;

test("distance et rectangle", () => {
  const d = distanceM(46.6758, 5.5574, 46.7322, 5.8825); // Lons → Ney
  assert.ok(d > 25_000 && d < 26_500, String(d));
  const b = box(46.7, 5.6, 1000);
  assert.ok(b.maxLat - b.minLat > 0.017 && b.maxLat - b.minLat < 0.019);
});

test("pseudo", () => {
  assert.equal(cleanName("  Nathan  39 "), "Nathan 39");
  assert.equal(cleanName("a"), null);
  assert.equal(cleanName("<script>"), "script");
});

test("confirmation prolonge la durée", () => {
  const now = 1_000_000_000;
  assert.equal(extendedExpiry("police", now + 10_000, now), now + TYPES.police.life / 2);
  assert.equal(extendedExpiry("travaux", now + 4 * D, now), now + 4 * D);
});

test("gardé à vie", () => {
  const now = 100 * D;
  assert.equal(shouldBecomePermanent("danger", 5, now - 4 * D, now), true);
  assert.equal(shouldBecomePermanent("danger", 4, now - 4 * D, now), false);
  assert.equal(shouldBecomePermanent("danger", 9, now - 1 * D, now), false);
  assert.equal(shouldBecomePermanent("police", 50, now - 30 * D, now), false);
});

test("suppression", () => {
  assert.equal(shouldRemove(0, 2, false), true);
  assert.equal(shouldRemove(5, 1, false), false);
  assert.equal(shouldRemove(0, 1, false), false);
  assert.equal(shouldRemove(10, 3, true), false);
  assert.equal(shouldRemove(10, 4, true), true);
});

import { cleanUsername, grant, isActive, TRIAL_MS } from "../src/license.ts";

test("comptes : identifiant, accès et actions de l'administrateur", () => {
  assert.equal(cleanUsername("  Nathan.L "), "nathan.l");
  assert.equal(cleanUsername("ab"), null);
  const now = 1_000_000_000_000;
  assert.ok(isActive({ access_until: now + TRIAL_MS, blocked: 0, admin: 0 }, now));
  assert.ok(!isActive({ access_until: now - 1, blocked: 0, admin: 0 }, now));
  assert.ok(isActive({ access_until: 0, blocked: 0, admin: 0 }, now));
  assert.ok(!isActive({ access_until: 0, blocked: 1, admin: 0 }, now));
  assert.ok(isActive({ access_until: now - 1, blocked: 1, admin: 1 }, now));
  assert.equal(grant({ access_until: now - 5 }, "unlimited", now)?.access_until, 0);
  assert.equal(grant({ access_until: now + 10 * D }, "month", now)?.access_until, now + 41 * D);
  assert.equal(grant({ access_until: now }, "bidon", now), null);
});

test("bouchons détectés : 2 conducteurs au pas au même endroit", () => {
  const rows = [
    { account_id: "a", lat: 46.70, lon: 5.55, speed: 10 },
    { account_id: "b", lat: 46.7001, lon: 5.5501, speed: 14 },
    { account_id: "c", lat: 46.80, lon: 5.60, speed: 12 },
    { account_id: "d", lat: 46.70, lon: 5.55, speed: 80 },
  ];
  const j = detectJams(rows);
  assert.equal(j.length, 1);
  assert.equal(j[0].n, 2);
  assert.equal(j[0].speed, 12);
});

test("messages : longueur et filtre", () => {
  assert.equal(cleanMessage("  Route glissante  après le virage "), "Route glissante après le virage");
  assert.equal(cleanMessage("ok"), null);
  assert.equal(cleanMessage("espèce de connard"), null);
  assert.equal(cleanMessage("x".repeat(101)), null);
});

test("corrections de carte : limitation valide et note OpenStreetMap", async () => {
  const { cleanLimit, osmNoteText } = await import("../src/index.ts");
  assert.equal(cleanLimit("70|90"), "70|90");
  assert.equal(cleanLimit("75|90"), null);
  assert.equal(cleanLimit("70"), null);
  assert.equal(cleanLimit("<b>|1"), null);
  assert.equal(osmNoteText("limite", "70|90", 1), "Limitation de vitesse sur le terrain : 70 km/h (la carte indique 90 km/h). Confirmé par 2 conducteurs (application Pilote).");
  assert.equal(osmNoteText("rond_point", null, 0), "Rond-point présent sur le terrain mais absent de la carte. Confirmé par 1 conducteur (application Pilote).");
});

test("preuve d'acceptation de l'avertissement légal", async () => {
  const { cleanAcceptance } = await import("../src/index.ts");
  const now = 1_800_000_000_000;
  const ok = cleanAcceptance({ version: 2, device: "a1b2c3d4e5f60718", radars: true, app: "drive", appVersion: "11.14.230", at: now - 5000 }, now);
  assert.deepEqual(ok, { version: 2, device: "a1b2c3d4e5f60718", at: now - 5000, radars: 1, swiss: 0, app: "drive", appVersion: "11.14.230", typed: "" });
  assert.equal(cleanAcceptance({ version: 3, device: "a1b2c3d4e5f60718", typed: "acceptation : « J'accepte »" }, now)?.typed, "acceptation : « J'accepte »");
  assert.equal(cleanAcceptance({ version: 2, device: "a1b2c3d4e5f60718", radars: true, swiss: true }, now)?.swiss, 1);
  // Case non cochée (ou absente) : radars coupés ; date dans le futur ramenée à maintenant.
  assert.equal(cleanAcceptance({ version: 2, device: "a1b2c3d4e5f60718", radars: "true", at: now + 99 }, now)?.radars, 0);
  assert.equal(cleanAcceptance({ version: 2, device: "a1b2c3d4e5f60718", at: now + 99 }, now)?.at, now);
  assert.equal(cleanAcceptance({ version: 0, device: "a1b2c3d4e5f60718" }, now), null);
  assert.equal(cleanAcceptance({ version: 2, device: "court" }, now), null);
  assert.equal(cleanAcceptance({ version: 2, device: "x'; DROP TABLE users;--" }, now), null);
});

test("prénom du conducteur", async () => {
  const { cleanFirstName } = await import("../src/license.ts");
  assert.equal(cleanFirstName("  nathan "), "Nathan");
  assert.equal(cleanFirstName("jean-marc"), "Jean-Marc");
  assert.equal(cleanFirstName("MARIE  CLAIRE"), "Marie Claire");
  assert.equal(cleanFirstName("éloïse"), "Éloïse");
  assert.equal(cleanFirstName("d’artagnan"), "D'Artagnan");
  assert.equal(cleanFirstName("<script>"), "");
  assert.equal(cleanFirstName("R2D2"), "");
  assert.equal(cleanFirstName(42), "");
  assert.equal(cleanFirstName(""), "");
});
