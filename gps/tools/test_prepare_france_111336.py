import pathlib, subprocess, tempfile, unittest
SCRIPT=pathlib.Path(__file__).with_name('prepare_france_pmtiles.sh')
class TestSourceValidation(unittest.TestCase):
 def test_missing_local_file_rejected_before_extraction(self):
  with tempfile.TemporaryDirectory() as d:
   p=subprocess.run(['bash',str(SCRIPT),str(pathlib.Path(d)/'missing.pmtiles'),str(pathlib.Path(d)/'out.pmtiles')],capture_output=True,text=True)
   self.assertEqual(p.returncode,2)
   self.assertIn('existing',p.stderr)
 def test_http_rejected(self):
  p=subprocess.run(['bash',str(SCRIPT),'http://example.org/world.pmtiles','/tmp/out.pmtiles'],capture_output=True,text=True)
  self.assertEqual(p.returncode,2)
 def test_output_extension_required(self):
  with tempfile.TemporaryDirectory() as d:
   source=pathlib.Path(d)/'source.pmtiles';source.write_bytes(b'x')
   p=subprocess.run(['bash',str(SCRIPT),str(source),str(pathlib.Path(d)/'out.bin')],capture_output=True,text=True)
   self.assertEqual(p.returncode,2)
if __name__=='__main__':unittest.main()
