# Capacity target and actual evidence

The confirmed product target is **500,000 simultaneous people** (user choice, 17 October 2026). No claim of 500,000 registered, daily active or simultaneous users has been validated. This deployment uses one Sites-managed D1 database and a Worker. Native Sites hosting quotas are not exposed by the connector.

Implemented: static immutable 1.67 MB inventory asset; hashed integrity; a reusable local cache; bounded indexed geographic queries; 200-record cursor pages; compact private-presence projections and lower-zoom aggregation; no continuous global location stream; ETag revalidation; per-account conditional SQL write limits; explicit opt-in; position expiry and rotation; visible-only polling with randomized jitter; abuse quarantine.

For 500,000 simultaneous active visitors, nominal report polling alone means 500000/120 ≈ 4167 requests/second. Presence updates at 10 minutes add up to 833 writes/second if everyone opts in. This arithmetic is a planning scenario, not a benchmark. One shared D1 database and public OSM infrastructure must not be assumed to sustain it.

Before broad rollout: agree registered vs concurrent target and peak geography; confirm Sites/Worker/D1 quotas and budget; use a tile/search provider licensed and sized for the audience; measure p50/p95/p99 latency, error rate, rows read/written and queueing under gradually increasing realistic read/write mixes; define pass/fail thresholds and rollback; exercise account abuse protection, moderation and retention; load test regional partitions before introducing more D1 bindings. Global replication and regional sharding require a supported hosting configuration and measurements, not a UI counter.

Sources checked 17 October 2026:
- https://developers.cloudflare.com/d1/platform/limits/
- https://developers.cloudflare.com/d1/reference/faq/
- https://developers.cloudflare.com/d1/platform/pricing/
- https://developers.cloudflare.com/workers/platform/limits/
- https://operations.osmfoundation.org/policies/tiles/
- https://operations.osmfoundation.org/policies/nominatim/

No unrequested load test was run against the production site or OSM services. No fictitious members or live reports are seeded. Local test fixtures are in an in-memory SQLite database only.

## This release

Added a database-wide geocoder limiter/cache, configurable HTTPS tile provider, native device authorization and cancellation of obsolete presence leases. Removed the full MIN/MAX scan and the global public-id sort from capped presence reads. Reports now paginate by an indexed spatial tuple rather than sorting a dense region by random UUID. See docs/performance and load-tests for reproducible evidence and a prepared staging load profile.

Local presence median improved from 302.157 ms to 0.509 ms on the deterministic in-memory fixture; revision bounds from 10.766 ms to 0.004 ms. These are single-machine SQL observations, not Workers/D1 capacity. Dense cluster aggregation, authorization reads and the single-writer database remain bottlenecks.

A dedicated backend with spatial partitions, replicated reads/cache, distributed authorization/revocation, maintained expiring aggregates, durable write queues and contracted tiles/search must be provisioned and tested for the confirmed target. Render discovery was offered as a possible infrastructure connection; it is not evidence of a connected account, chosen architecture or acquired capacity. No paid resources or production load traffic were created.
