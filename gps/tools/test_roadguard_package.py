#!/usr/bin/env python3
"""Checks static Android project packaging, no Android SDK needed."""
import pathlib,re,unittest,json
ROOT=pathlib.Path(__file__).resolve().parents[1]
class PackagingTests(unittest.TestCase):
 def test_version(self):
  gradle=(ROOT/'app/build.gradle').read_text()
  self.assertRegex(gradle,r"versionCode\s+365\b")
  self.assertRegex(gradle,r"versionName\s+'11\.14\.165'")
 def test_map_assets(self):
  assets=ROOT/'app/src/main/assets'
  for name in ('map.html','map_offline.html','map_maplibre.html'):
   self.assertTrue((assets/name).is_file(),name)
 def test_sector_manifest_optional(self):
  sector=ROOT/'app/src/main/assets/osm-sectors'
  manifest=sector/'manifest.json'
  if not manifest.exists():return
  data=json.loads(manifest.read_text())
  self.assertEqual(data['format'],'roadguard-osm-sectors-v1')
  for name in data['sectors']:
   self.assertTrue((sector/name).is_file(),name)
if __name__=='__main__':unittest.main()
