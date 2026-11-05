/**
 * ROADGUARD communauté (Cloudflare Worker + base D1).
 *
 * - Compte par appareil : identifiant + secret (pas de mot de passe), pseudo affiché.
 * - Signalements façon Waze : création, vérification « toujours là ? », expiration, gardés à vie si confirmés.
 * - Points et classement (semaine / général).
 * - Conducteurs à proximité (seulement s'ils partagent leur position) et « coucou ».
 */

import { licenseRoutes } from "./license.ts";

export interface Env {
  DB: D1Database;
  /** Applis à jour (APK découpés en morceaux de 20 Mo), déposées par la compilation automatique. */
  APPS?: KVNamespace;
}

const MIN = 60_000;
const HOUR = 60 * MIN;
const DAY = 24 * HOUR;

/** Types de signalement et durée de vie de base (prolongée à chaque confirmation). */
export const TYPES: Record<string, { label: string; life: number; canBePermanent: boolean }> = {
  police: { label: "Police", life: 1 * HOUR, canBePermanent: false },
  radar_mobile: { label: "Radar mobile", life: 2 * HOUR, canBePermanent: false },
  accident: { label: "Accident", life: 2 * HOUR, canBePermanent: false },
  bouchon: { label: "Bouchon", life: 45 * MIN, canBePermanent: false },
  danger: { label: "Danger", life: 6 * HOUR, canBePermanent: true },
  travaux: { label: "Travaux", life: 5 * DAY, canBePermanent: false },
  radar_fixe: { label: "Radar fixe", life: 14 * DAY, canBePermanent: true },
  message: { label: "Message", life: 24 * HOUR, canBePermanent: false },
};

export const RULES = {
  /** Même type à moins de 150 m d'un signalement actif : c'est une confirmation, pas un doublon. */
  mergeRadiusM: 150,
  /** Gardé à vie : au moins 5 personnes différentes l'ont confirmé, sur au moins 3 jours. */
  permanentVoters: 5,
  permanentSpan: 3 * DAY,
  /** Supprimé : 2 « plus là » depuis la dernière confirmation (ou plus de « non » que de « oui »). */
  removeNo: 2,
  pointsReport: 5,
  pointsConfirmed: 2,
  pointsVote: 1,
  pointsRemoved: -3,
  reportCooldown: 20_000,
  driversWindow: 3 * MIN,
};

export function distanceM(lat1: number, lon1: number, lat2: number, lon2: number): number {
  const r = 6371000;
  const dLat = ((lat2 - lat1) * Math.PI) / 180;
  const dLon = ((lon2 - lon1) * Math.PI) / 180;
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos((lat1 * Math.PI) / 180) * Math.cos((lat2 * Math.PI) / 180) * Math.sin(dLon / 2) ** 2;
  return 2 * r * Math.asin(Math.min(1, Math.sqrt(a)));
}

/** Rectangle autour d'un point (pour l'index lat/lon). */
export function box(lat: number, lon: number, radiusM: number) {
  const dLat = radiusM / 111_320;
  const dLon = radiusM / (111_320 * Math.max(0.1, Math.cos((lat * Math.PI) / 180)));
  return { minLat: lat - dLat, maxLat: lat + dLat, minLon: lon - dLon, maxLon: lon + dLon };
}

/** Nouvelle échéance après une confirmation : au moins la moitié de la durée de base à partir de maintenant. */
export function extendedExpiry(type: string, current: number, now: number): number {
  const life = TYPES[type]?.life ?? HOUR;
  return Math.max(current, now + life / 2);
}

export function shouldBecomePermanent(type: string, distinctYes: number, createdAt: number, now: number): boolean {
  return !!TYPES[type]?.canBePermanent && distinctYes >= RULES.permanentVoters && now - createdAt >= RULES.permanentSpan;
}

export function shouldRemove(yes: number, noSinceLastYes: number, permanent: boolean): boolean {
  // Un signalement gardé à vie demande plus de démentis.
  return permanent ? noSinceLastYes >= RULES.removeNo * 2 : noSinceLastYes >= RULES.removeNo || noSinceLastYes > yes + 1;
}

