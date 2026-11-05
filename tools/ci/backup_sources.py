#!/usr/bin/env python3
"""Sauvegarde ZIP des sources sur le serveur ROADGUARD (Cloudflare KV, morceaux de 20 Mo).
On garde les 2 dernières. Usage : backup_sources.py <zip> <numéro de build> <commit>"""
import hashlib, json, os, sys, time, urllib.request

path, run, sha = sys.argv[1], int(sys.argv[2]), sys.argv[3]
acc, token = os.environ["CLOUDFLARE_ACCOUNT_ID"], os.environ["CLOUDFLARE_API_TOKEN"]
api = f"https://api.cloudflare.com/client/v4/accounts/{acc}/storage/kv/namespaces"
CHUNK = 20 * 1024 * 1024

def call(method, url, data=None, ctype="application/octet-stream"):
    req = urllib.request.Request(url, data=data, method=method, headers={"Authorization": f"Bearer {token}", "Content-Type": ctype})
    with urllib.request.urlopen(req, timeout=300) as r:
        return r.read()

ns = next((n["id"] for n in json.loads(call("GET", api + "?per_page=100"))["result"] if n["title"].endswith("roadguard-apps")), None)
if not ns:
    print("Espace roadguard-apps absent."); sys.exit(0)
values = f"{api}/{ns}/values/"
try:
    index = json.loads(call("GET", values + "sources:index"))
except Exception:
    index = []

data = open(path, "rb").read()
parts = [data[i:i + CHUNK] for i in range(0, len(data), CHUNK)]
for i, p in enumerate(parts):
    call("PUT", values + f"sources:{run}:{i}", p)
entry = {"build": run, "commit": sha, "date": time.strftime("%Y-%m-%d %H:%M UTC", time.gmtime()),
         "size": len(data), "sha256": hashlib.sha256(data).hexdigest(), "chunks": len(parts)}
index = [e for e in index if e.get("build") != run] + [entry]
index.sort(key=lambda e: e["build"], reverse=True)
keep, drop = index[:2], index[2:]
for e in drop:
    for i in range(e.get("chunks", 0)):
        try: call("DELETE", values + f"sources:{e['build']}:{i}")
        except Exception: pass
    print("Ancienne sauvegarde supprimée :", e["build"])
call("PUT", values + "sources:index", json.dumps(keep).encode(), "application/json")
print(f"Sources build {run} sauvegardées : {len(parts)} morceaux, {len(data)} octets")
