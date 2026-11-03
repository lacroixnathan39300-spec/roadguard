import pathlib, unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]
class AttributionTest(unittest.TestCase):
 def test_offline_map_displays_credit(self):
  html=(ROOT/'app/src/main/assets/map_offline.html').read_text(encoding='utf-8')
  self.assertIn('id="osm-credit"',html)
  self.assertIn('OpenStreetMap contributors',html)
  self.assertIn('openstreetmap.org/copyright',html)
  self.assertIn('ODbL 1.0',html)
 def test_version(self):
  gradle=(ROOT/'app/build.gradle').read_text(encoding='utf-8')
  self.assertIn("versionName '11.13.10'",gradle)
  self.assertIn('versionCode 171',gradle)
if __name__=='__main__':unittest.main()