// ---------- outils HTTP ----------

const CORS = {
  "access-control-allow-origin": "*",
  "access-control-allow-headers": "authorization, content-type",
  "access-control-allow-methods": "GET, POST, PATCH, OPTIONS",
};

function json(data: unknown, status = 200): Response {
  return new Response(JSON.stringify(data), { status, headers: { "content-type": "application/json; charset=utf-8", ...CORS } });
}

function fail(status: number, message: string): Response {
  return json({ error: message }, status);
}

async function sha256(text: string): Promise<string> {
  const d = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(text));
  return [...new Uint8Array(d)].map((b) => b.toString(16).padStart(2, "0")).join("");
}

function randomId(bytes = 12): string {
  const a = new Uint8Array(bytes);
  crypto.getRandomValues(a);
  return [...a].map((b) => b.toString(16).padStart(2, "0")).join("");
}

/** Mots refusés dans les messages (filtre simple ; l'administrateur peut supprimer un message). */
const BANNED = ["connard", "connasse", "salope", "pute", "enculé", "encule", "nique", "fdp", "batard", "bâtard", "pd", "negro", "bougnoul"];

export function cleanMessage(raw: unknown): string | null {
  if (typeof raw !== "string") return null;
  const t = raw.normalize("NFC").replace(/[\u0000-\u001f<>]/g, "").replace(/\s+/g, " ").trim();
  if (t.length < 3 || t.length > 100) return null;
  const words = t.toLowerCase().normalize("NFD").replace(/\p{M}/gu, "").split(/[^a-z0-9]+/);
  const banned = BANNED.map((b) => b.normalize("NFD").replace(/\p{M}/gu, ""));
  return words.some((w) => banned.includes(w)) ? null : t;
}

export function cleanName(raw: unknown): string | null {
  if (typeof raw !== "string") return null;
  const n = raw.normalize("NFC").replace(/[\u0000-\u001f<>]/g, "").replace(/\s+/g, " ").trim();
  return n.length >= 2 && n.length <= 24 ? n : null;
}

function num(v: unknown, min: number, max: number): number | null {
  const n = typeof v === "number" ? v : typeof v === "string" ? Number(v) : NaN;
  return Number.isFinite(n) && n >= min && n <= max ? n : null;
}

interface Account {
  id: string;
  name: string;
  points: number;
  last_report_at: number;
}

async function auth(req: Request, env: Env): Promise<Account | null> {
  // Clé dans l'en-tête, ou dans l'adresse (« ?key= ») : les requêtes qui passent par le téléphone
  // (liaison ROADGUARD Link) ne transportent pas d'en-têtes personnalisés.
  const h = req.headers.get("authorization") ?? `Bearer ${new URL(req.url).searchParams.get("key") ?? ""}`;
  const m = /^Bearer ([0-9a-f]{24})\.([0-9a-f]{64})$/.exec(h);
  if (!m) return null;
  const row = await env.DB.prepare("SELECT id, name, points, last_report_at, secret_hash FROM accounts WHERE id = ?")
    .bind(m[1]).first<Account & { secret_hash: string }>();
  if (!row || row.secret_hash !== (await sha256(m[2]))) return null;
  return row;
}

async function addPoints(env: Env, accountId: string, delta: number, now: number) {
  await env.DB.batch([
    env.DB.prepare("UPDATE accounts SET points = MAX(0, points + ?) WHERE id = ?").bind(delta, accountId),
    env.DB.prepare("INSERT INTO points_log (account_id, delta, at) VALUES (?, ?, ?)").bind(accountId, delta, now),
  ]);
}

// ---------- routes ----------

async function createAccount(req: Request, env: Env): Promise<Response> {
  const body = (await req.json().catch(() => ({}))) as { name?: unknown };
  const name = cleanName(body.name);
  if (!name) return fail(400, "Pseudo de 2 à 24 caractères");
  const id = randomId(12);
  const secret = randomId(32);
  await env.DB.prepare("INSERT INTO accounts (id, secret_hash, name, created_at) VALUES (?, ?, ?, ?)")
    .bind(id, await sha256(secret), name, Date.now()).run();
  return json({ id, secret, name, points: 0 });
}

