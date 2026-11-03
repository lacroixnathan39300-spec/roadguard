declare namespace Cloudflare {
  interface Env {
    DB?: D1Database;
    PUBLIC_MAP_TILE_URL?: string;
    PUBLIC_MAP_ATTRIBUTION?: string;
    PHONE_HASH_SECRET?: string;
    ROADGUARD_ADMIN_USER_ID?: string;
    ROADGUARD_ADMIN_BOOTSTRAP_PHONE?: string;
    ROADGUARD_ADMIN_BOOTSTRAP_SALT?: string;
    ROADGUARD_ADMIN_BOOTSTRAP_VERIFIER?: string;
    BUCKET?: R2Bucket;
  }
}
