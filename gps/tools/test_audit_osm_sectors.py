import json,pathlib,tempfile,unittest,sys
sys.path.insert(0,str(pathlib.Path(__file__).resolve().parent))
from audit_osm_sectors import audit
class AuditTests(unittest.TestCase):
 def test_valid(self):
  with tempfile.TemporaryDirectory() as d:
   p=pathlib.Path(d);name='secteur_900_110_0.geojson'
   (p/'manifest.json').write_text(json.dumps({'format':'roadguard-osm-sectors-v1','cell':0.05,'sectors':[name],'attribution':'© OpenStreetMap contributors'}))
   (p/name).write_text(json.dumps({'type':'FeatureCollection','features':[{'type':'Feature','geometry':{'type':'LineString','coordinates':[[5.9,46.7],[5.91,46.71]]}}]}))
   result=audit(p);self.assertEqual(result['points'],2);self.assertEqual(result['secteurs'],1)
 def test_missing(self):
  with tempfile.TemporaryDirectory() as d:
   p=pathlib.Path(d);(p/'manifest.json').write_text(json.dumps({'format':'roadguard-osm-sectors-v1','cell':0.05,'sectors':['secteur_1_2_0.geojson']}))
   with self.assertRaises(ValueError):audit(p)
if __name__=='__main__':unittest.main()
