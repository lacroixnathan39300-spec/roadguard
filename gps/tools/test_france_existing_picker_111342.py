from pathlib import Path
import unittest
SRC=Path(__file__).resolve().parents[1]/'app/src/main/java/fr/roadguard/app/MainActivity.java'
class TestExistingMapPicker(unittest.TestCase):
 def test_france_dialog_offers_local_file_picker(self):
  s=SRC.read_text()
  section=s[s.index('void downloadFranceOneTap()'):s.index('void downloadPublicFranceCandidate()')]
  self.assertIn('Rechercher une carte existante',section)
  self.assertIn('openExistingPmtiles()',section)
  self.assertNotIn('setPositiveButton("Configurer la source"',section)
 def test_picker_does_not_hide_unknown_mime_pmtiles(self):
  s=SRC.read_text()
  section=s[s.index('void openExistingPmtiles()'):s.index('void importExistingPmtiles(')]
  self.assertIn('Intent.ACTION_OPEN_DOCUMENT',section)
  self.assertIn('intent.setType("*/*")',section)
  self.assertNotIn('EXTRA_MIME_TYPES',section)
 def test_existing_import_keeps_validation(self):
  s=SRC.read_text()
  section=s[s.index('void importExistingPmtiles('):s.index('void openExistingPmtiles(')] if s.index('void importExistingPmtiles(')<s.index('void openExistingPmtiles(') else s[s.index('void importExistingPmtiles('):s.index('void downloadPmtilesPack(')]
  self.assertIn('PmtilesReader',section)
if __name__=='__main__': unittest.main()
