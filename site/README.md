# ROADGUARD community

French mobile-first community website. Worker/Vinext + Sites-managed D1; public map and inventory; authenticated community writes through Sites ChatGPT sign-in. No fake community users or reports.

Development and publication follow the Sites plugin workflow. Keep the `sites()` Vite integration and `.openai/hosting.json` project identity. D1 migrations are schema-only under `drizzle`.

Checks:
```
pnpm exec tsc --noEmit
node --experimental-strip-types tests/api.test.mjs
node --experimental-strip-types tests/inventory.test.mjs
```

See `docs/API.md`, `docs/CAPACITY.md`, `docs/VERIFICATION.md`. Android 11.14.176 source implements account association and community sync. Real Xiaomi/account approval and 500,000 simultaneous capacity remain unvalidated. See load-tests for the isolated staging profile.
