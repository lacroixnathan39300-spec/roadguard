"""Regression checks for reusing a PMTiles archive already on the phone."""
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[1]
JAVA=(ROOT/'app/src/main/java/fr/roadguard/app/MainActivity.java').read_text(encoding='utf-8')
HTML=(ROOT/'app/src/main/assets/map_maplibre.html').read_text(encoding='utf-8')

class LocalPmtilesImportTests(unittest.TestCase):
 def test_saf_import_does_not_start_a_network_download(self):
  self.assertIn('startActivityForResult(intent,61)',JAVA)
  self.assertIn('if(request==61&&result==RESULT_OK&&data!=null){importExistingPmtiles(data.getData());return;}',JAVA)
  block=JAVA[JAVA.index('void importExistingPmtiles(Uri source)'):JAVA.index('static long pmtilesLE64')]
  self.assertIn('getContentResolver().openInputStream(source)',block)
  self.assertIn('france_local.pmtiles',block)
  self.assertIn('pmtilesHeaderSummary(tmp)',block)
  self.assertIn('pmtilesCoversFranceAndCorsica(tmp)',block)
  self.assertIn('Carte locale importée · aucun téléchargement effectué',block)
  self.assertNotIn('openConnection(',block)
 def test_local_vector_tile_and_glyph_requests_are_intercepted(self):
  self.assertIn('Access-Control-Allow-Origin',''.join(JAVA.splitlines()))
  self.assertIn('localTileResponse(vector?reader.vectorMimeType():reader.mimeType()',JAVA)
  self.assertIn('getAssets().open("glyphs/"+glyph)',JAVA)
  self.assertIn('roadguard.local/fonts/{fontstack}/{range}.pbf',HTML)
 def test_place_and_road_name_layers_exist(self):
  self.assertIn("'source-layer':'transportation_name'",HTML)
  self.assertIn("'source-layer':'place'",HTML)
  self.assertIn("['get','name:fr']",HTML)
  self.assertIn("['get','name']",HTML)

if __name__=='__main__':unittest.main()
