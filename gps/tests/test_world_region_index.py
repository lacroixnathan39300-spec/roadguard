import importlib.util, pathlib, unittest
p=pathlib.Path(__file__).resolve().parents[1]/'tools/generate_world_region_index.py'
s=importlib.util.spec_from_file_location('regions',p);m=importlib.util.module_from_spec(s);s.loader.exec_module(m)
class Tests(unittest.TestCase):
 def test_regions(self):
  x={'type':'FeatureCollection','features':[{'type':'Feature','properties':{'ISO_A2':'FR','name':'France'},'geometry':{'type':'Polygon','coordinates':[[[5,46],[7,46],[7,48],[5,46]]]}}]}
  r=m.build(x,'ISO_A2');self.assertEqual(r['regions'][0]['bbox'],[5,46,7,48]);self.assertIsNone(r['regions'][0]['pack_url'])
 def test_antimeridian(self):
  x={'type':'FeatureCollection','features':[{'type':'Feature','properties':{'ISO_A2':'FJ'},'geometry':{'type':'Polygon','coordinates':[[[179,-16],[-179,-16],[179,-15],[179,-16]]]}}]}
  self.assertTrue(m.build(x,'ISO_A2')['regions'][0]['requires_polygon_clip'])
 def test_invalid(self):
  with self.assertRaises(ValueError):m.build({'type':'FeatureCollection','features':[{'type':'Feature','properties':{},'geometry':{'type':'Polygon','coordinates':[[[5,46],[6,46],[5,46]]]}}]},'ISO_A2')
if __name__=='__main__':unittest.main()
