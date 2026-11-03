import sys,pathlib,unittest
sys.path.insert(0,str(pathlib.Path(__file__).parent))
from split_osm_sectors import split
class Tests(unittest.TestCase):
 def test_preserves_roads_inside_cells(self):
  features=[{'type':'Feature','geometry':{'type':'LineString','coordinates':[[6+i*.01,47.001],[6+i*.01,47.011]]},'properties':{'id':i}} for i in range(30)]
  sectors=split({'type':'FeatureCollection','features':features},max_points=6)
  ids={f['properties']['id'] for s in sectors.values() for f in s['features']}
  self.assertEqual(ids,set(range(30)))
 def test_crossing_road_is_in_both_cells(self):
  feature={'type':'Feature','geometry':{'type':'LineString','coordinates':[[6.01,47.01],[6.09,47.01]]},'properties':{'id':42}}
  sectors=split({'type':'FeatureCollection','features':[feature]},cell=.05)
  self.assertTrue(any('_120_0' in name for name in sectors) or len(sectors)>=2)
  self.assertGreaterEqual(len(sectors),2)
  for sector in sectors.values():
   for f in sector['features']:
    self.assertEqual(f['properties']['id'],42)
 def test_refuses_oversized_segment_count(self):
  feature={'type':'Feature','geometry':{'type':'LineString','coordinates':[[6,47.001],[6.001,47.001]]*10}}
  result=split({'type':'FeatureCollection','features':[feature]},max_points=5)
  self.assertGreater(len(result),1)
 def test_invalid_coordinates(self):
  feature={'type':'Feature','geometry':{'type':'LineString','coordinates':[[6,47],[float('nan'),47.01]]}}
  with self.assertRaises(ValueError):split({'type':'FeatureCollection','features':[feature]})
if __name__=='__main__':unittest.main()
