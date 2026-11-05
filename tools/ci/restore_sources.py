#!/usr/bin/env python3
"""Récupère la dernière sauvegarde des sources depuis le serveur ROADGUARD (Cloudflare KV).
Usage : CLOUDFLARE_ACCOUNT_ID=… CLOUDFLARE_API_TOKEN=… restore_sources.py [fichier.zip]"""
import hashlib, json, os, sys, urllib.request

acc, token = os.environ["CLOUDFLARE_ACCOUNT_ID"], os.environ["CLOUDFLARE_API_TOKEN"]
api = f"https://api.cloudflare.com/client/v4/accounts/{acc}/storage/kv/namespaces"
out = sys.argv[1] if len(sys.argv) > 1 else "ROADGUARD-sources.zip"

def get(url):
    req = urllib.request.Request(url, headers={"Authorization": f"Bearer {token}"})
    with urllib.request.urlopen(req, timeout=300) as r:
        return r.read()

ns = next(n["id"] for n in json.loads(get(api + "?per_page=100"))["result"] if n["title"].endswith("roadguard-apps"))
values = f"{api}/{ns}/values/"
e = json.loads(get(values + "sources:index"))[0]
data = b"".join(get(values + f"sources:{e['build']}:{i}") for i in range(e["chunks"]))
assert hashlib.sha256(data).hexdigest() == e["sha256"], "Sauvegarde abîmée"
open(out, "wb").write(data)
print(f"Sources build {e['build']} ({e['date']}, commit {e['commit'][:7]}) → {out}")
