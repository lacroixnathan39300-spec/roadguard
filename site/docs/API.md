# ROADGUARD Web 1.0 — API v1

Origin: https://roadguard-community.lacroixnathan39300.chatgpt.site

Public reading needs no secret once the Site is public. Member identity comes exclusively from Sites sign-in. Never embed a bypass token, account cookie or repository credential in an APK. CORS permits public GET reads only. A native app may use GET directly; writes currently use the signed-in website. Native account authorization is a separate integration.

All coordinates use WGS84 decimal degrees. `bbox=west,south,east,north`, maximum 2 degrees each axis; longitude wrapping is not supported. Errors use JSON `{error}` and appropriate HTTP status. Timestamps are milliseconds since epoch. Validate HTTP status before using a body.

| Endpoint | Behavior |
| --- | --- |
| GET `/data/manifest.json` | Version, count, SHA256 and immutable data URL for the permanent radar inventory. Check at launch. |
| GET `/data/radars-<hash>.json` | Full permanent inventory, verified against manifest SHA256 before atomic cache replacement. Source attribution is `/data/sources.txt`; derived database ODbL 1.0. |
| GET `/api/v1/health` | DB probe and intervals. `capacityValidated:false` is intentional. |
| GET `/api/v1/reports?bbox=...&cursor=...` | Active community reports. 200 records maximum per page; follow `nextCursor` until null. ETag/If-None-Match supported. Revalidate every 120 seconds while visible. |
| GET `/api/v1/snapshot?bbox=...` | Same bounded, paginated report snapshot. Can be downloaded before opening a native map. |
| GET `/api/v1/sync?bbox=...&since=N` | Monotonic changes with tombstones, 200 per page. Follow `since` while `hasMore`. A 409 requires a fresh snapshot. |
| GET `/api/v1/presence?bbox=...&zoom=...` | Active approximate positions, or clusters below zoom 14. At most 200 features. Never an email or account ID. Do not persist this response. |
| GET `/api/v1/session` | Private account state and preferences. No-store. |
| POST `/api/v1/reports` | Signed in; JSON kind, note <=180 chars, lat, lng, accuracy <=100 m. Required UUID `Idempotency-Key`; reuse after uncertain network failure. 5 writes per user per 5 minutes. |
| POST `/api/v1/reports/:id/vote` | Signed in; value seen, gone or flag. One vote per account/report. 30-second cooldown. Author cannot self-confirm. Three distinct abuse flags quarantine the report and emit a tombstone. |
| DELETE `/api/v1/reports/:id` | Author only. Removes from public map, blanks current comment, emits a tombstone. |
| GET `/api/v1/me/reports` | Author's 100 latest contributions, including expired/removed/quarantined ones. |
| PUT `/api/v1/presence` | Signed in; explicit `consent:true`, lat, lng, accuracy <=1000 m. Position rounded to a .003 degree grid. Ten-minute server write interval. Pseudonym rotates at update. No position history. |
| DELETE `/api/v1/presence` | Signed in; removes last published position immediately. |
| PUT `/api/v1/preferences` | Signed in; validated palette, theme, cursor, routeColor, voice and switches. |

Community fixed-camera and red-light reports expire after 24 h; other reports after 2 h. They remain unverified reports and do not modify the permanent inventory. Always discard expired records locally, including offline. Inventories are versioned historical source datasets, not live camera validation.

## Launch protocol for a native client

1. Keep the existing local inventory usable. Fetch the manifest with a bounded timeout.
2. Only fetch an inventory asset when the version or SHA changes; verify the complete bytes before replacing the cache. Never erase a working cache after a failed download.
3. Obtain a current permitted GPS position or last selected map area. Download all report snapshot pages for that area with bounded timeouts and store atomically. Never claim a complete snapshot when a page is missing.
4. Open the map after successful preparation or an explicit, clearly signaled cached fallback. Revalidate visible nearby reports at 120 s, add jitter and pause hidden/background pages. Query each newly visited area separately; do not apply a delta token to another bbox.
5. Presence requires a separate opt-in and account authorization. Update only every 600 s. Local positions must not be sent on a map visit by default. Never cache other people's locations. The server excludes positions after 1200 s.

