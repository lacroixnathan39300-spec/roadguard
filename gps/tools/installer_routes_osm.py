#!/usr/bin/env python3
"""Installe un GeoJSON de routes deja converti dans les assets Android.
Usage: python tools/installer_routes_osm.py chemin/vers/routes.geojson
Aucune donnee fictive n'est ajoutee au projet.
"""
import json, pathlib, shutil, sys
if len(sys.argv)!=2:
    sys.exit("Usage : python tools/installer_routes_osm.py routes.geojson")
source=pathlib.Path(sys.argv[1]); target=pathlib.Path(__file__).resolve().parents[1]/"app/src/main/assets/offline-roads.geojson"
raw=source.read_bytes()
if len(raw)>350000:sys.exit("Fichier trop gros : 350 Ko maximum (limite du chargeur actuel)")
data=json.loads(raw)
if data.get("type")!="FeatureCollection" or not isinstance(data.get("features"),list) or not data["features"]:
    sys.exit("FeatureCollection non vide attendu")
lines=[f for f in data["features"] if isinstance(f,dict) and isinstance(f.get("geometry"),dict) and f["geometry"].get("type") in ("LineString","MultiLineString")]
if not lines:sys.exit("Aucune route LineString/MultiLineString")
target.parent.mkdir(parents=True,exist_ok=True)
shutil.copyfile(source,target)
print("Routes installees dans les assets :",target,"/",len(lines),"objets")
