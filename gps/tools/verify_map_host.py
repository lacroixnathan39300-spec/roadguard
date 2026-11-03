#!/usr/bin/env python3
"""Check that an HTTPS PMTiles host supports byte-range delivery (without downloading the map)."""
import argparse
import json
import urllib.request
from urllib.parse import urlsplit


def check(url, opener=urllib.request.urlopen):
    u = urlsplit(url)
    if u.scheme != 'https' or not u.hostname or u.username or u.password or u.fragment or not u.path.endswith('.pmtiles'):
        raise ValueError('A direct HTTPS .pmtiles URL is required')
    req = urllib.request.Request(url, headers={'Range': 'bytes=0-126', 'Accept-Encoding': 'identity', 'User-Agent': 'ROADGUARD-map-check/1'})
    with opener(req, timeout=25) as response:
        status = response.status
        cr = response.headers.get('Content-Range', '')
        data = response.read(128)
        if status != 206 or not cr.startswith('bytes 0-126/'):
            raise ValueError(f'HTTP byte-range support missing: status={status}, Content-Range={cr!r}')
        if len(data) != 127 or data[:7] != b'PMTiles' or data[7] != 3:
            raise ValueError('Remote file is not PMTiles v3')
        total = int(cr.rsplit('/', 1)[-1])
        if total < 127:
            raise ValueError('Invalid total archive length')
        return {'url': url, 'http_status': status, 'bytes': total, 'pmtiles_version': 3, 'range_supported': True}


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('url')
    a = p.parse_args()
    try:
        print(json.dumps(check(a.url), ensure_ascii=False, indent=2))
    except Exception as exc:
        p.exit(1, f'BLOQUÉ: {exc}\n')

if __name__ == '__main__':
    main()
