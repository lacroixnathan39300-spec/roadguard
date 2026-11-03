import json,pathlib,sys,tempfile,unittest
sys.path.insert(0,str(pathlib.Path(__file__).resolve().parent))
from verifier_cartes_embarquees import verify
class VerificationTests(unittest.TestCase):
 def test_absent(self):
  with tempfile.TemporaryDirectory() as d:
   with self.assertRaisesRegex(ValueError,'AUCUNE CARTE'):verify(d)
 def test_real_assets_structure(self):
  with tempfile.TemporaryDirectory() as d:
   folder=pathlib.Path(d)/'app/src/main/assets/osm-sectors'
   folder.mkdir(parents=True)
   name='secteur_934_118_0.geojson'
   (folder/'manifest.json').write_text(json.dumps({'format':'roadguard-osm-sectors-v1','cell':.05,'sectors':[name]}))
   (folder/name).write_text(json.dumps({'type':'FeatureCollection','features':[{'type':'Feature','properties':{'highway':'residential'},'geometry':{'type':'LineString','coordinates':[[5.91,46.71],[5.92,46.72]]}}]}))
   self.assertEqual(verify(d)['points'],2)
 def test_empty(self):
  with tempfile.TemporaryDirectory() as d:
   folder=pathlib.Path(d)/'app/src/main/assets/osm-sectors'
   folder.mkdir(parents=True)
   name='secteur_934_118_0.geojson'
   (folder/'manifest.json').write_text(json.dumps({'format':'roadguard-osm-sectors-v1','cell':.05,'sectors':[name]}))
   (folder/name).write_text(json.dumps({'type':'FeatureCollection','features':[]}))
   with self.assertRaisesRegex(ValueError,'CARTE VIDE'):verify(d)
if __name__=='__main__':unittest.main()
