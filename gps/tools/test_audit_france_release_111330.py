import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch
from audit_france_release import audit, REQUIRED_DEVICE_CHECKS

class ReleaseGateTests(unittest.TestCase):
    def test_missing_archive_blocks(self):
        r = audit()
        self.assertFalse(r['ready'])
        self.assertIn('archive_present', r['blockers'])
    def test_no_network_verification_blocks(self):
        r = audit(url='https://example.org/france.pmtiles')
        self.assertIn('http_range_verified', r['blockers'])
    def test_partial_device_report_blocks(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / 'report.json'
            p.write_text(json.dumps({'apk_installed': True}))
            r = audit(device_report=p)
            self.assertIn('android_offline_test', r['blockers'])
    def test_complete_device_report_does_not_override_other_checks(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / 'report.json'
            p.write_text(json.dumps({k: True for k in REQUIRED_DEVICE_CHECKS}))
            r = audit(device_report=p)
            self.assertNotIn('android_offline_test', r['blockers'])
            self.assertFalse(r['ready'])
    def test_invalid_report_blocks(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / 'report.json'
            p.write_text('broken json')
            self.assertIn('android_offline_test', audit(device_report=p)['blockers'])
    def test_http_url_rejected(self):
        self.assertIn('public_https_url', audit(url='http://example.org/france.pmtiles')['blockers'])

if __name__ == '__main__': unittest.main()
