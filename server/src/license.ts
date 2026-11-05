/**
 * Comptes ROADGUARD : identifiant + mot de passe, essai gratuit d'un mois (un seul par appareil),
 * puis accès accordé à la main par l'administrateur (illimité, 1 mois, 1 an) ou appli bloquée.
 * Le tout premier compte créé devient administrateur.
 */
const DAY = 86_400_000;
export const TRIAL_MS = 30 * DAY;
/** Appareils connectés en même temps à un compte (téléphone + autoradio + 1 de secours). */
export const MAX_DEVICES = 3;

export interface LicEnv { DB: D1Database }

export interface User {
  username: string; pass_hash: string; salt: string; created_at: number;
  access_until: number; plan: string; blocked: number; admin: number; last_seen: number;
}

export function cleanUsername(raw: unknown): string | null {
  if (typeof raw !== "string") return null;
  const u = raw.trim().toLowerCase();
  return /^[a-z0-9._-]{3,24}$/.test(u) ? u : null;
}

export function validPassword(raw: unknown): raw is string {
  return typeof raw === "string" && raw.length >= 6 && raw.length <= 100;
}

export function cleanDevice(raw: unknown): string | null {
  return typeof raw === "string" && /^[0-9a-zA-Z_-]{8,64}$/.test(raw) ? raw : null;
}

/** Accès en cours ? (0 = illimité) */
export function isActive(u: Pick<User, "access_until" | "blocked" | "admin">, now: number): boolean {
  if (u.admin) return true;
  if (u.blocked) return false;
  return u.access_until === 0 || u.access_until > now;
}

/** Nouvelle date de fin selon l'action de l'administrateur. */
export function grant(u: Pick<User, "access_until">, action: string, now: number): { access_until: number; plan: string } | null {
  const from = Math.max(now, u.access_until === 0 ? now : u.access_until);
  switch (action) {
    case "unlimited": return { access_until: 0, plan: "unlimited" };
    case "month": return { access_until: from + 31 * DAY, plan: "month" };
    case "year": return { access_until: from + 365 * DAY, plan: "year" };
    case "trial": return { access_until: now + TRIAL_MS, plan: "trial" };
    case "expire": return { access_until: now - 1, plan: "expired" };
    default: return null;
  }
}

function hex(buf: ArrayBuffer | Uint8Array): string {
  return [...new Uint8Array(buf)].map((b) => b.toString(16).padStart(2, "0")).join("");
}
function random(bytes: number): string {
  const a = new Uint8Array(bytes); crypto.getRandomValues(a); return hex(a);
}
async function sha256(text: string): Promise<string> {
  return hex(await crypto.subtle.digest("SHA-256", new TextEncoder().encode(text)));
}
/**
 * Itérations PBKDF2 : l'offre gratuite Cloudflare limite le calcul à ~10 ms par requête ;
 * 100 000 itérations dépassaient la limite (connexion refusée). Le sel reste unique par compte.
 */
export const PBKDF2_ITERATIONS = 4_000;
export async function hashPassword(password: string, salt: string): Promise<string> {
  const key = await crypto.subtle.importKey("raw", new TextEncoder().encode(password), "PBKDF2", false, ["deriveBits"]);
  const bits = await crypto.subtle.deriveBits({ name: "PBKDF2", hash: "SHA-256", salt: new TextEncoder().encode(salt), iterations: PBKDF2_ITERATIONS }, key, 256);
  return hex(bits);
}

function json(data: unknown, status = 200): Response {
  return new Response(JSON.stringify(data), { status, headers: { "content-type": "application/json", "access-control-allow-origin": "*" } });
}
function fail(status: number, error: string): Response { return json({ error }, status); }

export function status(u: User, now: number) {
  return {
    username: u.username, admin: !!u.admin, plan: u.admin ? "admin" : u.blocked ? "blocked" : u.plan,
    accessUntil: u.access_until, active: isActive(u, now), blocked: !!u.blocked, serverTime: now,
  };
}

