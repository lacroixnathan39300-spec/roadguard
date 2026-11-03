import json,pathlib,sys,tempfile,unittest
sys.path.insert(0,str(pathlib.Path(__file__).parent))
from install_osm_sectors import install,validate
class InstallerTests(unittest.TestCase):
 def setUp(self):
  self.tmp=tempfile.TemporaryDirectory();self.addCleanup(self.tmp.cleanup)
  self.root=pathlib.Path(self.tmp.name);self.src=self.root/'source';self.src.mkdir()
  self.name='secteur_940_120_0.geojson'
  self.manifest={'format':'roadguard-osm-sectors-v1','cell':0.02,'sectors':[self.name]}
  (self.src/'manifest.json').write_text(json.dumps(self.manifest))
  self.geo={'type':'FeatureCollection','features':[{'type':'Feature','geometry':{'type':'LineString','coordinates':[[2.405,18.805],[2.415,18.815]]}}]}
  (self.src/self.name).write_text(json.dumps(self.geo))
 def test_custom_cell_and_install(self):
  self.assertEqual(install(self.src,self.root),1)
  self.assertTrue((self.root/'app/src/main/assets/osm-sectors'/self.name).exists())
 def test_invalid_sector_does_not_erase_existing(self):
  install(self.src,self.root)
  existing=self.root/'app/src/main/assets/osm-sectors'/self.name
  (self.src/self.name).write_text('{bad')
  with self.assertRaises(ValueError):install(self.src,self.root)
  self.assertTrue(existing.exists())
 def test_duplicate_rejected(self):
  self.manifest['sectors']=[self.name,self.name]
  (self.src/'manifest.json').write_text(json.dumps(self.manifest))
  with self.assertRaises(ValueError):validate(self.src)
if __name__=='__main__':unittest.main()
