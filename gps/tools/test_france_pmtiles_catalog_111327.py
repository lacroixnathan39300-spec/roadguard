import pathlib
import struct
import tempfile
import unittest
from build_france_pmtiles_catalog import build_entry

class CatalogTests(unittest.TestCase):
    def test_valid_header_and_catalog(self):
        with tempfile.TemporaryDirectory() as d:
            path = pathlib.Path(d) / 'france.pmtiles'
            header = bytearray(127)
            header[:7] = b'PMTiles'; header[7] = 3; header[99] = 1
            for i, off in enumerate((8, 24, 40, 56)):
                struct.pack_into('<QQ', header, off, 127 + i * 8, 8)
            struct.pack_into('<iiii', header, 102, -60000000, 410000000, 100000000, 520000000)
            path.write_bytes(header + bytes(32))
            entry = build_entry(path, 'https://example.org/france.pmtiles')
            self.assertEqual(entry['scope'], 'country')
            self.assertEqual(len(entry['sha256']), 64)
            with self.assertRaises(ValueError):
                build_entry(path, 'https://example.org/france.pmtiles?token=abc')
            header[99] = 2; path.write_bytes(header + bytes(32))
            with self.assertRaisesRegex(ValueError, 'vector'):
                build_entry(path, 'https://example.org/france.pmtiles')
            header[99] = 1; struct.pack_into('<QQ', header, 8, 999999, 8)
            path.write_bytes(header + bytes(32))
            with self.assertRaisesRegex(ValueError, 'outside'):
                build_entry(path, 'https://example.org/france.pmtiles')

if __name__ == '__main__': unittest.main()