async function reportsNear(url: URL, env: Env, me: Account | null): Promise<Response> {
  const lat = num(url.searchParams.get("lat"), -90, 90);
  const lon = num(url.searchParams.get("lon"), -180, 180);
  const radius = num(url.searchParams.get("radius"), 100, 100_000) ?? 30_000;
  if (lat == null || lon == null) return fail(400, "lat et lon requis");
  const now = Date.now();
  const b = box(lat, lon, radius);
  const rows = await env.DB.prepare(
    `SELECT r.id, r.type, r.lat, r.lon, r.heading, r.created_at, r.expires_at, r.yes, r.no, r.permanent, r.text,
            a.name AS author, (r.author_id = ?) AS mine,
            (SELECT still FROM votes v WHERE v.report_id = r.id AND v.account_id = ?) AS my_vote
       FROM reports r JOIN accounts a ON a.id = r.author_id
      WHERE r.lat BETWEEN ? AND ? AND r.lon BETWEEN ? AND ? AND (r.permanent = 1 OR r.expires_at > ?)
      LIMIT 2000`,
  ).bind(me?.id ?? "", me?.id ?? "", b.minLat, b.maxLat, b.minLon, b.maxLon, now).all();
  return json({ now, reports: rows.results });
}

async function createReport(req: Request, env: Env, me: Account): Promise<Response> {
  const body = (await req.json().catch(() => ({}))) as Record<string, unknown>;
  const type = typeof body.type === "string" && TYPES[body.type] ? body.type : null;
  const lat = num(body.lat, -90, 90);
  const lon = num(body.lon, -180, 180);
  const heading = num(body.heading, 0, 360);
  if (!type || lat == null || lon == null) return fail(400, "Type ou position invalide");
  const text = type === "message" ? cleanMessage(body.text) : null;
  if (type === "message" && !text) return fail(400, "Message de 3 à 100 caractères, sans insulte");
  const now = Date.now();
  if (now - me.last_report_at < RULES.reportCooldown) return fail(429, "Patiente quelques secondes entre deux signalements");

  // Déjà signalé tout près : on confirme au lieu de créer un doublon.
  const b = box(lat, lon, RULES.mergeRadiusM);
  const near = await env.DB.prepare(
    "SELECT id, lat, lon FROM reports WHERE type = ? AND lat BETWEEN ? AND ? AND lon BETWEEN ? AND ? AND (permanent = 1 OR expires_at > ?)",
  ).bind(type, b.minLat, b.maxLat, b.minLon, b.maxLon, now).all<{ id: string; lat: number; lon: number }>();
  const existing = near.results.find((r) => distanceM(lat, lon, r.lat, r.lon) <= RULES.mergeRadiusM);
  await env.DB.prepare("UPDATE accounts SET last_report_at = ? WHERE id = ?").bind(now, me.id).run();
  // Les messages ne se fusionnent pas : chacun garde son texte.
  if (existing && type !== "message") return vote(env, me, existing.id, true, now, true);

  const id = randomId(10);
  await env.DB.prepare(
    "INSERT INTO reports (id, type, lat, lon, heading, author_id, created_at, expires_at, last_yes_at, text) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
  ).bind(id, type, lat, lon, heading == null ? null : Math.round(heading), me.id, now, now + TYPES[type].life, now, text).run();
  await addPoints(env, me.id, RULES.pointsReport, now);
  return json({ id, created: true, points: me.points + RULES.pointsReport });
}

