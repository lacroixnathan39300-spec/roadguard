#!/usr/bin/env python3
"""Focused lot 288 JS checks. Actual packaged scripts; renderer and DOM adapters, no Android."""
from pathlib import Path
import os,subprocess
root=Path(__file__).resolve().parents[1]
node=os.environ.get('CODEX_PRIMARY_RUNTIME_NODE','node');logs=[]
try:
 for name in ['test_marker_duplicates.js','test_offline_regressions.js','test_radar_icons.js','test_map_startup.js','test_map_view_state.js','test_map_native_network.js','test_vector_camera.js','roadguard_map_syntax_check.js']:
  p=subprocess.run([node,str(root/'tools'/name)],cwd=root,capture_output=True,text=True,timeout=60);out=p.stdout+p.stderr;logs.append(name+'\n'+out+'\nexit='+str(p.returncode));print(out,flush=True)
  if p.returncode:raise SystemExit(p.returncode)
finally:(root/'verification/ui-regressions-11.14.290.log').write_text('\n\n'.join(logs)+'\n')
