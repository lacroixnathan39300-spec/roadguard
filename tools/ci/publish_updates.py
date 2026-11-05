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

# Une compilation plus ancienne qui finit après une plus récente ne doit pas la remplacer.
if max((old.get(a) or {}).get("build", 0) for a in ("gps", "drive", "suite")) > run:
    print(f"Build {run} plus ancienne que celle publiée : rien à faire."); sys.exit(0)
manifest = dict(old)
for app, version in (("gps", 600 + run), ("drive", run), ("suite", run), ("drivesys", run)):
    path = os.path.join(dist, f"ROADGUARD-{app.upper()}-build{run}.apk")
    if not os.path.isfile(path):
        continue
    data = open(path, "rb").read()
    parts = [data[i:i + CHUNK] for i in range(0, len(data), CHUNK)]
    for i, p in enumerate(parts):
        call("PUT", values + f"{app}:{run}:{i}", p)
    manifest[app] = {"version": version, "build": run, "size": len(data), "sha256": hashlib.sha256(data).hexdigest(), "chunks": len(parts)}
    print(f"{app} build {run} : {len(parts)} morceaux, {len(data)} octets")

# On garde au plus 2 versions par appli (l'actuelle et la précédente) ; les plus anciennes sont supprimées.
for app in ("gps", "drive", "suite", "drivesys"):
    cur, prev = manifest.get(app), old.get(app)
    if not cur:
        continue
    if prev and prev.get("build") != cur.get("build"):
        cur["previous"] = {k: v for k, v in prev.items() if k != "previous"}
    keep = {cur["build"]} | ({cur["previous"]["build"]} if cur.get("previous") else set())
    for info in [prev, (prev or {}).get("previous")]:
        if info and info.get("build") not in keep:
            for i in range(info.get("chunks", 0)):
                try:
                    call("DELETE", values + f"{app}:{info['build']}:{i}")
                except Exception:
                    pass
            print(f"{app} build {info['build']} supprimé")
# Ménage complet : tout morceau d'une version qui n'est ni l'actuelle ni la précédente est supprimé.
try:
    keys = [k["name"] for k in json.loads(call("GET", f"{api}/{ns}/keys?limit=1000"))["result"]]
    for app in ("gps", "drive", "suite", "drivesys"):
        cur = manifest.get(app) or {}
        keep = {str(cur.get("build"))} | ({str(cur["previous"]["build"])} if cur.get("previous") else set())
        for k in keys:
            parts = k.split(":")
            if len(parts) == 3 and parts[0] == app and parts[1] not in keep:
                call("DELETE", values + k)
                print("morceau ancien supprimé :", k)
except Exception as e:
    print("Ménage incomplet :", e)
call("PUT", values + "manifest", json.dumps(manifest).encode(), "application/json")
print("Manifeste :", json.dumps(manifest))
