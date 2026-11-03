#!/usr/bin/env python3
"""Prepare a locally available, licensed PMTiles region for future ROADGUARD hosting.

Does not download map data, start a server, or enable Android offline downloads.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import tempfile


def bbox(value):
    try:
        vals = [float(x) for x in value.split(',')]
    except ValueError as exc:
        raise argparse.ArgumentTypeError('bbox must be four numbers') from exc
    if len(vals) != 4 or not (-180 <= vals[0] < vals[2] <= 180 and -90 <= vals[1] < vals[3] <= 90):
        raise argparse.ArgumentTypeError('invalid min_lon,min_lat,max_lon,max_lat')
    return vals


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--pmtiles-cli', required=True, type=Path)
    p.add_argument('--source', required=True, type=Path, help='Local PMTiles source with appropriate redistribution rights')
    p.add_argument('--bbox', required=True, type=bbox)
    p.add_argument('--region-id', required=True)
    p.add_argument('--output-dir', required=True, type=Path)
    p.add_argument('--attribution', required=True)
    args = p.parse_args()
    if not args.region_id.replace('-', '').replace('_', '').isalnum() or len(args.region_id) > 70:
        p.error('region-id must be alphanumeric, hyphen or underscore, max 70 chars')
    if not args.source.is_file() or not args.pmtiles_cli.is_file():
        p.error('PMTiles source and executable must exist')
    args.output_dir.mkdir(parents=True, exist_ok=True)
    destination = args.output_dir / (args.region_id + '.pmtiles')
    manifest = args.output_dir / (args.region_id + '.json')
    if destination.exists() or manifest.exists():
        p.error('destination already exists; refusing to overwrite an existing region')
    with tempfile.TemporaryDirectory(prefix='pmtiles-region-', dir=args.output_dir) as tmp:
        candidate = Path(tmp) / 'region.pmtiles'
        box = ','.join(str(x) for x in args.bbox)
        subprocess.run([str(args.pmtiles_cli), 'extract', str(args.source), str(candidate), '--bbox', box], check=True)
        subprocess.run([str(args.pmtiles_cli), 'verify', str(candidate)], check=True)
        details = subprocess.run([str(args.pmtiles_cli), 'show', str(candidate)], capture_output=True, text=True, check=True).stdout
        if 'pmtiles spec version: 3' not in details:
            raise RuntimeError('output is not PMTiles v3')
        h = hashlib.sha256()
        with candidate.open('rb') as stream:
            for chunk in iter(lambda: stream.read(1024 * 1024), b''):
                h.update(chunk)
        data = {
            'schema_version': 1,
            'region_id': args.region_id,
            'filename': destination.name,
            'bbox': args.bbox,
            'bytes': candidate.stat().st_size,
            'sha256': h.hexdigest(),
            'format': 'pmtiles-v3',
            'attribution': args.attribution,
            'status': 'prepared_locally_not_hosted',
            'source_filename': args.source.name,
            'notes': 'A hosting endpoint, Android downloader and map rendering connection are still required.'
        }
        pending_manifest = Path(tmp) / 'manifest.json'
        pending_manifest.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
        os.replace(candidate, destination)
        os.replace(pending_manifest, manifest)
    print(json.dumps(data, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
