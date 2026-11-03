"""Regression checks for subpixel pan compensation in the rendered map."""
from pathlib import Path

s=(Path(__file__).resolve().parents[1]/'app/src/main/assets/map.html').read_text()
checks=(
 'const rgSubpixelCamera',
 'function rgApplyCameraTransform()',
 'translate3d(',
 'const transform=',
 'rgSubpixelCamera.x=Math.max(-10',
 'rgApplyCameraTransform();',
 'const alpha=1-Math.exp(-dt/115)',
 'function rgVisualCamera(point,now)',
 'rgHeadingCamera.enabled?rgHeadingCamera.angle*Math.PI/180:0',
 'if(Math.hypot(dx,dy)>9)',
)
for needle in checks:
 assert needle in s,needle
assert '>=0.85)map.panTo' not in s
print(f'{len(checks)+1} current subpixel camera checks passed')
