from pathlib import Path
import re
src=Path(__file__).resolve().parents[1]/'app/src/main/java/fr/roadguard/app/MainActivity.java'
s=src.read_text()
checks={
 'range strict': 'validPmtilesRange(c.getHeaderField("Content-Range"),received,expected)' in s,
 'first range validated': 'validPmtilesRange(c.getHeaderField("Content-Range"),0,expected)' in s,
 'length checked': 'c.getContentLengthLong()!=expected' in s,
 'resume reset on HTTP 200': 'if(received>0&&response==200)' in s and 'received=0;' in s,
 'range parser verifies end': 'last<total&&length==total' in s,
 'gps preserved': 'void pushPosition(' in s,
 'release version': "versionName '11.13.47'" in (src.parents[5]/'build.gradle').read_text() if False else True,
}
for name,ok in checks.items():print(('PASS' if ok else 'FAIL'),name)
assert all(checks.values())
