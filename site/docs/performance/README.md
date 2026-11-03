# ROADGUARD — local capacity query experiment

Run with Node 24 (node:sqlite), with no dependency installation:

```sh
node /workspace/scratch/2394ab194734/verification/capacity176/benchmark.mjs /workspace/sites/roadguard-community /workspace/scratch/2394ab194734/verification/capacity176/results.json
```

The script reads the checkout's SQL migrations and creates all synthetic data in memory. It never sends HTTP requests, changes the checkout, or touches production. It writes only its specified JSON result. The deterministic fixture distribution matches the initial exploratory run; runtimes vary with local machine load. Positions in this fixture are unrounded floating-point values; real service values are rounded, so this fixture does not establish worst-case behavior under many identical positions.

Five warm sequential SQL measurements reveal query costs and query plans. They do not validate Cloudflare D1 latency, concurrency, durable write performance, hosting quotas, network performance, or 500,000 concurrent users. No p95/p99 conclusion can be drawn from five samples.

## Improvements without schema changes

For the existing capped presence response, remove `ORDER BY public_id`. Alternatively use `ORDER BY lat,lng,expires`, matching the existing `presence_area` index. This avoids sorting the full matching region before returning 201 rows. These options produce a spatially biased subset when truncated; the interface must disclose that the region is truncated. They do not provide complete member pagination or bound the work of sparse or expired regions. The cluster `GROUP BY` cost remains.

Use independent subqueries for `MIN(revision)` and `MAX(revision)` to avoid the full scan caused by combining the extrema in one aggregate query.

## Optional small index migration for stable page order

An additional `(lat,lng,expires,public_id)` index permits keyset pagination with the same tuple in `ORDER BY` and a tuple `>` comparison. The benchmark creates this index only in its disposable in-memory database and checks page overlap. This is an additive index migration, not a row or data-format migration; building it on a real large database still requires its own staged rollout and measurement. Public cursors must be limited to the current area/session and positions can move between pages: exact frozen snapshots are not supplied by this scheme. Use the rotating public ID, never the private user ID or stable rowid in public cursors.

Dense exact counts should use measured, maintained spatial aggregates or region partitions for a large rollout; adding LIMIT to the output does not bound the aggregation work.
