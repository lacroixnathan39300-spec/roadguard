from pathlib import Path
import re
s=Path('app/src/main/java/fr/roadguard/app/MainActivity.java').read_text(encoding='utf-8')
assert 'addRoadLine(line,parsed,count,4000);' in s
assert 'addRoadLine(coordinates,next,count,12000);' in s
assert 'addRoadLine(line,next,count,12000);' in s
assert 'if(count[0]>=maxPoints)' in s
assert 'if(count[0]>=4000)' not in s
print('OK: routes personnelles 4000 points, secteurs GPS 12000 points')
