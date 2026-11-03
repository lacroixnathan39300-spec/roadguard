"""Regression: an EXIT trap must retain rollback, not just cleanup."""
import pathlib, unittest
P=pathlib.Path(__file__).with_name('prepare_france_pmtiles.sh')
class RollbackTrapTests(unittest.TestCase):
    def test_rollback_remains_registered(self):
        script=P.read_text()
        self.assertIn("trap 'rollback; cleanup' EXIT", script)
        self.assertNotIn('trap cleanup EXIT', script)
    def test_no_rollback_directory_left_on_success(self):
        script=P.read_text()
        self.assertIn('rm -rf "$BACKUP"',script)
        self.assertIn('trap - EXIT',script)
if __name__=='__main__': unittest.main()
