import { test } from "node:test";
import assert from "node:assert/strict";
import { stars, level, placement, suspicious, isEarlyNo } from "../src/trust.ts";
import { distanceM } from "../src/index.ts";

const acc = (good: number, bad: number, extra: Partial<{ trusted: number; banned: number; suspect_until: number }> = {}) =>
  ({ good, bad, trusted: 0, banned: 0, suspect_until: 0, ...extra });

test("étoiles : pas de note avant 5 jugements, puis lissée", () => {
  assert.equal(stars(3, 0), null);
  assert.ok((stars(20, 0) ?? 0) >= 4.8);
  assert.ok((stars(2, 8) ?? 5) < 2.5);
});

test("niveaux", () => {
  assert.equal(level(acc(0, 0), 0), "new");
  assert.equal(level(acc(15, 1), 0), "reliable");
  assert.equal(level(acc(1, 9), 0), "low");
  assert.equal(level(acc(0, 0, { trusted: 1 }), 0), "trusted");
  assert.equal(level(acc(15, 1, { suspect_until: 100 }), 50), "suspect");
  assert.equal(level(acc(15, 1, { banned: 1 }), 0), "banned");
});

test("placement selon le niveau et le lancement", () => {
  assert.equal(placement("trusted", "radar_fixe", true, false, false).permanent, true);
  // Personne de confiance : radars mobiles et contrôles restent aussi ; bouchons et accidents passent.
  assert.equal(placement("trusted", "radar_mobile", false, false, false).permanent, true);
  assert.equal(placement("trusted", "police", false, false, false).permanent, true);
  assert.equal(placement("trusted", "bouchon", false, false, false).permanent, false);
  assert.equal(placement("reliable", "radar_mobile", false, false, false).permanent, false);
  assert.equal(placement("new", "radar_fixe", true, false, false).status, "pending");
  assert.equal(placement("new", "radar_fixe", true, true, false).status, "ok"); // région en lancement
  assert.equal(placement("new", "radar_fixe", true, false, true).permanent, true); // radar déjà connu
  assert.equal(placement("low", "police", false, true, false).status, "hidden");
  assert.equal(placement("new", "police", false, false, false).status, "ok");
});

test("anti-abus", () => {
  assert.ok(suspicious(10, null, 46, 5, 0, distanceM));
  assert.ok(suspicious(1, { lat: 46, lon: 5, at: 1 }, 47, 5, 10 * 60_000, distanceM)); // 111 km en 10 min
  assert.equal(suspicious(1, { lat: 46, lon: 5, at: 1 }, 46.5, 5, 60 * 60_000, distanceM), null); // 55 km en 1 h
});

test("démenti tôt = erreur de l'auteur", () => {
  assert.equal(isEarlyNo("police", 0, 10 * 60_000, 0), true);
  assert.equal(isEarlyNo("police", 0, 2 * 3_600_000, 0), false);
  assert.equal(isEarlyNo("radar_fixe", 0, 3_600_000, 1), false);
});
