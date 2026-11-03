"""Guard against rejecting full-country PMTiles at the 600 MiB XYZ limit."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]
SRC=(ROOT/'app/src/main/java/fr/roadguard/app/MainActivity.java').read_text()
class CountrySizeLimits(unittest.TestCase):
    def test_france_catalog_uses_pmtiles_limit(self):
        self.assertIn('if("france".equals(id)){downloadFranceOneTap();return;}',SRC)
        self.assertIn('PUBLIC_FRANCE_PMTILES_BYTES=2898478070L',SRC)
        self.assertIn('MAX_PMTILES_DOWNLOAD_BYTES=8L*1024*1024*1024',SRC)
    def test_world_catalog_uses_pmtiles_limit(self):
        self.assertIn('pack.optLong("size",0)>("pmtiles".equals(pack.optString("format"))?MAX_PMTILES_DOWNLOAD_BYTES:600L*1024*1024)',SRC)
    def test_xyz_limit_retained(self):
        self.assertIn('600L*1024*1024',SRC)
if __name__=='__main__': unittest.main()