async function vote(env: Env, me: Account, reportId: string, still: boolean, now = Date.now(), merged = false): Promise<Response> {
  const r = await env.DB.prepare("SELECT * FROM reports WHERE id = ?").bind(reportId).first<{
    id: string; type: string; author_id: string; created_at: number; expires_at: number; yes: number; no: number;
    last_yes_at: number; permanent: number;
  }>();
  if (!r) return fail(404, "Signalement introuvable");
  if (r.author_id === me.id) {
    // L'auteur peut retirer son propre signalement (« plus là »), sans perte de points.
    if (!still) await env.DB.prepare("DELETE FROM reports WHERE id = ?").bind(r.id).run();
    return json({ id: r.id, removed: !still, merged });
  }
  const previous = await env.DB.prepare("SELECT still FROM votes WHERE report_id = ? AND account_id = ?")
    .bind(r.id, me.id).first<{ still: number }>();
  if (previous && previous.still === (still ? 1 : 0)) return json({ id: r.id, already: true, merged });
  await env.DB.prepare(
    "INSERT INTO votes (report_id, account_id, still, at) VALUES (?, ?, ?, ?) ON CONFLICT(report_id, account_id) DO UPDATE SET still = excluded.still, at = excluded.at",
  ).bind(r.id, me.id, still ? 1 : 0, now).run();
  await addPoints(env, me.id, RULES.pointsVote, now);

  if (still) {
    const distinctYes = (await env.DB.prepare("SELECT COUNT(*) AS n FROM votes WHERE report_id = ? AND still = 1").bind(r.id).first<{ n: number }>())?.n ?? 0;
    const permanent = r.permanent === 1 || shouldBecomePermanent(r.type, distinctYes, r.created_at, now);
    await env.DB.prepare("UPDATE reports SET yes = yes + 1, last_yes_at = ?, expires_at = ?, permanent = ? WHERE id = ?")
      .bind(now, extendedExpiry(r.type, r.expires_at, now), permanent ? 1 : 0, r.id).run();
    await addPoints(env, r.author_id, RULES.pointsConfirmed, now);
    return json({ id: r.id, confirmed: true, permanent, merged });
  }
  const noSince = (await env.DB.prepare("SELECT COUNT(*) AS n FROM votes WHERE report_id = ? AND still = 0 AND at >= ?")
    .bind(r.id, r.last_yes_at).first<{ n: number }>())?.n ?? 0;
  if (shouldRemove(r.yes, noSince, r.permanent === 1)) {
    await env.DB.prepare("DELETE FROM reports WHERE id = ?").bind(r.id).run();
    await env.DB.prepare("DELETE FROM votes WHERE report_id = ?").bind(r.id).run();
    await addPoints(env, r.author_id, RULES.pointsRemoved, now);
    return json({ id: r.id, removed: true });
  }
  await env.DB.prepare("UPDATE reports SET no = no + 1 WHERE id = ?").bind(r.id).run();
  return json({ id: r.id, removed: false });
}

async function leaderboard(url: URL, env: Env, me: Account | null): Promise<Response> {
  const week = url.searchParams.get("period") === "week";
  const rows = week
    ? await env.DB.prepare(
        `SELECT a.id, a.name, SUM(p.delta) AS points FROM points_log p JOIN accounts a ON a.id = p.account_id
          WHERE p.at > ? GROUP BY a.id HAVING points > 0 ORDER BY points DESC LIMIT 50`,
      ).bind(Date.now() - 7 * DAY).all<{ id: string; name: string; points: number }>()
    : await env.DB.prepare("SELECT id, name, points FROM accounts WHERE points > 0 ORDER BY points DESC LIMIT 50")
        .all<{ id: string; name: string; points: number }>();
  const list = rows.results.map((r, i) => ({ rank: i + 1, name: r.name, points: r.points, me: r.id === me?.id }));
  return json({ period: week ? "week" : "all", list });
}

