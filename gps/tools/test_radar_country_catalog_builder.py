"""Exercises the publisher helper with temporary, explicitly synthetic data."""
import csv
import hashlib
import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
SCRIPT=ROOT/'tools/build_radar_country_catalog.py'


class RadarCatalogBuilderTests(unittest.TestCase):
    def test_normalizes_records_and_emits_mobile_catalog_checksum(self):
        with tempfile.TemporaryDirectory() as temp:
            root=Path(temp)
            source=root/'source.csv'
            source.write_text('id;kind;latitude;longitude\nA1;FIXED;46.50001;5.50001\nA2;UNKNOWN;46.6;5.6\nA3;FIXED;91;5.6\n',encoding='utf-8')
            manifest={"schema":1,"packs":[{
                "id":"sample-country","name":"Pays test","country":"XY",
                "url":"https://data.example.test/radars.csv","attribution":"Source fictive, test seulement",
                "license":"Licence de test","file":"source.csv","delimiter":";",
                "columns":{"id":"id","type":"kind","lat":"latitude","lon":"longitude"},
                "type_map":{"FIXED":"Radar fixe"}
            }]}
            manifest_path=root/'manifest.json'
            manifest_path.write_text(json.dumps(manifest),encoding='utf-8')
            output=root/'publish'
            subprocess.run([sys.executable,str(SCRIPT),str(manifest_path),'--out-dir',str(output)],check=True,capture_output=True,text=True)
            catalog=json.loads((output/'catalog.json').read_text())
            pack=catalog['packs'][0]
            data=(output/'sample-country.csv').read_bytes()
            self.assertEqual(catalog['schema'],1)
            self.assertEqual(pack['count'],1)
            self.assertEqual(pack['format'],'roadguard-radar-csv-v1')
            self.assertEqual(pack['size'],len(data))
            self.assertEqual(pack['sha256'],hashlib.sha256(data).hexdigest())
            self.assertEqual(data.decode().splitlines(),['id;type;lat;lon','A1;Radar fixe;46.5000100;5.5000100'])

    def test_rejects_non_https_data_links(self):
        with tempfile.TemporaryDirectory() as temp:
            root=Path(temp)
            (root/'source.csv').write_text('id;type;lat;lon\n1;Radar fixe;46;5\n',encoding='utf-8')
            manifest={"schema":1,"packs":[{"id":"test-country","name":"Test","country":"XY","url":"http://bad.example/r.csv","attribution":"Source","license":"Licence","file":"source.csv","columns":{"id":"id","type":"type","lat":"lat","lon":"lon"}}]}
            path=root/'manifest.json';path.write_text(json.dumps(manifest),encoding='utf-8')
            result=subprocess.run([sys.executable,str(SCRIPT),str(path),'--out-dir',str(root/'out')],capture_output=True,text=True)
            self.assertNotEqual(result.returncode,0)
            self.assertIn('HTTPS',result.stderr)


if __name__=='__main__':
    unittest.main()
