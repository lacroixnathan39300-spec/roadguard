"""Test extraction behavior without network or large real PMTiles archives."""
import os
import pathlib
import subprocess
import tempfile
import unittest

SCRIPT = pathlib.Path(__file__).with_name('prepare_france_pmtiles.sh')

class ExtractionTests(unittest.TestCase):
    def run_case(self, fail_verify=False):
        with tempfile.TemporaryDirectory() as root:
            root = pathlib.Path(root)
            bin_dir = root / 'bin'; bin_dir.mkdir()
            pm = bin_dir / 'pmtiles'
            pm.write_text('''#!/bin/sh
case "$1" in
extract) printf 'new archive' > "$3" ;;
verify) [ "${FAIL_VERIFY:-0}" != 1 ] ;;
show) printf '{"mock":true}\\n' ;;
*) exit 9 ;;
esac
''')
            pm.chmod(0o755)
            target = root / 'dir with spaces' / 'france.pmtiles'
            target.parent.mkdir()
            target.write_bytes(b'previous good archive')
            env = dict(os.environ, PATH=str(bin_dir)+os.pathsep+os.environ['PATH'], FAIL_VERIFY='1' if fail_verify else '0')
            p = subprocess.run(['bash', str(SCRIPT), 'https://example.org/planet.pmtiles', str(target)], env=env, capture_output=True, text=True)
            return p.returncode, target.read_bytes(), (target.with_suffix('.pmtiles.sha256').read_text() if target.with_suffix('.pmtiles.sha256').exists() else None), list(target.parent.glob('*.partial.*'))
    def test_failure_preserves_existing_archive(self):
        code, data, sha, leftovers = self.run_case(True)
        self.assertNotEqual(code, 0)
        self.assertEqual(data, b'previous good archive')
        self.assertIsNone(sha)
        self.assertEqual(leftovers, [])
    def test_success_replaces_and_checksums_final_name(self):
        code, data, sha, leftovers = self.run_case(False)
        self.assertEqual(code, 0)
        self.assertEqual(data, b'new archive')
        self.assertTrue(sha.endswith('  france.pmtiles\n'))
        self.assertEqual(leftovers, [])

if __name__ == '__main__': unittest.main()
