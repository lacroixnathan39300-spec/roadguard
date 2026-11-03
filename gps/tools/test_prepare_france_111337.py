import pathlib, subprocess, tempfile, unittest
SCRIPT=pathlib.Path(__file__).with_name('prepare_france_pmtiles.sh')
class TestExtractionLimits(unittest.TestCase):
 def test_invalid_zoom_rejected(self):
  with tempfile.TemporaryDirectory() as d:
   src=pathlib.Path(d)/'src.pmtiles';src.write_bytes(b'x')
   for zoom in ('16','-1','bad'):
    r=subprocess.run(['bash',str(SCRIPT),str(src),str(pathlib.Path(d)/'out.pmtiles'),zoom],capture_output=True,text=True)
    self.assertEqual(r.returncode,2)
 def test_default_zoom_is_bounded_and_failed_extraction_preserves_old_pack(self):
  with tempfile.TemporaryDirectory() as d:
   root=pathlib.Path(d);src=root/'src.pmtiles';src.write_bytes(b'x')
   dest=root/'out.pmtiles';dest.write_bytes(b'old valid map')
   fake=root/'pmtiles';fake.write_text('#!/bin/sh\nprintf "%s\\n" "$@" >> "$TRACE"\nexit 4\n');fake.chmod(0o755)
   import os
   env=os.environ.copy();env['PATH']=d+':'+env['PATH'];env['TRACE']=str(root/'trace')
   r=subprocess.run(['bash',str(SCRIPT),str(src),str(dest)],env=env,capture_output=True,text=True)
   self.assertEqual(r.returncode,4)
   self.assertEqual(dest.read_bytes(),b'old valid map')
   self.assertIn('--maxzoom=12',(root/'trace').read_text())
   self.assertFalse(list(root.glob('*.partial.*')))
if __name__=='__main__':unittest.main()