async function presence(req: Request, env: Env, me: Account): Promise<Response> {
  const body = (await req.json().catch(() => ({}))) as Record<string, unknown>;
  if (body.visible === false) {
    await env.DB.prepare("DELETE FROM presence WHERE account_id = ?").bind(me.id).run();
    return json({ visible: false });
  }
  const lat = num(body.lat, -90, 90);
  const lon = num(body.lon, -180, 180);
  if (lat == null || lon == null) return fail(400, "Position invalide");
  // Position arrondie (~100 m) : on voit qu'un conducteur est là, pas son adresse exacte.
  const rLat = Math.round(lat * 1000) / 1000;
  const rLon = Math.round(lon * 1000) / 1000;
  await env.DB.prepare(
    "INSERT INTO presence (account_id, lat, lon, heading, speed, at) VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT(account_id) DO UPDATE SET lat = excluded.lat, lon = excluded.lon, heading = excluded.heading, speed = excluded.speed, at = excluded.at",
  ).bind(me.id, rLat, rLon, num(body.heading, 0, 360), num(body.speed, 0, 400), Date.now()).run();
  return json({ visible: true });
}

async function drivers(url: URL, env: Env, me: Account): Promise<Response> {
  const lat = num(url.searchParams.get("lat"), -90, 90);
  const lon = num(url.searchParams.get("lon"), -180, 180);
  if (lat == null || lon == null) return fail(400, "lat et lon requis");
  return driversNear(lat, lon, env, me);
}

async function driversNear(lat: number, lon: number, env: Env, me: Account): Promise<Response> {
  const b = box(lat, lon, 20_000);
  const rows = await env.DB.prepare(
    `SELECT p.account_id AS id, a.name, p.lat, p.lon, p.heading, p.speed, p.at FROM presence p JOIN accounts a ON a.id = p.account_id
      WHERE p.lat BETWEEN ? AND ? AND p.lon BETWEEN ? AND ? AND p.at > ? AND p.account_id != ? LIMIT 200`,
  ).bind(b.minLat, b.maxLat, b.minLon, b.maxLon, Date.now() - RULES.driversWindow, me.id).all();
  return json({ drivers: rows.results });
}

async function wave(req: Request, env: Env, me: Account): Promise<Response> {
  const body = (await req.json().catch(() => ({}))) as { to?: unknown };
  if (typeof body.to !== "string" || !/^[0-9a-f]{24}$/.test(body.to)) return fail(400, "Destinataire invalide");
  const recent = await env.DB.prepare("SELECT COUNT(*) AS n FROM waves WHERE from_id = ? AND to_id = ? AND at > ?")
    .bind(me.id, body.to, Date.now() - 2 * MIN).first<{ n: number }>();
  if ((recent?.n ?? 0) > 0) return json({ sent: false, reason: "déjà fait signe" });
  await env.DB.prepare("INSERT INTO waves (from_id, to_id, at) VALUES (?, ?, ?)").bind(me.id, body.to, Date.now()).run();
  return json({ sent: true });
}

async function inbox(env: Env, me: Account): Promise<Response> {
  const rows = await env.DB.prepare(
    "SELECT w.id, a.name AS from_name, w.at FROM waves w JOIN accounts a ON a.id = w.from_id WHERE w.to_id = ? ORDER BY w.at",
  ).bind(me.id).all<{ id: number }>();
  if (rows.results.length) await env.DB.prepare("DELETE FROM waves WHERE to_id = ?").bind(me.id).run();
  return json({ waves: rows.results });
}

/**
 * Une seule requête par minute et par conducteur : position envoyée (si partagée), et en retour
 * signalements autour, conducteurs proches, coucous reçus et points. Économise le quota gratuit.
 */
async function sync(req: Request, env: Env, me: Account | null): Promise<Response> {
  const body = (await req.json().catch(() => ({}))) as Record<string, unknown>;
  const lat = num(body.lat, -90, 90);
  const lon = num(body.lon, -180, 180);
  if (lat == null || lon == null) return fail(400, "Position invalide");
  const q = new URL(req.url);
  q.searchParams.set("lat", String(lat));
  q.searchParams.set("lon", String(lon));
  q.searchParams.set("radius", String(num(body.radius, 1000, 100_000) ?? 30_000));
  const reports = (await (await reportsNear(q, env, me)).json()) as { reports: unknown[] };
  if (!me) return json({ reports: reports.reports, drivers: [], waves: [], points: null, jams: await jamsNear(lat, lon, env) });
  let drivers: unknown[] = [];
  if (body.share === true) {
    await presence(new Request(req.url, { method: "POST", body: JSON.stringify({ lat, lon, heading: body.heading, speed: body.speed }) }), env, me);
    drivers = ((await (await driversNear(lat, lon, env, me)).json()) as { drivers: unknown[] }).drivers;
  } else if (body.share === false) {
    await env.DB.prepare("DELETE FROM presence WHERE account_id = ?").bind(me.id).run();
  }
  const waves = ((await (await inbox(env, me)).json()) as { waves: unknown[] }).waves;
  return json({ reports: reports.reports, drivers, waves, points: me.points, name: me.name, jams: await jamsNear(lat, lon, env) });
}

