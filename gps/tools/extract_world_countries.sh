#!/usr/bin/env bash
# Run on a computer/server with the official Protomaps CLI installed.
# Source must be an existing, authorized Protomaps daily-build PMTiles URL.
# Creates country-sized BBOX extracts; bounding boxes include neighboring border tiles.
set -euo pipefail
: "${PROTOMAPS_SOURCE:?Set PROTOMAPS_SOURCE to a valid HTTPS Protomaps daily-build .pmtiles URL}"
command -v pmtiles >/dev/null || { echo 'Install pmtiles CLI first'; exit 1; }
mkdir -p packs
# lon_min,lat_min,lon_max,lat_max (approximate geographical coverage)
while IFS='|' read -r name bbox; do
  echo "Extracting ${name}..."
  pmtiles extract "$PROTOMAPS_SOURCE" "packs/${name}.pmtiles" --bbox="$bbox" --maxzoom="${MAX_ZOOM:-12}"
done <<'COUNTRIES'
france|-5.6,41.2,9.9,51.2
switzerland|5.8,45.7,10.6,47.9
germany|5.8,47.2,15.2,55.2
belgium|2.4,49.4,6.5,51.6
italy|6.4,35.4,18.7,47.2
spain|-9.6,35.8,4.4,43.9
luxembourg|5.7,49.3,6.6,50.3
netherlands|3.2,50.7,7.3,53.8
austria|9.4,46.3,17.3,49.1
portugal|-9.7,36.8,-6.0,42.3
COUNTRIES
printf '\nPack files created. Check sizes (ROADGUARD limit: 600 MiB per pack).\n'
