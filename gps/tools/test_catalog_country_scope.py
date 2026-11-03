#!/usr/bin/env python3
"""Exercise catalog country-versus-region metadata without external map data."""
import json
import pathlib
import subprocess
import sys
import tempfile
import unittest

ROOT=pathlib.Path(__file__).resolve().parents[1]
SCRIPT=ROOT/'tools/build_world_pmtiles_catalog.py'

def make_pmtiles(path):
    # Minimal header signature for catalog metadata tests; no tile data consumed.
    path.write_bytes(b'PMTiles'+bytes([3])+bytes(119))

class CatalogScopeTests(unittest.TestCase):
    def test_explicit_full_country_and_region_are_distinct(self):
        with tempfile.TemporaryDirectory() as td:
            d=pathlib.Path(td)
            full=d/'france-openmaptiles.pmtiles'; region=d/'france-jura.pmtiles'
            make_pmtiles(full); make_pmtiles(region)
            out=d/'catalog.json'
            subprocess.run([sys.executable,str(SCRIPT),'--base-url','https://maps.example/','--country-pack','france_openmaptiles=france','--out',str(out),str(full),str(region)],check=True,capture_output=True,text=True)
            items={x['id']:x for x in json.loads(out.read_text())['packs']}
            self.assertEqual(items['france_openmaptiles']['country'],'france')
            self.assertEqual(items['france_openmaptiles']['scope'],'country')
            self.assertEqual(items['france_jura']['scope'],'region')
    def test_missing_declared_pack_is_rejected(self):
        with tempfile.TemporaryDirectory() as td:
            p=subprocess.run([sys.executable,str(SCRIPT),'--base-url','https://maps.example/','--country-pack','france_openmaptiles=france','--out',str(pathlib.Path(td)/'catalog.json'),str(ROOT/'app/src/main/assets/maps/empty.pmtiles')],capture_output=True,text=True)
            self.assertNotEqual(p.returncode,0)

if __name__=='__main__':unittest.main()
