import unittest
from pbf_to_sectors import clip_segment,clip_polyline
B=(5.0,46.0,6.0,47.0)
class ClipTests(unittest.TestCase):
 def test_crossing_without_internal_vertex(self):
  self.assertEqual(clip_segment([4,46.5],[7,46.5],B),[[5.,46.5],[6.,46.5]])
 def test_outside(self):
  self.assertIsNone(clip_segment([4,45],[4.5,45.5],B))
 def test_vertical(self):
  self.assertEqual(clip_segment([5.5,45],[5.5,48],B),[[5.5,46.],[5.5,47.]])
 def test_disjoint_pieces(self):
  parts=clip_polyline([[4,46.5],[7,46.5],[7,48],[7,46.5],[4,46.5]],B)
  self.assertEqual(len(parts),2)
 def test_all_coordinates_in_bounds(self):
  for line in clip_polyline([[4,45],[5.5,46.5],[7,48]],B):
   for x,y in line:self.assertTrue(5<=x<=6 and 46<=y<=47)
if __name__=='__main__':unittest.main()
