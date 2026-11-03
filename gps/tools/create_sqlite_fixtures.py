#!/usr/bin/env python3
"""Synthetic SQLite fixtures, never radar data shipped as an inventory."""
import sqlite3,sys
from pathlib import Path
folder=Path(sys.argv[1]);folder.mkdir(parents=True,exist_ok=True)
def create(name,sql,rows=(),insert='INSERT INTO cameras VALUES(?,?,?,?)'):
    with sqlite3.connect(folder/(name+'.sqlite')) as database:
        database.executescript(sql)
        if rows:database.executemany(insert,rows)
schema='CREATE TABLE cameras(id INTEGER PRIMARY KEY,lat REAL NOT NULL,lon REAL NOT NULL,type TEXT NOT NULL);'
create('valid',schema,[(1,0.123456,-150.87654,'speed_camera'),(2,0.123456,-150.87654,'red_light'),(3,0.234567,-150.87654,'unmapped <kind>; raw\ntext')])
create('extra',schema.replace('type TEXT NOT NULL','type TEXT NOT NULL,note TEXT,speed INTEGER'),[(4,0.4567,-150.87654,'red_light','keep unknown <field>;\nline',90)],'INSERT INTO cameras VALUES(?,?,?,?,?,?)')
create('missing','CREATE TABLE cameras(id INTEGER,lat REAL,lon REAL);')
create('wrong_table','CREATE TABLE other(id INTEGER);')
create('view','CREATE TABLE original(id INTEGER,lat REAL,lon REAL,type TEXT);CREATE VIEW cameras AS SELECT * FROM original;')
create('empty',schema)
create('bad_coordinates',schema,[(1,91,-150,'speed_camera')])
create('duplicate','CREATE TABLE cameras(id INTEGER,lat REAL,lon REAL,type TEXT);',[(1,1,1,'speed_camera'),(1,2,2,'speed_camera')])
create('text_coordinates','CREATE TABLE cameras(id INTEGER,lat TEXT,lon REAL,type TEXT);',[(1,'not a number',1,'speed_camera')])
create('null_type','CREATE TABLE cameras(id INTEGER,lat REAL,lon REAL,type TEXT);',[(1,1,1,None)])
create('huge_type',schema,[(1,1,1,'x'*2049)])
create('blob','CREATE TABLE cameras(id INTEGER,lat REAL,lon REAL,type TEXT,extra BLOB);',[(1,1,1,'speed_camera',b'not permitted')],'INSERT INTO cameras VALUES(?,?,?,?,?)')
create('large',schema,[(i,1,1,'speed_camera') for i in range(100001)])
(folder/'invalid.sqlite').write_bytes(b'NOT SQLITE'+b'\0'*600)
print('Created 14 synthetic SQLite fixtures using real sqlite3')