/**
 * Bouchons détectés automatiquement : au moins 2 conducteurs ROADGUARD différents roulant au pas (3 à 25 km/h)
 * dans la même case d'environ 250 m au cours des 10 dernières minutes.
 */
export function detectJams(rows: { account_id: string; lat: number; lon: number; speed: number | null }[]) {
  const cells = new Map<string, { ids: Set<string>; lat: number; lon: number; speed: number; n: number }>();
  for (const r of rows) {
    if (r.speed == null || r.speed < 3 || r.speed > 25) continue;
    const key = `${Math.round(r.lat * 450)}:${Math.round(r.lon * 320)}`;
    const c = cells.get(key) ?? { ids: new Set<string>(), lat: 0, lon: 0, speed: 0, n: 0 };
    c.ids.add(r.account_id); c.lat += r.lat; c.lon += r.lon; c.speed += r.speed; c.n++;
    cells.set(key, c);
  }
  return [...cells.values()].filter((c) => c.ids.size >= 2)
    .map((c) => ({ lat: c.lat / c.n, lon: c.lon / c.n, speed: Math.round(c.speed / c.n), n: c.ids.size }));
}

async function jamsNear(lat: number, lon: number, env: Env) {
  const b = box(lat, lon, 30_000);
  const rows = await env.DB.prepare(
    "SELECT account_id, lat, lon, speed FROM presence WHERE at > ? AND lat BETWEEN ? AND ? AND lon BETWEEN ? AND ?"
  ).bind(Date.now() - 10 * MIN, b.minLat, b.maxLat, b.minLon, b.maxLon).all<{ account_id: string; lat: number; lon: number; speed: number | null }>();
  return detectJams(rows.results);
}

/** Chiffres d'utilisation (pour suivre l'approche des limites de l'offre gratuite). */
async function stats(env: Env): Promise<Response> {
  const now = Date.now();
  const [drivers, reports, accounts] = await Promise.all([
    env.DB.prepare("SELECT COUNT(*) AS n FROM presence WHERE at > ?").bind(now - HOUR).first<{ n: number }>(),
    env.DB.prepare("SELECT COUNT(*) AS n FROM reports WHERE permanent = 1 OR expires_at > ?").bind(now).first<{ n: number }>(),
    env.DB.prepare("SELECT COUNT(*) AS n FROM accounts").first<{ n: number }>(),
  ]);
  return json({ visibleDriversLastHour: drivers?.n ?? 0, activeReports: reports?.n ?? 0, accounts: accounts?.n ?? 0 });
}

async function cleanup(env: Env) {
  const now = Date.now();
  await env.DB.batch([
    env.DB.prepare("DELETE FROM votes WHERE report_id IN (SELECT id FROM reports WHERE permanent = 0 AND expires_at < ?)").bind(now),
    env.DB.prepare("DELETE FROM reports WHERE permanent = 0 AND expires_at < ?").bind(now),
    env.DB.prepare("DELETE FROM presence WHERE at < ?").bind(now - 30 * MIN),
    env.DB.prepare("DELETE FROM waves WHERE at < ?").bind(now - DAY),
    env.DB.prepare("DELETE FROM points_log WHERE at < ?").bind(now - 60 * DAY),
  ]);
}

