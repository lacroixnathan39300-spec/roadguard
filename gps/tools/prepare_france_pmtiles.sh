#!/usr/bin/env bash
# ROADGUARD 11.13.37 — controlled France + Corsica extraction.
set -euo pipefail
if [[ $# -lt 2 || $# -gt 3 ]]; then echo 'Usage: prepare_france_pmtiles.sh SOURCE.pmtiles OUTPUT.pmtiles [MAXZOOM 0..15]' >&2; exit 2; fi
SOURCE="$1"; DEST="$2"; MAXZOOM="${3:-12}"
case "$SOURCE" in https://*.pmtiles) ;; http://*) echo 'HTTPS source required' >&2; exit 2;; *) [[ -f "$SOURCE" && "$SOURCE" == *.pmtiles ]] || { echo 'Local source must be an existing .pmtiles file' >&2; exit 2; };; esac
[[ "$DEST" == *.pmtiles ]] || { echo 'Output must end in .pmtiles' >&2; exit 2; }
[[ "$MAXZOOM" =~ ^([0-9]|1[0-5])$ ]] || { echo 'MAXZOOM must be between 0 and 15' >&2; exit 2; }
command -v pmtiles >/dev/null || { echo 'Missing official pmtiles CLI' >&2; exit 3; }
command -v sha256sum >/dev/null || { echo 'Missing sha256sum' >&2; exit 3; }
mkdir -p "$(dirname "$DEST")"
TMP="$(mktemp "${DEST}.partial.XXXXXXXX")"
HEADER="${TMP}.header.json"; CHECKSUM="${TMP}.sha256"
# Publication transactionnelle : aucune sortie existante ne change avant validation.
BACKUP="${DEST}.rollback.$$"
cleanup() { rm -f "$TMP" "$HEADER" "$CHECKSUM"; }
rollback() {
  if [[ -d "$BACKUP" ]]; then
    for suffix in '' '.header.json' '.sha256'; do
      if [[ -f "$BACKUP/$(basename "$DEST")${suffix}" ]]; then
        mv -f "$BACKUP/$(basename "$DEST")${suffix}" "${DEST}${suffix}"
      else
        rm -f "${DEST}${suffix}"
      fi
    done
    rmdir "$BACKUP" 2>/dev/null || true
  fi
}
trap 'rollback; cleanup' EXIT
pmtiles extract "$SOURCE" "$TMP" --bbox=-5.6,41.2,9.8,51.3 --maxzoom="$MAXZOOM"
pmtiles verify "$TMP"
pmtiles show "$TMP" --header-json > "$HEADER"
python3 - "$HEADER" "$TMP" <<'PY'
import json,sys,struct,os
h=json.load(open(sys.argv[1],encoding='utf-8'))
assert isinstance(h,dict) and h, 'Empty PMTiles header'
with open(sys.argv[2],'rb') as f: raw=f.read(127)
assert len(raw)==127 and raw[:7]==b'PMTiles' and raw[7]==3, 'Invalid PMTiles v3 header'
assert raw[99]==1, 'Expected vector MVT tiles'
west,south,east,north=(v/1e7 for v in struct.unpack_from('<iiii',raw,102))
assert west<=-5 and south<=41.6 and east>=9.5 and north>=51, 'Insufficient France + Corsica bounding box'
assert os.path.getsize(sys.argv[2])<=8*1024**3, 'Archive exceeds Android 8 GiB download limit'
PY
DIGEST="$(sha256sum "$TMP" | cut -d ' ' -f 1)"
printf '%s  %s\n' "$DIGEST" "$(basename "$DEST")" > "$CHECKSUM"
# Do not replace an existing map unless extraction, archive verification and metadata succeed.
mkdir "$BACKUP"
for suffix in '' '.header.json' '.sha256'; do
  [[ ! -e "${DEST}${suffix}" ]] || mv -f "${DEST}${suffix}" "$BACKUP/$(basename "$DEST")${suffix}"
done
mv -f "$TMP" "$DEST"
mv -f "$HEADER" "${DEST}.header.json"
mv -f "$CHECKSUM" "${DEST}.sha256"
rm -rf "$BACKUP"
trap - EXIT
printf 'Prepared and verified: %s (maxzoom=%s)\n' "$DEST" "$MAXZOOM"
