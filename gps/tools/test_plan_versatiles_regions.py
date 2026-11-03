import unittest
from plan_versatiles_regions import build, DEFAULT_SOURCE
class Tests(unittest.TestCase):
 def test_france_swiss(self):
  p=build([{'id':'france_mainland','bbox':[-5.2,41.2,9.7,51.2]},{'id':'switzerland','bbox':[5.9,45.8,10.6,47.9]}],DEFAULT_SOURCE,14)
  self.assertEqual(len(p),2)
  self.assertIn('france_mainland.pmtiles',p[0]['command'])
  self.assertEqual(p[0]['status'],'NOT_DOWNLOADED')
 def test_reject_bad(self):
  for bbox in [[180,-1,-179,1],[-1,95,1,100],[1,1,1,2]]:
   with self.assertRaises(ValueError):build([{'id':'bad','bbox':bbox}],DEFAULT_SOURCE,14)
 def test_reject_duplicates(self):
  with self.assertRaises(ValueError):build([{'id':'fr','bbox':[0,0,1,1]}]*2,DEFAULT_SOURCE,14)
 def test_reject_http(self):
  with self.assertRaises(ValueError):build([{'id':'fr','bbox':[0,0,1,1]}],'http://example.org/maps',14)
if __name__=='__main__':unittest.main()
