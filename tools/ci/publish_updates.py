#!/usr/bin/env python3
"""Dépose les APK finaux sur le serveur ROADGUARD (Cloudflare KV, morceaux de 20 Mo) pour la mise à jour
depuis les applis. Usage : publish_updates.py <dossier dist> <numéro de build>"""
import hashlib, json, os, sys, urllib.request

dist, run = sys.argv[1], int(sys.argv[2])
acc, token = os.environ["CLOUDFLARE_ACCOUNT_ID"], os.environ["CLOUDFLARE_API_TOKEN"]
api = f"https://api.cloudflare.com/client/v4/accounts/{acc}/storage/kv/namespaces"
CHUNK = 20 * 1024 * 1024

def call(method, url, data=None, ctype="application/octet-stream"):
    req = urllib.request.Request(url, data=data, method=method, headers={"Authorization": f"Bearer {token}", "Content-Type": ctype})
    with urllib.request.urlopen(req, timeout=300) as r:
        return r.read()

ns = next((n["id"] for n in json.loads(call("GET", api + "?per_page=100"))["result"] if n["title"].endswith("roadguard-apps")), None)
if not ns:
    print("Espace roadguard-apps absent : le serveur doit d'abord être déployé."); sys.exit(0)
values = f"{api}/{ns}/values/"
try:
    old = json.loads(call("GET", values + "manifest"))
except Exception:
    old = {}

manifest = dict(old)
for app, version in (("gps", 600 + run), ("drive", run)):
    path = os.path.join(dist, f"ROADGUARD-{app.upper()}-build{run}.apk")
    if not os.path.isfile(path):
        continue
    data = open(path, "rb").read()
    parts = [data[i:i + CHUNK] for i in range(0, len(data), CHUNK)]
    for i, p in enumerate(parts):
        call("PUT", values + f"{app}:{run}:{i}", p)
    manifest[app] = {"version": version, "build": run, "size": len(data), "sha256": hashlib.sha256(data).hexdigest(), "chunks": len(parts)}
    print(f"{app} build {run} : {len(parts)} morceaux, {len(data)} octets")

call("PUT", values + "manifest", json.dumps(manifest).encode(), "application/json")

# Anciennes versions supprimées (le stockage gratuit est limité).
for app, info in old.items():
    if manifest.get(app, {}).get("build") != info.get("build"):
        for i in range(info.get("chunks", 0)):
            try:
                call("DELETE", values + f"{app}:{info['build']}:{i}")
            except Exception:
                pass
print("Manifeste :", json.dumps(manifest))
