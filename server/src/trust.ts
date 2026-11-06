/**
 * Confiance des signalements (règles pures, testées).
 *
 * - Note de fiabilité en étoiles (1 à 5) d'après les signalements confirmés (« toujours là ») ou démentis tôt.
 * - Niveaux : admin / confiance, fiable (4 étoiles et plus), nouveau (pas encore noté), peu fiable, suspect, banni.
 * - Mode « lancement » : tant que la région compte peu de conducteurs, un nouveau est traité normalement.
 */

export type Level = "trusted" | "reliable" | "normal" | "new" | "low" | "suspect" | "banned";

export interface TrustAccount { good: number; bad: number; trusted: number; banned: number; suspect_until: number }

export const TRUST = {
  /** Nombre de signalements jugés avant d'avoir une note. */
  minJudged: 5,
  reliableStars: 4,
  reliableMinJudged: 10,
  lowStars: 2.5,
  /** Région « en lancement » : moins de 20 conducteurs actifs dans le mois à moins de 50 km. */
  launchDrivers: 20,
  launchRadiusM: 50_000,
  /** Nouveau, hors lancement : radar fixe retiré au bout de 7 jours sans confirmation, danger au bout de 2 jours. */
  pendingLife: { radar_fixe: 7 * 86_400_000, danger: 2 * 86_400_000 } as Record<string, number>,
  /** Anti-abus. */
  maxPerHour: 10,
  maxSpeedKmh: 250,
  suspectFor: 24 * 3_600_000,
  /** Un « plus là » compte comme une erreur de l'auteur s'il arrive vite après le signalement. */
  earlyNo: { police: 30 * 60_000, radar_mobile: 30 * 60_000, accident: 30 * 60_000, bouchon: 20 * 60_000 } as Record<string, number>,
  earlyNoDefault: 24 * 3_600_000,
};

/** Note sur 5 (lissée : un débutant n'a pas 5 étoiles pour un seul signalement juste), ou null si pas encore noté. */
export function stars(good: number, bad: number): number | null {
  if (good + bad < TRUST.minJudged) return null;
  const ratio = (good + 1) / (good + bad + 2);
  return Math.round((1 + 4 * ratio) * 10) / 10;
}

export function level(a: TrustAccount, now: number): Level {
  if (a.banned) return "banned";
  if (a.trusted) return "trusted";
  if (a.suspect_until > now) return "suspect";
  const s = stars(a.good, a.bad);
  if (s == null) return "new";
  if (s < TRUST.lowStars) return "low";
  if (s >= TRUST.reliableStars && a.good + a.bad >= TRUST.reliableMinJudged) return "reliable";
  return "normal";
}

export interface Placement { status: "ok" | "pending" | "hidden"; lifeFactor: number; permanent: boolean; pendingLife: number | null }

/**
 * Comment un nouveau signalement est publié selon le niveau de l'auteur.
 * [known] : radar fixe à moins de 100 m d'un radar déjà connu des bases (validé d'office).
 */
export function placement(lv: Level, type: string, canBePermanent: boolean, launch: boolean, known: boolean): Placement {
  if (lv === "suspect" || lv === "low") return { status: "hidden", lifeFactor: 1, permanent: false, pendingLife: null };
  if (lv === "trusted") return { status: "ok", lifeFactor: 2, permanent: canBePermanent, pendingLife: null };
  if (known && type === "radar_fixe") return { status: "ok", lifeFactor: 1, permanent: true, pendingLife: null };
  if (lv === "reliable") return { status: "ok", lifeFactor: 2, permanent: false, pendingLife: null };
  if (lv === "new" && !launch && TRUST.pendingLife[type]) return { status: "pending", lifeFactor: 1, permanent: false, pendingLife: TRUST.pendingLife[type] };
  return { status: "ok", lifeFactor: 1, permanent: false, pendingLife: null };
}

/** Comportement suspect : trop de signalements, ou deux signalements impossibles à faire en roulant. */
export function suspicious(lastHour: number, last: { lat: number; lon: number; at: number } | null,
  lat: number, lon: number, now: number, distanceM: (a: number, b: number, c: number, d: number) => number): string | null {
  if (lastHour >= TRUST.maxPerHour) return `plus de ${TRUST.maxPerHour} signalements en une heure`;
  if (last && last.at > 0) {
    const d = distanceM(last.lat, last.lon, lat, lon);
    const h = Math.max(now - last.at, 1) / 3_600_000;
    if (d > 20_000 && d / 1000 / h > TRUST.maxSpeedKmh) return `${Math.round(d / 1000)} km entre deux signalements en ${Math.round(h * 60)} min`;
  }
  return null;
}

/** Un « plus là » est-il un démenti (erreur de l'auteur) ou une simple fin naturelle ? */
export function isEarlyNo(type: string, createdAt: number, now: number, yes: number): boolean {
  if (yes > 0) return false;
  return now - createdAt <= (TRUST.earlyNo[type] ?? TRUST.earlyNoDefault);
}