export default {
  async fetch(req: Request, env: Env): Promise<Response> {
    if (req.method === "OPTIONS") return new Response(null, { headers: CORS });
    const url = new URL(req.url);
    const path = url.pathname.replace(/\/+$/, "");
    try {
      if (path === "" || path === "/v1") return json({ service: "ROADGUARD communauté", version: 1, types: TYPES });
      const lic = await licenseRoutes(req, env, path);
      if (lic) return lic;
      if (req.method === "POST" && path === "/v1/account") return createAccount(req, env);
      const me = await auth(req, env);
      if (req.method === "GET" && path === "/v1/reports") return reportsNear(url, env, me);
      if (req.method === "GET" && path === "/v1/leaderboard") return leaderboard(url, env, me);
      if (req.method === "POST" && path === "/v1/sync") return sync(req, env, me);
      if (req.method === "GET" && path === "/v1/stats") return stats(env);
      if (req.method === "GET" && path === "/v1/apps") {
        const m = env.APPS ? await env.APPS.get("manifest") : null;
        return new Response(m ?? "{}", { headers: { "content-type": "application/json", "cache-control": "max-age=300", ...CORS } });
      }
      // Lien direct pour installer Pilote Suite la première fois (à partager, ou en QR code).
      if (req.method === "GET" && (path === "/suite" || path === "/v1/apps/suite.apk") && env.APPS) {
        const m = JSON.parse((await env.APPS.get("manifest")) ?? "{}") as Record<string, { build: number; chunks: number }>;
        const info = m.suite;
        if (!info) return fail(404, "Pilote Suite pas encore publiée");
        const parts: ArrayBuffer[] = [];
        for (let i = 0; i < info.chunks; i++) {
          const part = await env.APPS.get(`suite:${info.build}:${i}`, "arrayBuffer");
          if (!part) return fail(404, "Morceau introuvable");
          parts.push(part);
        }
        return new Response(new Blob(parts), { headers: {
          "content-type": "application/vnd.android.package-archive",
          "content-disposition": `attachment; filename="Pilote-Suite-${info.build}.apk"`, ...CORS } });
      }
      const chunk = /^\/v1\/apps\/(gps|drive|suite|drivesys)\/(\d+)\/(\d{1,2})$/.exec(path);
      if (req.method === "GET" && chunk && env.APPS) {
        const data = await env.APPS.get(`${chunk[1]}:${chunk[2]}:${chunk[3]}`, "arrayBuffer");
        if (!data) return fail(404, "Morceau introuvable");
        return new Response(data, { headers: { "content-type": "application/octet-stream", "cache-control": "max-age=86400", ...CORS } });
      }
      if (!me) return fail(401, "Compte requis");
      if (req.method === "GET" && path === "/v1/account") return json({ id: me.id, name: me.name, points: me.points });
      if ((req.method === "PATCH" || req.method === "POST") && path === "/v1/account/name") {
        const name = cleanName(((await req.json().catch(() => ({}))) as { name?: unknown }).name);
        if (!name) return fail(400, "Pseudo de 2 à 24 caractères");
        await env.DB.prepare("UPDATE accounts SET name = ? WHERE id = ?").bind(name, me.id).run();
        return json({ id: me.id, name, points: me.points });
      }
      if (req.method === "POST" && path === "/v1/reports") return createReport(req, env, me);
      const m = /^\/v1\/reports\/([0-9a-f]{20})\/vote$/.exec(path);
      if (req.method === "POST" && m) {
        const body = (await req.json().catch(() => ({}))) as { still?: unknown };
        if (typeof body.still !== "boolean") return fail(400, "still requis");
        return vote(env, me, m[1], body.still);
      }
      if (req.method === "POST" && path === "/v1/presence") return presence(req, env, me);
      if (req.method === "GET" && path === "/v1/drivers") return drivers(url, env, me);
      if (req.method === "POST" && path === "/v1/wave") return wave(req, env, me);
      if (req.method === "GET" && path === "/v1/inbox") return inbox(env, me);
      return fail(404, "Inconnu");
    } catch (e) {
      return fail(500, "Erreur serveur");
    }
  },
  async scheduled(_event: ScheduledController, env: Env): Promise<void> {
    await cleanup(env);
  },
};
