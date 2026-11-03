#!/usr/bin/env bash
# ROADGUARD 11.13.34 — single entry point for building a real France pack.
# Usage: bash tools/produire_france_et_catalogue.sh SOURCE.pmtiles https://YOUR-HOST/france.pmtiles [OUTPUT_DIR]
set -euo pipefail
if [[ $# -lt 2 || $# -gt 3 ]]; then
  echo 'Usage: produire_france_et_catalogue.sh PLANET_SOURCE.pmtiles PUBLIC_HTTPS_URL [OUTPUT_DIR]' >&2
  exit 2
fi
SOURCE="$1"; PUBLIC_URL="$2"; OUT="${3:-output/france}"
case "$PUBLIC_URL" in https://*.pmtiles) ;; *) echo 'Expected a public HTTPS .pmtiles URL' >&2; exit 2;; esac
command -v pmtiles >/dev/null || { echo 'Missing official pmtiles CLI' >&2; exit 3; }
command -v python3 >/dev/null || { echo 'Missing Python 3' >&2; exit 3; }
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
mkdir -p "$OUT"
DEST="$OUT/france_metropolitaine_corse.pmtiles"
# Extraction script writes to a temporary file and verifies before promoting.
bash "$ROOT/tools/prepare_france_pmtiles.sh" "$SOURCE" "$DEST"
python3 "$ROOT/tools/build_france_pmtiles_catalog.py" "$DEST" --https-url "$PUBLIC_URL" --out "$OUT/catalog.json"
cat > "$OUT/ETAT-LIVRAISON.txt" <<'NOTE'
Pack generated locally, NOT YET HOSTED and NOT YET VALIDATED ON ANDROID.
NEXT: upload france_metropolitaine_corse.pmtiles to the exact public HTTPS URL
in catalog.json, publish catalog.json, check Range HTTP 206 and CORS,
then compile ROADGUARD and validate in airplane mode on a physical Xiaomi.
Never declare offline France ready until those tests succeed.
NOTE
printf '\nPack: %s\nCatalogue: %s\n' "$DEST" "$OUT/catalog.json"
