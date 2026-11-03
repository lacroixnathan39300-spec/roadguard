from pathlib import Path
import unittest
SRC=Path(__file__).resolve().parents[1]/'app/src/main/java/fr/roadguard/app/MainActivity.java'
class RegionPickerTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls): cls.source=SRC.read_text()
    def test_picker_present(self):
        self.assertIn('void chooseOfflineArea()',self.source)
        self.assertIn('Télécharger une région / un département',self.source)
    def test_rejects_unverified_catalog_entries(self):
        for fragment in ('"region".equals(scope)','"department".equals(scope)','"https".equalsIgnoreCase(url.getProtocol())','hash.matches("[0-9a-fA-F]{64}")','confirmMapDownload(candidates.get(which))'):
            self.assertIn(fragment,self.source)
    def test_no_fake_osm_bulk_download(self):
        self.assertIn('Les tuiles publiques OpenStreetMap ne peuvent pas être aspirées en masse',self.source)
    def test_gps_code_preserved(self):
        self.assertIn('void showMap()',self.source)
        self.assertIn('pushPosition()',self.source)
if __name__=='__main__':unittest.main()
