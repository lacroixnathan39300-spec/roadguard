from pathlib import Path
root=Path(__file__).resolve().parents[1]
java=(root/'app/src/main/java/fr/roadguard/app/MainActivity.java').read_text()
assert 'int ti=rr+cc;String key=radarFilterKeys[ti];' in java
assert 'int ti=rr*3+cc' not in java
keys=list(range(10))
seen=[]
for rr in range(0,len(keys),3):
    for cc in range(3):
        if rr+cc<len(keys): seen.append(rr+cc)
assert seen==list(range(10)) and max(seen)<len(keys)
print('Filtres radar : indices des 10 catégories bornés et continus')
