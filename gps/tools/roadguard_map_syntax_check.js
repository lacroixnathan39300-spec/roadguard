// Parse each inline JavaScript block from the shipped map assets without executing it.
const fs = require('fs');
const vm = require('vm');
for (const file of ['app/src/main/assets/map.html','app/src/main/assets/map_maplibre.html']) {
  const html = fs.readFileSync(file, 'utf8');
  const scripts = [...html.matchAll(/<script(?![^>]*\bsrc=)[^>]*>([\s\S]*?)<\/script>/gi)];
  if (!scripts.length) throw new Error(`No inline scripts found in ${file}`);
  scripts.forEach((match, index) => new vm.Script(match[1], { filename: `${file}#script-${index + 1}` }));
  process.stdout.write(`${file}: ${scripts.length} inline script(s) parse OK\n`);
}
