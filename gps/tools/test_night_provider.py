import pathlib,unittest
class NightProviderTests(unittest.TestCase):
 def test_no_carto_watermark_provider(self):
  html=(pathlib.Path(__file__).resolve().parents[1]/'app/src/main/assets/map.html').read_text()
  self.assertNotIn('basemaps.cartocdn.com',html)
  self.assertIn('tile.openstreetmap.org',html)
 def test_night_only_filters_tile_images(self):
  html=(pathlib.Path(__file__).resolve().parents[1]/'app/src/main/assets/map.html').read_text()
  self.assertIn('body.rg-night .leaflet-tile-pane img.leaflet-tile',html)
  self.assertIn('function setRoadguardNight(enabled)',html)
if __name__=='__main__':unittest.main()
