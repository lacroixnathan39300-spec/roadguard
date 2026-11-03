from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]
class TestVectorZoom(unittest.TestCase):
 def test_native_header_zoom(self):
  s=(ROOT/'app/src/main/java/fr/roadguard/app/MainActivity.java').read_text()
  self.assertIn('int declared=bytes[101]&255;',s)
  self.assertIn('JSONObject.quote(id)+","+vectorMaxZoom+',s)
 def test_maplibre_uses_declared_maxzoom(self):
  s=(ROOT/'app/src/main/assets/map_maplibre.html').read_text()
  self.assertIn('function setVectorPmtiles(id,maxZoom)',s)
  self.assertIn('minzoom:0,maxzoom:maxZoom',s)
  self.assertNotIn('minzoom:0,maxzoom:14,attribution:',s)
if __name__=='__main__':unittest.main()
