import pathlib,subprocess,sys
s=pathlib.Path(__file__).with_name('build_pilot_jura.py')
assert 'pmtiles' in s.read_text() and 'jura_pilot' in s.read_text()
r=subprocess.run([sys.executable,str(s),'--help'],capture_output=True,text=True)
assert r.returncode==0 and '--base-url' in r.stdout
r=subprocess.run([sys.executable,str(s),'--source','http://invalid/p.pmtiles'],capture_output=True,text=True)
assert r.returncode!=0 and 'HTTPS' in r.stderr
print('Jura pilot CLI validation: OK')
