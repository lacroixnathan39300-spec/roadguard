import { test } from "node:test";
import assert from "node:assert/strict";
import { box, cleanName, distanceM, extendedExpiry, shouldBecomePermanent, shouldRemove, TYPES } from "../src/index.ts";

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
