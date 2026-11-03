import json, pathlib, sys, tempfile, unittest
sys.path.insert(0,str(pathlib.Path(__file__).resolve().parents[1]/'tools'))
from execute_versatiles_plan import execute,prepare_jobs
from plan_versatiles_regions import build,DEFAULT_SOURCE
class TestRunner(unittest.TestCase):
 def setUp(self):
  self.job=build([{'id':'jura_01','bbox':[5.5,46.5,6.5,47.5]}],DEFAULT_SOURCE,10)
  self.plan={'regions':self.job,'source':DEFAULT_SOURCE}
 def test_dry_run(self):
  with tempfile.TemporaryDirectory() as d:
   self.assertEqual(execute(self.plan,d)[0]['status'],'DRY_RUN')
   self.assertFalse(pathlib.Path(d,'jura_01.pmtiles').exists())
 def test_reject_injected_plan(self):
  self.plan['regions'][0]['id']='../../oops'
  with self.assertRaises(ValueError):prepare_jobs(self.plan)
 def test_fake_extraction(self):
  with tempfile.TemporaryDirectory() as d:
   script=pathlib.Path(d,'fake.py')
   script.write_text('#!/usr/bin/env python3\nimport pathlib,sys\npathlib.Path(sys.argv[-1]).write_bytes(b"PMTiles"+bytes(200))\n')
   script.chmod(0o755)
   result=execute(self.plan,pathlib.Path(d,'out'),str(script),True)
   self.assertEqual(result[0]['status'],'EXTRACTED_UNHOSTED')
   self.assertEqual(len(result[0]['sha256']),64)
   self.assertEqual(execute(self.plan,pathlib.Path(d,'out'),str(script),True)[0]['status'],'EXISTING_INSPECTED_UNHOSTED')
 def test_corrupt_existing_not_silently_skipped(self):
  with tempfile.TemporaryDirectory() as d:
   out=pathlib.Path(d,'out');out.mkdir()
   (out/'jura_01.pmtiles').write_bytes(b'bad')
   result=execute(self.plan,out,'missing-binary',True)[0]
   self.assertEqual(result['status'],'INVALID_EXISTING_NEEDS_MANUAL_REPAIR')
   self.assertEqual((out/'jura_01.pmtiles').read_bytes(),b'bad')
 def test_existing_inspection_has_digest(self):
  with tempfile.TemporaryDirectory() as d:
   out=pathlib.Path(d,'out');out.mkdir()
   (out/'jura_01.pmtiles').write_bytes(b'PMTiles'+bytes(200))
   result=execute(self.plan,out,'missing-binary',True)[0]
   self.assertEqual(result['status'],'EXISTING_INSPECTED_UNHOSTED')
   self.assertEqual(len(result['sha256']),64)
 def test_failure_cleans_partial(self):
  with tempfile.TemporaryDirectory() as d:
   script=pathlib.Path(d,'bad.py')
   script.write_text('#!/usr/bin/env python3\nimport pathlib,sys\npathlib.Path(sys.argv[-1]).write_bytes(b"bad")\n')
   script.chmod(0o755)
   self.assertEqual(execute(self.plan,pathlib.Path(d,'out'),str(script),True)[0]['status'],'FAILED')
   self.assertFalse(list(pathlib.Path(d,'out').glob('*.partial')))
if __name__=='__main__':unittest.main()
