import sys,unittest,pathlib,math
sys.path.insert(0,str(pathlib.Path(__file__).resolve().parents[1]/'tools'))
from plan_gps_pmtiles import gps_bbox,make_jobs
class GPSPlanTest(unittest.TestCase):
 def test_jura_bbox(self):
  b=gps_bbox(46.75,5.9)
  self.assertLess(b[0],5.9);self.assertGreater(b[2],5.9)
  self.assertLess(b[1],46.75);self.assertGreater(b[3],46.75)
  self.assertGreater((b[3]-b[1])*111.32,99)
 def test_high_latitude(self):
  b=gps_bbox(70,20)
  self.assertGreater((b[2]-b[0])*111.32*math.cos(math.radians(70)),99)
 def test_invalid(self):
  for args in [(91,0),(0,181),(0,0,0),(0,179.9)]:
   with self.assertRaises(ValueError):gps_bbox(*args)
 def test_commands(self):
  jobs=make_jobs([{'id':'jura','lat':46.75,'lon':5.9}])
  self.assertEqual(len(jobs),1)
  self.assertIn('jura.pmtiles',jobs[0]['command'])
  self.assertEqual(jobs[0]['status'],'NOT_DOWNLOADED')
if __name__=='__main__':unittest.main()
