import pathlib,unittest
class DarkMapTests(unittest.TestCase):
 def test_real_dark_tiles(self):
  s=(pathlib.Path(__file__).resolve().parents[1]/'app/src/main/assets/map.html').read_text()
  self.assertNotIn('basemaps.cartocdn.com',s)
  self.assertNotIn('filter:brightness(.65)',s)
 def test_speed_gps(self):
  s=(pathlib.Path(__file__).resolve().parents[1]/'app/src/main/assets/map.html').read_text()
  self.assertIn('rg-speed-value',s)
  self.assertIn('Math.round(Number(speedKmh)',s)
