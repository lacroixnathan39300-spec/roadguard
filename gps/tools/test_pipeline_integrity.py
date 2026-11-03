import pathlib,sys,tempfile,unittest
from unittest.mock import patch
sys.path.insert(0,str(pathlib.Path(__file__).resolve().parent))
import preparer_carte_jura
class PipelineIntegrityTests(unittest.TestCase):
 def test_verified_report_called_after_integration(self):
  with tempfile.TemporaryDirectory() as tmp:
   with patch.object(preparer_carte_jura,'download_checked',return_value=pathlib.Path(tmp)/'map.pbf') as dl,patch.object(preparer_carte_jura,'integrate',return_value={'secteurs_installes':2}) as it,patch.object(preparer_carte_jura,'verify',return_value={'routes':8,'points':34}) as vr,patch.object(sys,'argv',['preparer_carte_jura','--project',tmp]):
    preparer_carte_jura.main()
    self.assertEqual(dl.call_count,1)
    self.assertEqual(it.call_count,1)
    vr.assert_called_once_with(pathlib.Path(tmp))
 def test_missing_embedded_map_causes_error(self):
  with tempfile.TemporaryDirectory() as tmp:
   with patch.object(preparer_carte_jura,'download_checked',return_value=pathlib.Path(tmp)/'map.pbf'),patch.object(preparer_carte_jura,'integrate',return_value={'secteurs_installes':0}),patch.object(preparer_carte_jura,'verify',side_effect=ValueError('CARTE VIDE')),patch.object(sys,'argv',['preparer_carte_jura','--project',tmp]):
    with self.assertRaises(SystemExit) as cm:preparer_carte_jura.main()
    self.assertEqual(cm.exception.code,2)
if __name__=='__main__':unittest.main()
