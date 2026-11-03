import sys, pathlib, tempfile, hashlib, unittest
sys.path.insert(0,str(pathlib.Path(__file__).resolve().parents[1]/'tools'))
from publish_verified_catalog import build_catalog
class CatalogTests(unittest.TestCase):
 def setUp(self):
  self.t=tempfile.TemporaryDirectory();self.root=pathlib.Path(self.t.name)
  self.data=b'PMTiles\x03'+bytes(140);(self.root/'jura_demo.pmtiles').write_bytes(self.data)
  self.entry={'id':'jura_demo','status':'EXTRACTED_UNHOSTED','bytes':len(self.data),'sha256':hashlib.sha256(self.data).hexdigest(),'bbox':[5.2,46.3,6.5,47.2]}
 def tearDown(self):self.t.cleanup()
 def generate(self):return build_catalog({'results':[self.entry]},self.root,'https://example.org/maps/')
 def test_valid_catalog(self):
  fields=self.generate().splitlines()[1].split('\t');self.assertEqual(len(fields),9);self.assertEqual(fields[1],'https://example.org/maps/jura_demo.pmtiles');self.assertEqual(fields[4:8],['46.3','5.2','47.2','6.5'])
 def test_corruption_rejected(self):
  (self.root/'jura_demo.pmtiles').write_bytes(self.data+b'X')
  with self.assertRaises(ValueError):self.generate()
 def test_non_https_rejected(self):
  with self.assertRaises(ValueError):build_catalog({'results':[self.entry]},self.root,'http://example.org/maps/')
 def test_missing_file_rejected(self):
  (self.root/'jura_demo.pmtiles').unlink()
  with self.assertRaises(ValueError):self.generate()
 def test_duplicate_rejected(self):
  with self.assertRaises(ValueError):build_catalog({'results':[self.entry,self.entry]},self.root,'https://example.org/maps/')
if __name__=='__main__':unittest.main()
