import { DatabaseSync } from 'node:sqlite';
import { readFileSync, readdirSync, writeFileSync } from 'node:fs';
import { resolve } from 'node:path';

// Usage: node benchmark.mjs [site checkout] [output JSON]
// No HTTP requests; all fixtures and indexes exist only in an in-memory database.
const checkout = resolve(process.argv[2] || '/workspace/sites/roadguard-community');
const output = resolve(process.argv[3] || new URL('results.json', import.meta.url).pathname);
const db = new DatabaseSync(':memory:');
const migrations = readdirSync(resolve(checkout, 'drizzle')).filter(x => x.endsWith('.sql')).sort();
for (const f of migrations) db.exec(readFileSync(resolve(checkout, 'drizzle', f), 'utf8'));
let seed = 444;
const random = () => ((seed = (Math.imul(seed, 1664525) + 1013904223) >>> 0) / 4294967296);
const now = 1792219200000;
const p = db.prepare('INSERT INTO presence VALUES(?,?,?,?,?,?,?)');
const r = db.prepare('INSERT INTO reports VALUES(?,?,?,?,?,?,?,?,?,?)');
const e = db.prepare('INSERT INTO events(report_id,lat,lng,payload,created) VALUES(?,?,?,?,?)');
db.exec('BEGIN');
for (let i = 0; i < 500000; i++) {
  const dense = i < 250000;
  const lat = dense ? 46.7 + random() * .1 : 40 + random() * 12;
  const lng = dense ? 5.85 + random() * .1 : -5 + random() * 15;
  p.run('u' + i, 'p' + i, 'Conducteur', lat, lng, now, now + 1200000);
  if (i < 100000) {
    r.run('r' + i, 'u' + i, 'danger', lat, lng, 'Fixture', now, now, now + 7200000, 'active');
    e.run('r' + i, lat, lng, '{}', now);
  }
}
db.exec('COMMIT');
const select = 'SELECT public_id id,alias,lat,lng,updated,expires FROM presence';
const where = ' WHERE lat BETWEEN ? AND ? AND lng BETWEEN ? AND ? AND expires>?';
const dense = [46.6,46.9,5.8,6,now];
const sparse = [47,47.1,3,3.1,now];
const queries = {
  presenceDense: [select + where + ' ORDER BY public_id LIMIT 201', dense],
  presenceSparse: [select + where + ' ORDER BY public_id LIMIT 201', sparse],
  clusterDense: ['SELECT round(lat/?) * ? lat,round(lng/?) * ? lng,count(*) count,min(expires) expires FROM presence WHERE lat BETWEEN ? AND ? AND lng BETWEEN ? AND ? AND expires>? GROUP BY round(lat/?),round(lng/?) LIMIT 201',[.03,.03,.03,.03,...dense,.03,.03]],
  reportsDense: ["SELECT r.id,r.kind,r.lat,r.lng,r.note,r.created,r.updated,r.expires,r.status,(SELECT count(*) FROM votes v WHERE v.report_id=r.id AND v.value='seen') confirmations,(SELECT count(*) FROM votes v WHERE v.report_id=r.id AND v.value='gone') cleared FROM reports r WHERE r.lat BETWEEN ? AND ? AND r.lng BETWEEN ? AND ? AND r.status='active' AND r.expires>? AND r.id>? ORDER BY r.id LIMIT 201", [...dense,'']],
  revisionBounds: ['SELECT MIN(revision) min,MAX(revision) max FROM events', []],
  syncCatchup: ['SELECT revision,payload FROM events WHERE revision>? AND revision<=? AND lat BETWEEN ? AND ? AND lng BETWEEN ? AND ? ORDER BY revision LIMIT 201',[0,100000,47,47.1,3,3.1]],
  presenceDenseNoSort: [select + where + ' LIMIT 201', dense],
  presenceDenseIndexOrder: [select + where + ' ORDER BY lat,lng,expires LIMIT 201', dense],
  presenceSparseIndexOrder: [select + where + ' ORDER BY lat,lng,expires LIMIT 201', sparse],
  revisionBoundsIndependent: ['SELECT (SELECT MIN(revision) FROM events) min,(SELECT MAX(revision) FROM events) max', []],
};
function measure(q, args) {
  const statement = db.prepare(q);
  statement.all(...args);
  const samples = [];
  let returned;
  for (let i = 0; i < 5; i++) {
    const start = performance.now();
    returned = statement.all(...args).length;
    samples.push(performance.now() - start);
  }
  samples.sort((a,b) => a-b);
  return {sql:q,params:args,returned,medianMs:+samples[2].toFixed(3),maxMs:+samples[4].toFixed(3),samplesMs:samples.map(x=>+x.toFixed(3)),plan:db.prepare('EXPLAIN QUERY PLAN ' + q).all(...args).map(x=>x.detail)};
}
const out = {
  context: 'Local in-memory SQLite only; not D1 throughput, network, concurrent users or production load. Five warmed sequential samples are insufficient for p95/p99 inference.',
  node: process.version,
  sqlite: db.prepare('SELECT sqlite_version() version').get().version,
  migrationFiles: migrations,
  fixtures:{seed:444,now,presence:500000,reports:100000,events:100000,densePresence:250000,coordinates:'Unrounded deterministic floating-point fixture, same distribution as initial exploratory test; real service rounds positions to .003 degrees.'},
  queries: Object.fromEntries(Object.entries(queries).map(([name,[q,args]])=>[name,measure(q,args)])),
};
// Optional additional index exists only in this disposable database, never the site.
db.exec('CREATE INDEX benchmark_presence_page ON presence(lat,lng,expires,public_id)');
out.optionalIndex = {sql:'CREATE INDEX presence_page ON presence(lat,lng,expires,public_id)',firstPage:measure(select + where + ' ORDER BY lat,lng,expires,public_id LIMIT 201', dense)};
const first = db.prepare(select + where + ' ORDER BY lat,lng,expires,public_id LIMIT 200').all(...dense);
const last = first.at(-1);
out.optionalIndex.nextPage = measure(select + where + ' AND (lat,lng,expires,public_id)>(?,?,?,?) ORDER BY lat,lng,expires,public_id LIMIT 201', [...dense,last.lat,last.lng,last.expires,last.id]);
const firstIds = new Set(first.map(x=>x.id));
out.optionalIndex.pageOverlap = db.prepare(select + where + ' AND (lat,lng,expires,public_id)>(?,?,?,?) ORDER BY lat,lng,expires,public_id LIMIT 201').all(...dense,last.lat,last.lng,last.expires,last.id).filter(x=>firstIds.has(x.id)).length;
writeFileSync(output, JSON.stringify(out,null,2)+'\n');
console.log(JSON.stringify(out,null,2));
db.close();
