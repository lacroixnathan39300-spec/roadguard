import pathlib,sys,tempfile,unittest,json
sys.path.insert(0,str(pathlib.Path(__file__).resolve().parent))
from install_osm_sectors import validate
class SectorBoundsTests(unittest.TestCase):
 def make(self,folder,coord):
  name='secteur_934_118_0.geojson'
  (folder/name).write_text(json.dumps({'type':'FeatureCollection','features':[{'type':'Feature','geometry':{'type':'LineString','coordinates':coord},'properties':{}}]}))
  (folder/'manifest.json').write_text(json.dumps({'format':'roadguard-osm-sectors-v1','cell':.05,'sectors':[name]}))
 def test_correct_cell(self):
  with tempfile.TemporaryDirectory() as d:
   p=pathlib.Path(d);self.make(p,[[5.91,46.71],[5.92,46.72]])
   self.assertEqual(len(validate(p)),1)
 def test_wrong_cell_rejected(self):
  with tempfile.TemporaryDirectory() as d:
   p=pathlib.Path(d);self.make(p,[[6.1,46.71],[6.2,46.72]])
   with self.assertRaisesRegex(ValueError,'hors du secteur'):validate(p)
 def test_invalid_name_rejected(self):
  with tempfile.TemporaryDirectory() as d:
   p=pathlib.Path(d);self.make(p,[[5.91,46.71],[5.92,46.72]])
   name='secteur_934_118_0.geojson'
   (p/'manifest.json').write_text(json.dumps({'format':'roadguard-osm-sectors-v1','cell':.05,'sectors':['secteur_bogus.geojson']}))
   (p/'secteur_bogus.geojson').write_bytes((p/name).read_bytes())
   with self.assertRaisesRegex(ValueError,'non conforme'):validate(p)
if __name__=='__main__':unittest.main()