async function openSession(env: LicEnv, u: User, device: string, app: string, now: number): Promise<Response> {
  const token = random(32);
  // Même appareil + même appli : l'ancienne connexion est remplacée.
  await env.DB.prepare("DELETE FROM sessions WHERE username = ? AND device = ? AND app = ?").bind(u.username, device, app).run();
  const devices = await env.DB.prepare("SELECT DISTINCT device FROM sessions WHERE username = ?").bind(u.username).all<{ device: string }>();
  const others = devices.results.map((r) => r.device).filter((d) => d !== device);
  if (!u.admin && others.length >= MAX_DEVICES) return fail(403, `Compte déjà utilisé sur ${MAX_DEVICES} appareils. Demande à l'administrateur de les déconnecter.`);
  await env.DB.prepare("INSERT INTO sessions (token_hash, username, device, app, created_at, last_seen) VALUES (?, ?, ?, ?, ?, ?)")
    .bind(await sha256(token), u.username, device, app, now, now).run();
  return json({ token, ...status(u, now) });
}

async function sessionUser(req: Request, env: LicEnv): Promise<User | null> {
  // En-tête, ou « ?session= » (les requêtes relayées par le téléphone n'ont pas d'en-têtes personnalisés).
  const t = req.headers.get("x-rg-session") ?? new URL(req.url).searchParams.get("session") ?? "";
  if (!/^[0-9a-f]{64}$/.test(t)) return null;
  const h = await sha256(t);
  const u = await env.DB.prepare("SELECT u.* FROM sessions s JOIN users u ON u.username = s.username WHERE s.token_hash = ?").bind(h).first<User>();
  if (!u) return null;
  const now = Date.now();
  if (now - u.last_seen > 3_600_000) {
    await env.DB.batch([
      env.DB.prepare("UPDATE users SET last_seen = ? WHERE username = ?").bind(now, u.username),
      env.DB.prepare("UPDATE sessions SET last_seen = ? WHERE token_hash = ?").bind(now, h),
    ]);
  }
  return u;
}

