import pathlib,sys,tempfile,unittest
sys.path.insert(0,str(pathlib.Path(__file__).resolve().parent))
from pbf_to_sectors import generate
from audit_osm_sectors import audit
class TestDirect(unittest.TestCase):
 def test_sector_pack(self):
  features=[]
  for i in range(50):
   features.append({'type':'Feature','geometry':{'type':'LineString','coordinates':[[5.9+i*.0001,46.7],[5.901+i*.0001,46.701]]},'properties':{'highway':'residential'}})
  with tempfile.TemporaryDirectory() as tmp:
   count,roads=generate({'type':'FeatureCollection','features':features},pathlib.Path(tmp))
   self.assertEqual(roads,50)
   self.assertGreater(count,0)
   self.assertEqual(audit(pathlib.Path(tmp))['lignes'],50)
 def test_empty(self):
  with tempfile.TemporaryDirectory() as tmp:
   with self.assertRaises(ValueError):generate({'type':'FeatureCollection','features':[]},pathlib.Path(tmp))
if __name__=='__main__':unittest.main()
