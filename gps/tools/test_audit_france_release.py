import tempfile, pathlib, unittest
from audit_france_release import audit
class TestAudit(unittest.TestCase):
 def test_missing_archive_never_ready(self):
  r=audit();self.assertFalse(r['ready']);self.assertIn('archive_present',r['blockers'])
 def test_fake_archive_rejected(self):
  with tempfile.TemporaryDirectory() as d:
   p=pathlib.Path(d)/'france.pmtiles';p.write_bytes(b'not a map')
   r=audit(p,'https://example.org/france.pmtiles')
   self.assertIn('archive_and_catalog_valid',r['blockers'])
 def test_non_https_rejected(self):
  r=audit(url='http://example.org/france.pmtiles');self.assertIn('public_https_url',r['blockers'])
if __name__=='__main__':unittest.main()
