#!/usr/bin/env python3
"""Build a ROADGUARD catalog entry from an existing complete France PMTiles v3 file.
No network download or hosting is performed by this tool.
"""
import argparse, hashlib, json, struct
from pathlib import Path


def inspect(path):
    size = path.stat().st_size
    with path.open('rb') as f: h = f.read(127)
    if len(h) != 127 or h[:7] != b'PMTiles' or h[7] != 3:
        raise ValueError('Archive PMTiles v3 invalide')
    for offset in (8, 24, 40, 56):
        start, length = struct.unpack_from('<QQ', h, offset)
        if start > size or length > size - start:
            raise ValueError('Sections PMTiles hors fichier')
    bounds = tuple(v / 1e7 for v in struct.unpack_from('<iiii', h, 102))
    west, south, east, north = bounds
    if not (west <= -5 and south <= 41.6 and east >= 9.5 and north >= 51):
        raise ValueError(f'Emprise France + Corse insuffisante : {bounds}')
    if h[99] != 1:
        raise ValueError('Le fichier doit contenir des tuiles vectorielles MVT')
    digest = hashlib.sha256()
    with path.open('rb') as f:
        for chunk in iter(lambda: f.read(4 * 1024 * 1024), b''):
            digest.update(chunk)
    return size, digest.hexdigest(), bounds


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('pmtiles', type=Path)
    ap.add_argument('--url', required=True, help='URL HTTPS directe du fichier déjà hébergé')
    ap.add_argument('--output', type=Path, default=Path('roadguard-france-catalog.json'))
    args = ap.parse_args()
    if not args.url.startswith('https://') or '?' in args.url or '#' in args.url:
        ap.error('URL HTTPS directe sans paramètres requise')
    size, sha, bounds = inspect(args.pmtiles)
    item = {'id': 'france_metropolitaine_corse', 'name': 'France métropolitaine + Corse',
            'country': 'france', 'scope': 'country', 'format': 'pmtiles',
            'url': args.url, 'sha256': sha, 'size': size}
    args.output.write_text(json.dumps({'packs': [item]}, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
    print(f'Pack validé (en-tête et emprise uniquement) : {size} octets, SHA-256 {sha}, bbox {bounds}')
    print('Catalogue écrit :', args.output)
    print('ATTENTION : vérifier aussi les couches et le rendu réel sur Android avant publication.')

if __name__ == '__main__': main()
