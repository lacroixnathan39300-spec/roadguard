#!/usr/bin/env python3
"""Conservative France offline release gate; never infer device success from source code."""
import argparse
import json
import pathlib
import sys
from urllib.parse import urlsplit
from build_france_pmtiles_catalog import build_entry
from verify_map_host import check as check_host

REQUIRED_DEVICE_CHECKS = ('apk_installed', 'airplane_mode', 'roads_visible',
                          'city_labels_visible', 'gps_blue_visible', 'france_corsica_coverage')

def audit(archive=None, url=None, check_network=False, device_report=None):
    checks = []
    def add(name, ok, detail):
        checks.append({'check': name, 'passed': bool(ok), 'detail': str(detail)})
    path = pathlib.Path(archive) if archive else None
    add('archive_present', bool(path and path.is_file()), path or 'No PMTiles archive supplied')
    if path and path.is_file():
        try:
            if not url:
                raise ValueError('No public HTTPS URL supplied')
            entry = build_entry(path, url)
            add('archive_and_catalog_valid', True, f"{entry['size']} bytes, SHA256 {entry['sha256']}")
        except (ValueError, OSError) as exc:
            add('archive_and_catalog_valid', False, exc)
    else:
        add('archive_and_catalog_valid', False, 'Requires a real France + Corsica PMTiles archive')
    u = urlsplit(url or '')
    url_valid = u.scheme == 'https' and bool(u.hostname) and not u.username and not u.password and not u.query and not u.fragment and u.path.endswith('.pmtiles')
    add('public_https_url', url_valid, url or 'No hosting URL')
    if check_network:
        try:
            if not url_valid:
                raise ValueError('Missing valid HTTPS URL')
            remote = check_host(url)
            add('http_range_verified', True, remote)
        except Exception as exc:
            add('http_range_verified', False, exc)
    else:
        add('http_range_verified', False, 'Network verification was not performed')
    if device_report:
        try:
            report = json.loads(pathlib.Path(device_report).read_text(encoding='utf-8'))
            missing = [k for k in REQUIRED_DEVICE_CHECKS if report.get(k) is not True]
            add('android_offline_test', not missing,
                'All required Xiaomi checks recorded' if not missing else 'Missing/failed: ' + ', '.join(missing))
        except (OSError, ValueError, TypeError) as exc:
            add('android_offline_test', False, f'Invalid device report: {exc}')
    else:
        add('android_offline_test', False, 'Requires actual Xiaomi airplane-mode test and device report')
    return {'ready': all(c['passed'] for c in checks), 'checks': checks,
            'blockers': [c['check'] for c in checks if not c['passed']]}

def main():
    p = argparse.ArgumentParser()
    p.add_argument('--archive')
    p.add_argument('--url')
    p.add_argument('--out', default='france-release-audit.json')
    p.add_argument('--network', action='store_true')
    p.add_argument('--device-report', help='JSON of verified Xiaomi device checks')
    a = p.parse_args()
    result = audit(a.archive, a.url, a.network, a.device_report)
    pathlib.Path(a.out).write_text(json.dumps(result, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
    print(json.dumps(result, indent=2, ensure_ascii=False))
    return 0 if result['ready'] else 2

if __name__ == '__main__':
    sys.exit(main())
