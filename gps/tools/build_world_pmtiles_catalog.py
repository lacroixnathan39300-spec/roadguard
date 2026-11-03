#!/usr/bin/env python3
"""Build a verified ROADGUARD schema-1 catalog from existing PMTiles packs.
No map data is fetched and no URL is fabricated. Host all files at --base-url first.
Declare full-country packs explicitly with --country-pack ID=COUNTRY, after checking
their coverage. Unmarked packs are treated as regions by the app.
Usage: python tools/build_world_pmtiles_catalog.py --base-url https://YOUR-HOST/maps/ --country-pack france_openmaptiles=france --out catalog.json france-openmaptiles.pmtiles
"""
import argparse,hashlib,json,pathlib,re,urllib.parse
p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--base-url',required=True)
p.add_argument('--out',default='catalog.json')
p.add_argument('--country-pack',action='append',default=[],metavar='ID=COUNTRY',help='Explicitly mark a pack as a full country after coverage verification; repeat as needed')
p.add_argument('packs',nargs='+')
a=p.parse_args()
full_countries={}
for mapping in a.country_pack:
 try: ident,country=mapping.lower().split('=',1)
 except ValueError:p.error('--country-pack must be ID=COUNTRY')
 ident=ident.replace('-','_')
 if not re.fullmatch(r'[a-z0-9_]{2,50}',ident) or not re.fullmatch(r'[a-z0-9_]{2,50}',country):p.error('Invalid --country-pack mapping: '+mapping)
 if ident in full_countries:p.error('Duplicate --country-pack ID: '+ident)
 full_countries[ident]=country
u=urllib.parse.urlparse(a.base_url)
if u.scheme!='https' or not u.netloc or u.username or u.password or not a.base_url.endswith('/'):
 p.error('--base-url must be an HTTPS directory ending in /')
items=[]
for name in a.packs:
 f=pathlib.Path(name)
 if not f.is_file() or f.suffix.lower()!='.pmtiles':p.error('Missing PMTiles file: '+name)
 ident=f.stem.lower().replace('-','_')
 if not re.fullmatch(r'[a-z0-9_]{2,50}',ident):p.error('Invalid pack id: '+ident)
 with f.open('rb') as h:
  header=h.read(127)
  if len(header)!=127 or header[:7]!=b'PMTiles' or header[7]!=3:p.error('Not PMTiles v3: '+name)
  h.seek(0);sha=hashlib.file_digest(h,'sha256').hexdigest()
 if f.stat().st_size>600*1024*1024:p.error('Pack over app 600 MiB limit: '+name)
 # Keep country and regional packs distinct: country=france alone must never
 # make a Jura or other regional extract eligible for the one-click country action.
 country=full_countries.get(ident)
 if country is None and '_' not in ident:country=ident
 item=dict(id=ident,name=f.stem.replace('_',' ').title(),country=country or ident.split('_')[0],scope='country' if country else 'region',format='pmtiles',url=urllib.parse.urljoin(a.base_url,urllib.parse.quote(f.name)),size=f.stat().st_size,sha256=sha)
 items.append(item)
unknown=set(full_countries)-{x['id'] for x in items}
if unknown:p.error('Declared full-country pack not supplied: '+', '.join(sorted(unknown)))
ids=[x['id'] for x in items]
if len(ids)!=len(set(ids)):p.error('Duplicate country/region IDs')
if len(items)>100:p.error('App supports max 100 catalog items')
pathlib.Path(a.out).write_text(json.dumps(dict(schema=1,packs=items),ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(f'Created {a.out} with {len(items)} verified PMTiles packs; upload packs and catalog to your HTTPS host.')
