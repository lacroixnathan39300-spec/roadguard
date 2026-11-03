#!/usr/bin/env python3
"""Validate a published ROADGUARD France catalog and its remote PMTiles byte ranges.
Does not claim offline rendering or full geographical coverage.
"""
import argparse, hashlib, json, re, urllib.request, urllib.parse


def check(catalog_url, timeout=25):
    u = urllib.parse.urlsplit(catalog_url)
    if u.scheme != 'https' or not u.hostname or u.username or u.password:
        raise ValueError('Catalog must be hosted at HTTPS URL')
    req = urllib.request.Request(catalog_url, headers={'Accept': 'application/json'})
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        if urllib.parse.urlsplit(resp.url).scheme != 'https':
            raise ValueError('Insecure redirect')
        raw = resp.read(1024 * 1024 + 1)
        if len(raw) > 1024 * 1024:
            raise ValueError('Catalog too large')
    data = json.loads(raw)
    packs = [p for p in data.get('packs', []) if p.get('country') == 'france' and p.get('scope') == 'country' and p.get('format') == 'pmtiles']
    if len(packs) != 1:
        raise ValueError('Expected exactly one France country PMTiles pack')
    p = packs[0]
    url = p['url']; size = p['size']; digest = p['sha256']
    if not isinstance(size, int) or isinstance(size, bool) or not 127 < size <= 8 * 1024**3:
        raise ValueError('Invalid pack size')
    if not isinstance(digest, str) or not re.fullmatch(r'[a-fA-F0-9]{64}', digest):
        raise ValueError('Invalid SHA-256')
    parsed = urllib.parse.urlsplit(url)
    if parsed.scheme != 'https' or not parsed.hostname or parsed.username or parsed.password or parsed.query or parsed.fragment or not parsed.path.endswith('.pmtiles'):
        raise ValueError('Pack must have a direct HTTPS PMTiles URL')
    req = urllib.request.Request(url, headers={'Range': 'bytes=0-126', 'Accept-Encoding': 'identity'})
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        if resp.status != 206:
            raise ValueError(f'HTTP Range unsupported: {resp.status}, expected 206')
        if urllib.parse.urlsplit(resp.url).scheme != 'https':
            raise ValueError('Insecure pack redirect')
        content_range = resp.headers.get('Content-Range', '')
        if content_range != f'bytes 0-126/{size}':
            raise ValueError(f'Unexpected Content-Range: {content_range!r}')
        header = resp.read(128)
        if len(header) != 127 or header[:7] != b'PMTiles' or header[7] != 3 or header[99] != 1:
            raise ValueError('Hosted file is not PMTiles v3 vector')
    return {'url': url, 'size': size, 'sha256_declared': digest, 'range': '206 OK', 'header': 'PMTiles v3 vector OK', 'warning': 'Full SHA-256 and map rendering NOT checked'}


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument('catalog_https_url')
    args = ap.parse_args()
    try:
        print(json.dumps(check(args.catalog_https_url), indent=2))
    except Exception as exc:
        ap.exit(1, f'FAILED: {exc}\n')

if __name__ == '__main__':
    main()
