#!/usr/bin/env python3
"""Generate a ROADGUARD catalog only from a locally present PMTiles v3 archive.
This checks structure and declared bounds, NOT actual coverage or rendering.
"""
import argparse
import hashlib
import json
import pathlib
import struct
from urllib.parse import urlsplit


def inspect_archive(path):
    size = path.stat().st_size
    with path.open('rb') as f:
        h = f.read(127)
    if len(h) != 127 or h[:7] != b'PMTiles' or h[7] != 3:
        raise ValueError('Not a PMTiles v3 archive')
    if h[99] != 1:
        raise ValueError('Expected vector MVT tiles, not raster')
    for offset in (8, 24, 40, 56):
        start, length = struct.unpack_from('<QQ', h, offset)
        if not length or start < 127 or start > size or length > size - start:
            raise ValueError('PMTiles section is outside the archive')
    west, south, east, north = struct.unpack_from('<iiii', h, 102)
    if not (west <= -5_0000000 and east >= 9_0000000 and south <= 41_5000000 and north >= 51_0000000):
        raise ValueError('Declared bounds do not cover mainland France and Corsica')
    sha = hashlib.sha256()
    with path.open('rb') as f:
        for chunk in iter(lambda: f.read(8 * 1024 * 1024), b''):
            sha.update(chunk)
    return size, sha.hexdigest()


def build_entry(path, url):
    u = urlsplit(url)
    if u.scheme != 'https' or not u.hostname or u.username or u.password or u.query or u.fragment or not u.path.endswith('.pmtiles'):
        raise ValueError('A direct public HTTPS .pmtiles URL is required')
    size, digest = inspect_archive(path)
    return {'id': 'france_metropolitaine_corse', 'name': 'France métropolitaine + Corse',
            'country': 'france', 'scope': 'country', 'format': 'pmtiles',
            'url': url, 'size': size, 'sha256': digest}


def main():
    p = argparse.ArgumentParser()
    p.add_argument('archive', type=pathlib.Path)
    p.add_argument('--https-url', required=True)
    p.add_argument('--out', type=pathlib.Path, default=pathlib.Path('catalog.json'))
    a = p.parse_args()
    try:
        entry = build_entry(a.archive, a.https_url)
    except (ValueError, OSError) as exc:
        p.error(str(exc))
    a.out.write_text(json.dumps({'schema': 1, 'packs': [entry]}, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
    print('Catalog generated:', a.out)
    print('WARNING: Bounds do not prove complete tile coverage. Test HTTP range, styles and offline rendering.')


if __name__ == '__main__':
    main()
