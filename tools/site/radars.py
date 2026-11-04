#!/usr/bin/env python3
"""Bases radars de l'appli → server/public/radars.json (pour la carte du site). [lat, lon, type, vitesse]."""
import csv, glob, json, os, sys

FIXED = {"fixed", "turret", "discriminant", "urban", "tunnel", "variable", "carpool", "crossing"}
out = []
for path in sorted(glob.glob(os.path.join(sys.argv[1], "radars_embedded_*.csv"))):
    for r in csv.DictReader(open(path, encoding="utf-8"), delimiter=";"):
        if r.get("status") == "historical":
            continue
        try:
            lat, lon = round(float(r["lat"]), 5), round(float(r["lon"]), 5)
        except (KeyError, ValueError):
            continue
        c, t = r.get("category", ""), r.get("type", "").lower()
        if c in FIXED or (not c and ("fixe" in t or "variable" in t)): k = "F"
        elif c == "redlight" or (not c and "feu rouge" in t): k = "R"
        elif c in ("average", "average_start", "average_end") or (not c and "moyenne" in t): k = "M"
        elif c == "mobile": k = "P"
        else: continue
        s = r.get("speed_kmh") or ""
        out.append([lat, lon, k, int(s) if s.isdigit() else 0])
json.dump(out, open(sys.argv[2], "w"), separators=(",", ":"))
print(len(out), "radars")
