"""Regression checks for the ROADGUARD 11.14.38 UI-only changes."""
import pathlib,re,unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]
JAVA=(ROOT/'app/src/main/java/fr/roadguard/app/MainActivity.java').read_text()
HTML=(ROOT/'app/src/main/assets/map.html').read_text()
GRADLE=(ROOT/'app/build.gradle').read_text()
class ReleaseTests(unittest.TestCase):
 def test_version(self):
  self.assertIn("versionCode 238; versionName '11.14.38'",GRADLE)
 def test_theme_toggle_and_persistence(self):
  for token in ('high_contrast_map','setRoadguardContrast','setRoadguardNight','recenter.setOnLongClickListener'):
   self.assertIn(token,JAVA)
 def test_raster_only_contrast(self):
  self.assertIn('body.rg-night.rg-contrast .leaflet-tile-pane img.leaflet-tile',HTML)
  self.assertIn('body:not(.rg-night).rg-contrast .leaflet-tile-pane img.leaflet-tile',HTML)
  self.assertIn('function setRoadguardContrast(enabled)',HTML)
 def test_controls_and_gesture(self):
  self.assertIn('final int railGap=shortScreen?1:3',JAVA)
  self.assertIn('driveSheetHandle.setOnTouchListener',JAVA)
  self.assertIn('Math.abs(delta)>pxDp(28)',JAVA)
  self.assertEqual(JAVA.count('actionRail.addView('),5)
 def test_legacy_camera_preserved(self):
  self.assertIn('void applyMapTheme()',JAVA)
  self.assertIn('void pushPosition()',JAVA)
if __name__=='__main__':unittest.main()
