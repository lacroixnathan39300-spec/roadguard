#!/usr/bin/env python3
"""Read-only SQLite transport for the JVM tests, never packaged in the Android app."""
import base64,sqlite3,sys
from pathlib import Path
def encode(value):return base64.b64encode(value.encode('utf-8')).decode('ascii')
with sqlite3.connect(Path(sys.argv[1]).resolve().as_uri()+'?mode=ro',uri=True) as database:
    cursor=database.execute(sys.argv[2],sys.argv[3:])
    print('C\t'+'\t'.join(encode(column[0]) for column in cursor.description))
    for row in cursor:
        cells=[]
        for value in row:
            if value is None:cells.append('N:')
            elif isinstance(value,bytes):cells.append('B:'+base64.b64encode(value).decode('ascii'))
            else:cells.append(('I' if isinstance(value,int) else 'F' if isinstance(value,float) else 'S')+':'+encode(str(value)))
        print('R\t'+'\t'.join(cells))
