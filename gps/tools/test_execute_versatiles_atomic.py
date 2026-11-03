#!/usr/bin/env python3
"""Offline integration tests using a fake VersaTiles executable (no map data)."""
import json, pathlib, tempfile, unittest
from execute_versatiles_plan import execute

class TestExtraction(unittest.TestCase):
    def setUp(self):
        self.tmp=tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root=pathlib.Path(self.tmp.name)
        self.plan={'source':'https://download.versatiles.org/osm.versatiles','regions':[{'id':'jura','bbox':[5.0,46.5,6.5,47.8], 'command':'versatiles convert --max-zoom 14'}]}
        self.binary=self.root/'fake-versatiles'
    def fake(self,success):
        self.binary.write_text('#!/usr/bin/env python3\nimport pathlib,sys\np=pathlib.Path(sys.argv[-1])\nassert p.suffix==".pmtiles", "output format cannot be inferred"\n'+('p.write_bytes(b"PMTiles"+bytes(160))\n' if success else 'p.write_bytes(b"bad")\nsys.exit(2)\n'))
        self.binary.chmod(0o755)
    def test_success_and_skip(self):
        self.fake(True)
        out=self.root/'packs'
        first=execute(self.plan,out,str(self.binary),True)
        self.assertEqual(first[0]['status'],'EXTRACTED_UNHOSTED')
        self.assertEqual(len(first[0]['sha256']),64)
        self.assertFalse((out/'jura.incomplete.pmtiles').exists())
        again=execute(self.plan,out,str(self.binary),True)
        self.assertEqual(again[0]['status'],'EXISTING_INSPECTED_UNHOSTED')
    def test_failure_cleans_partial(self):
        self.fake(False)
        out=self.root/'packs'
        res=execute(self.plan,out,str(self.binary),True)
        self.assertEqual(res[0]['status'],'FAILED')
        self.assertFalse((out/'jura.incomplete.pmtiles').exists())
        self.assertFalse((out/'jura.pmtiles').exists())

if __name__=='__main__':unittest.main()