Delta retention housekeeping is bounded and triggered by writes; old deltas may remain longer in an inactive database. Expiry is always checked on reads. Snapshot bootstrapping should start from `since=0` and replay bounded deltas or use a full paginated snapshot; an initial snapshot's timing is not a global transactional watermark. This initial web client uses snapshots rather than assuming such a watermark.

Android 11.14.176 implements the API integration described below. Its new native account flow still requires real-device approval/testing; publishing source is not proof of an executed Xiaomi session. Existing private local reports are never uploaded automatically.

## Android 11.14.176 account association

The website continues to use Sites-owned ChatGPT sign-in. Android receives a device-specific capability only after explicit approval through that signed-in website; no password, platform bypass token or browser cookie is embedded in the APK.

1. POST `/api/v1/devices/enroll` with a name <=60 chars returns a random `rg_` credential, pending id, comparison code and approvalPath. The server stores SHA256 only. Pending enrollment lasts ten minutes. The client stores the credential encrypted with non-exportable Android Keystore AES-GCM. Never put this token in a URL or log.
2. GET `/api/v1/devices/:id` shows the pending device name/code. The user signs in through the normal top-level Sites route and explicitly POSTs `/:id/approve` with consent and the matching comparison code. A pending bearer cannot approve itself. Maximum five active devices per user.
3. Android polls `/api/v1/devices/session` while foreground with its bearer. Only an approved, unexpired token confers the corresponding account identity. It expires after thirty days.
4. Approved native requests can use reports, votes, own history, preferences and opt-in presence. They cannot approve new devices. The website can list/revoke devices; a native bearer can revoke itself, including cancelling a pending enrollment. Revocation is checked on subsequent writes/reads without an authorization cache.

### Shared visibility cancellation

An explicit activation sends `start:true,consent:true` and receives a lease. Renewals require `consent:true,lease:<current lease>` and cannot implicitly start a new sharing session. DELETE `/presence` atomically deletes both the lease and last position. A stale app cannot republish using the cancelled lease. A deliberate activation from another device replaces the lease; the old app must stop renewing. The private `/session` response exposes the current lease to its authenticated owner only. Public map queries never expose it.

### Providers and retention

GET `/config` supplies a validated HTTPS XYZ template and public attribution. Operators can set PUBLIC_MAP_TILE_URL and PUBLIC_MAP_ATTRIBUTION to a contracted provider; these are public client values, never server secrets. Both website and Android read this configuration. The existing custom Android HTTP tile cache handles OSM PNGs; other providers use WebView caching according to their own response headers.

GET `/places?q=...` caches normalized place queries for 30 days in shared D1 and arbitrates upstream Nominatim calls using one atomic global DB row, spaced at least 1.2 seconds. It is a pilot geocoder, not a 500k-concurrent geocoding service. Cached queries are not associated with accounts. A contracted/self-hosted provider is required for the large rollout.

Expired reports/votes are removed in bounded batches after 30 days, old change events after seven days, and expired approved devices in small batches on writes. Housekeeping is opportunistic: inactive databases may retain rows longer, but expired records are excluded from public reads immediately. The first web client uses whole bounded-area report snapshots, not a frozen multi-page global transaction. Report cursors now encode an opaque spatial tuple; do not interpret or reuse them for another area.

## Connexion native ROADGUARD (11.14.274)

L'application utilise `POST /api/v1/native-auth/register` et `POST /api/v1/native-auth/login` sur l'origine HTTPS du site. Corps JSON : `phone` au format E.164 et `password` (12 à 128 caractères) ; l'inscription ajoute `displayName` (2 à 32 caractères). `deviceName` et `deviceSignal` sont facultatifs. Ces routes ne nécessitent pas de session navigateur et ne reçoivent aucun mot de passe dans une URL. Elles renvoient un jeton `rg_` propre au téléphone, accepté par les routes de session déjà existantes. L'application chiffre ce jeton dans Android Keystore ; elle ne stocke pas le mot de passe.

Le serveur conserve le numéro sous HMAC, le mot de passe sous PBKDF2-SHA256 avec sel aléatoire et limite les tentatives par IP et par numéro. La création refuse un numéro déjà déclaré. Le numéro n'est pas vérifié par SMS ou opérateur ; la récupération automatique du mot de passe n'est pas encore disponible. Les comptes créés auparavant uniquement sur le site ne disposent pas automatiquement d'un mot de passe natif : une migration vérifiée par le propriétaire sera nécessaire, sans contournement de leur identité existante.
