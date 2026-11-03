import hashlib,io,pathlib,sys,tempfile,unittest
sys.path.insert(0,str(pathlib.Path(__file__).resolve().parent))
from preparer_carte_jura import download_checked,parse_md5
class FakeResponse(io.BytesIO):
 def __enter__(self):return self
 def __exit__(self,*args):self.close()
class Tests(unittest.TestCase):
 def test_md5_parser(self):
  self.assertEqual(parse_md5('d41d8cd98f00b204e9800998ecf8427e  fichier.pbf'),'d41d8cd98f00b204e9800998ecf8427e')
 def test_verified_download(self):
  payload=b'osm-test';md5=hashlib.md5(payload).hexdigest().encode()
  def opener(url,timeout):return FakeResponse(md5 if url.endswith('.md5') else payload)
  with tempfile.TemporaryDirectory() as d:
   target=pathlib.Path(d)/'region.pbf'
   self.assertEqual(download_checked(target,opener),target)
   self.assertEqual(target.read_bytes(),payload)
 def test_invalid_hash_rejected(self):
  def opener(url,timeout):return FakeResponse(b'0'*32 if url.endswith('.md5') else b'corrupt')
  with tempfile.TemporaryDirectory() as d:
   target=pathlib.Path(d)/'region.pbf'
   with self.assertRaisesRegex(ValueError,'MD5'):download_checked(target,opener)
   self.assertFalse(target.exists());self.assertFalse(target.with_name('region.pbf.part').exists())
 def test_no_overwrite_existing_pbf(self):
  with tempfile.TemporaryDirectory() as d:
   target=pathlib.Path(d)/'region.pbf';target.write_bytes(b'original')
   def opener(url,timeout):return FakeResponse(b'0'*32)
   with self.assertRaises(FileExistsError):download_checked(target,opener)
   self.assertEqual(target.read_bytes(),b'original')
 def test_no_overwrite_partial_pbf(self):
  with tempfile.TemporaryDirectory() as d:
   target=pathlib.Path(d)/'region.pbf';partial=pathlib.Path(d)/'region.pbf.part';partial.write_bytes(b'other-job')
   def opener(url,timeout):return FakeResponse(b'0'*32)
   with self.assertRaises(FileExistsError):download_checked(target,opener)
   self.assertEqual(partial.read_bytes(),b'other-job')
if __name__=='__main__':unittest.main()
