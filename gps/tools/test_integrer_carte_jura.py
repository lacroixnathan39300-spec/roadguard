import pathlib,sys,tempfile,unittest
from unittest.mock import patch
sys.path.insert(0,str(pathlib.Path(__file__).resolve().parent))
from integrer_carte_jura import integrate
class IntegrationTests(unittest.TestCase):
 def test_no_pbf_does_not_modify_project(self):
  with tempfile.TemporaryDirectory() as d:
   root=pathlib.Path(d);(root/'app/src/main/assets').mkdir(parents=True)
   with self.assertRaises(FileNotFoundError):integrate(root/'absent.pbf',root)
   self.assertFalse((root/'app/src/main/assets/osm-sectors').exists())
 def test_valid_pbf_mock_pipeline(self):
  with tempfile.TemporaryDirectory() as d:
   root=pathlib.Path(d);(root/'app/src/main/assets').mkdir(parents=True)
   pbf=root/'local.pbf';pbf.write_bytes(b'test')
   sample={'type':'FeatureCollection','features':[{'type':'Feature','properties':{'highway':'residential'},'geometry':{'type':'LineString','coordinates':[[5.91,46.71],[5.92,46.72]]}}]}
   with patch('integrer_carte_jura.extract',return_value=sample):
    result=integrate(pbf,root)
   self.assertEqual(result['secteurs_installes'],1)
   self.assertTrue((root/'app/src/main/assets/osm-sectors/manifest.json').exists())
 def test_invalid_project_keeps_source(self):
  with tempfile.TemporaryDirectory() as d:
   root=pathlib.Path(d);pbf=root/'local.pbf';pbf.write_bytes(b'test')
   with self.assertRaises(FileNotFoundError):integrate(pbf,root)
if __name__=='__main__':unittest.main()
