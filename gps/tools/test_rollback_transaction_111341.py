"""Simulate failure during installation and verify all previous files survive."""
import os, pathlib, subprocess, tempfile, unittest
SCRIPT=pathlib.Path(__file__).with_name('prepare_france_pmtiles.sh')
class TransactionTest(unittest.TestCase):
 def test_failed_commit_restores_all_three_files(self):
  with tempfile.TemporaryDirectory() as td:
   d=pathlib.Path(td); b=d/'bin'; b.mkdir(); target=d/'france.pmtiles'; source=d/'planet.pmtiles'; source.write_bytes(b'x')
   for suffix in ('','.header.json','.sha256'):(d/('france.pmtiles'+suffix)).write_bytes(b'ORIGINAL'+suffix.encode())
   pm=b/'pmtiles'; pm.write_text('''#!/usr/bin/env python3
import sys,struct,json
cmd=sys.argv[1]
if cmd=='extract':
 p=sys.argv[3]; h=bytearray(127); h[:7]=b'PMTiles'; h[7]=3; h[99]=1
 struct.pack_into('<iiii',h,102,-56000000,412000000,98000000,513000000)
 open(p,'wb').write(h)
elif cmd=='verify': pass
elif cmd=='show': print('{}')
else: sys.exit(1)
''');pm.chmod(0o755)
   mv=b/'mv';mv.write_text('''#!/bin/sh
case "$1 $2 $3" in *partial.*.header.json*france.pmtiles.header.json*) exit 43;; esac
exec /usr/bin/mv "$@"
''');mv.chmod(0o755)
   env=dict(os.environ,PATH=str(b)+os.pathsep+os.environ['PATH'])
   r=subprocess.run(['bash',str(SCRIPT),str(source),str(target)],env=env,capture_output=True,text=True)
   self.assertNotEqual(r.returncode,0,(r.stdout,r.stderr))
   for suffix in ('','.header.json','.sha256'):
    self.assertEqual((d/('france.pmtiles'+suffix)).read_bytes(),b'ORIGINAL'+suffix.encode())
   self.assertEqual(list(d.glob('*.rollback.*')),[])
   self.assertEqual(list(d.glob('*.partial.*')),[])
if __name__=='__main__':unittest.main()
