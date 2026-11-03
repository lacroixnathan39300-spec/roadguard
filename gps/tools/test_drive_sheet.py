import pathlib,unittest
S=(pathlib.Path(__file__).resolve().parents[1]/'app/src/main/java/fr/roadguard/app/MainActivity.java').read_text()
class SheetTests(unittest.TestCase):
 def test_sheet_installed_on_map(self):
  self.assertIn('installDriveSheet();',S)
  self.assertIn('frame.addView(driveSheet,sheetParams)',S)
 def test_auto_collapse(self):
  self.assertIn('setDriveSheetLevel(0)',S)
  self.assertIn('drivingDecluttered&&driveSheetLevel!=0&&!driveSheetUserOverride',S)
  self.assertIn('lastSpeedKmh>=18f',S)
  self.assertIn('lastSpeedKmh<=8f',S)
 def test_manual_swipe(self):
  self.assertIn('android.view.MotionEvent.ACTION_UP',S)
  self.assertIn('setDriveSheetLevel(delta<0?2:0)',S)
  self.assertIn('driveSheetUserOverride=true',S)
 def test_radar_map_preserved(self):
  self.assertIn('pushDangers();',S)
  self.assertIn('setDangers([',S)
if __name__=='__main__':unittest.main()