/** Routes /v1/auth/… et /v1/admin/… ; null si la route n'est pas la nôtre. */
export async function licenseRoutes(req: Request, env: LicEnv, path: string): Promise<Response | null> {
  if (!path.startsWith("/v1/auth") && !path.startsWith("/v1/admin")) return null;
  const now = Date.now();
  if (req.method === "POST" && (path === "/v1/auth/register" || path === "/v1/auth/login")) {
    const b = (await req.json().catch(() => ({}))) as Record<string, unknown>;
    const username = cleanUsername(b.username);
    const device = cleanDevice(b.device);
    const app = b.app === "drive" ? "drive" : "gps";
    if (!username) return fail(400, "Identifiant : 3 à 24 caractères (lettres, chiffres, . _ -)");
    const password = b.password;
    if (!validPassword(password)) return fail(400, "Mot de passe : au moins 6 caractères");
    if (!device) return fail(400, "Appareil inconnu");
    if (path === "/v1/auth/register") {
      if (await env.DB.prepare("SELECT 1 FROM users WHERE username = ?").bind(username).first()) return fail(409, "Cet identifiant est déjà pris");
      const first = !(await env.DB.prepare("SELECT 1 FROM users LIMIT 1").first());
      // Un seul essai gratuit par appareil : un 2e compte créé sur le même appareil démarre sans essai.
      const usedTrial = await env.DB.prepare("SELECT username FROM trials WHERE device = ?").bind(device).first<{ username: string }>();
      const salt = random(16);
      const user: User = {
        username, pass_hash: await hashPassword(password, salt), salt, created_at: now,
        access_until: usedTrial ? now - 1 : now + TRIAL_MS, plan: usedTrial ? "expired" : "trial",
        blocked: 0, admin: first ? 1 : 0, last_seen: now,
      };
      const stmts = [env.DB.prepare("INSERT INTO users (username, pass_hash, salt, created_at, access_until, plan, blocked, admin, last_seen) VALUES (?, ?, ?, ?, ?, ?, 0, ?, ?)")
        .bind(user.username, user.pass_hash, user.salt, now, user.access_until, user.plan, user.admin, now)];
      if (!usedTrial) stmts.push(env.DB.prepare("INSERT INTO trials (device, username, at) VALUES (?, ?, ?)").bind(device, username, now));
      await env.DB.batch(stmts);
      return openSession(env, user, device, app, now);
    }
    const u = await env.DB.prepare("SELECT * FROM users WHERE username = ?").bind(username).first<User>();
    if (!u || u.pass_hash !== (await hashPassword(password, u.salt))) return fail(401, "Identifiant ou mot de passe incorrect");
    return openSession(env, u, device, app, now);
  }

  const me = await sessionUser(req, env);
  if (!me) return fail(401, "Connexion requise");
  if (req.method === "GET" && path === "/v1/auth/status") return json(status(me, now));
  if (req.method === "POST" && path === "/v1/auth/logout") {
    const t = req.headers.get("x-rg-session") ?? new URL(req.url).searchParams.get("session") ?? "";
    await env.DB.prepare("DELETE FROM sessions WHERE token_hash = ?").bind(await sha256(t)).run();
    return json({ ok: true });
  }
  if (req.method === "POST" && path === "/v1/auth/password") {
    const b = (await req.json().catch(() => ({}))) as Record<string, unknown>;
    const old = b.old, next = b.password;
    if (!validPassword(old) || me.pass_hash !== (await hashPassword(old, me.salt))) return fail(401, "Ancien mot de passe incorrect");
    if (!validPassword(next)) return fail(400, "Mot de passe : au moins 6 caractères");
    const salt = random(16);
    await env.DB.prepare("UPDATE users SET pass_hash = ?, salt = ? WHERE username = ?").bind(await hashPassword(next, salt), salt, me.username).run();
    return json({ ok: true });
  }

  if (!me.admin) return fail(403, "Réservé à l'administrateur");
  if (req.method === "GET" && path === "/v1/admin/users") {
    const rows = await env.DB.prepare(
      "SELECT u.*, (SELECT COUNT(DISTINCT device) FROM sessions s WHERE s.username = u.username) AS devices FROM users u ORDER BY u.created_at DESC LIMIT 500"
    ).all<User & { devices: number }>();
    return json({ users: rows.results.map((u) => ({ ...status(u, now), createdAt: u.created_at, lastSeen: u.last_seen, devices: u.devices })) });
  }
  const m = /^\/v1\/admin\/users\/([a-z0-9._-]{3,24})$/.exec(path);
  if (req.method === "POST" && m) {
    const u = await env.DB.prepare("SELECT * FROM users WHERE username = ?").bind(m[1]).first<User>();
    if (!u) return fail(404, "Compte introuvable");
    const action = String(((await req.json().catch(() => ({}))) as Record<string, unknown>).action ?? "");
    if (action === "block" || action === "unblock") {
      if (u.admin) return fail(400, "Impossible de bloquer l'administrateur");
      await env.DB.prepare("UPDATE users SET blocked = ? WHERE username = ?").bind(action === "block" ? 1 : 0, u.username).run();
      if (action === "block") await env.DB.prepare("DELETE FROM sessions WHERE username = ?").bind(u.username).run();
    } else if (action === "devices") {
      await env.DB.prepare("DELETE FROM sessions WHERE username = ?").bind(u.username).run();
    } else if (action === "delete") {
      if (u.admin) return fail(400, "Impossible de supprimer l'administrateur");
      await env.DB.batch([
        env.DB.prepare("DELETE FROM sessions WHERE username = ?").bind(u.username),
        env.DB.prepare("DELETE FROM users WHERE username = ?").bind(u.username),
      ]);
      return json({ ok: true });
    } else {
      const g = grant(u, action, now);
      if (!g) return fail(400, "Action inconnue");
      await env.DB.prepare("UPDATE users SET access_until = ?, plan = ?, blocked = 0 WHERE username = ?").bind(g.access_until, g.plan, u.username).run();
    }
    const fresh = await env.DB.prepare("SELECT * FROM users WHERE username = ?").bind(u.username).first<User>();
    return json(status(fresh!, now));
  }
  return fail(404, "Inconnu");
}
