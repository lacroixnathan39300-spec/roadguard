# Isolated load validation — target 500,000 simultaneous people

The user explicitly selected **500,000 simultaneous connections** on 17 October 2026. This is not a registered-account target.

`sqlite-benchmark.mjs` creates synthetic rows in memory only. Its results reveal query plans; it cannot validate production capacity. `docs/performance/sqlite-500000.json` is the captured local run (500k presence, 100k reports, 100k events). Five measurements do not establish p95/p99.

`api-concurrency.js` is a k6 staging profile, prepared but NOT executed. It refuses the production ROADGUARD origin and external tile/search providers. Defaults target 500,000 virtual users and ramp gradually over two hours. Even a failed ramp can incur significant generator/hosting costs: configure an agreed budget, quotas, stop conditions, isolated databases, monitoring and distributed generators before execution. Use real hardware measurements to size generators; do not assume one computer can run 500,000 JS contexts.

ACTORS_FILE must be a private file of **synthetic staging grants**, never real customer credentials. Each record needs token, lease (if sharing), lat, lng and bbox. Grants/leases must be provisioned only in the dedicated staging database. Match the production authorization path, indexes, TTLs, write mix and geographic distributions. Keep actors outside Git and exports.

This profile exercises one report page per 120 seconds and optional presence every 600 seconds. It does not include all pagination, account sign-in bursts, report writes/votes, static assets, cartographic bandwidth, geocoding, device enrollment or resilience failovers. Add those independent accepted scenarios before declaring full product capacity. Preserve hash integrity and bounded caches while testing. Never load-test OSM/Nominatim.

Acceptance thresholds in the script are **proposed gates**, not measured results or a service guarantee. Record request rate, error classes, p50/p95/p99, DB rows/work, write queue time, cache hit rate, CPU, memory, bytes/second and token-revocation/visibility behavior during failures. Roll back on a gate violation.

The current Sites-managed single D1 database remains a pilot. Native Sites controls exposed to this session do not provide D1 sharding, replicas, dedicated load generators or a paid tile-provider account. Deploying the website does not certify the concurrency target.
