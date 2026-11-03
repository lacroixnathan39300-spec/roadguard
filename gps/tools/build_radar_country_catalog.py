#!/usr/bin/env python3
"""Normalize authorized national radar CSVs and build a ROADGUARD download catalog."""
import argparse
import csv
import hashlib
import json
import re
import sys
from pathlib import Path
from urllib.parse import urlsplit

MAX_BYTES = 32 * 1024 * 1024
MAX_ROWS = 100_000
RADAR = re.compile(r"radar|contrôle de vitesse|controle de vitesse|contrôle mobile|controle mobile|speed camera|^et(f|d|t|u|vm|fr|pn)$", re.I)


def require_text(value, field, limit):
    if not isinstance(value, str) or not value.strip() or len(value.strip()) > limit:
        raise ValueError(f"Champ {field} invalide")
    return value.strip()


def normalize(manifest_path: Path, output: Path):
    source = json.loads(manifest_path.read_text(encoding="utf-8"))
    if source.get("schema") != 1 or not isinstance(source.get("packs"), list):
        raise ValueError("Manifest attendu: schema 1 et liste packs")
    if not source["packs"] or len(source["packs"]) > 100:
        raise ValueError("Le manifest doit contenir entre 1 et 100 pays")
    ids, countries, entries = set(), set(), []
    output.mkdir(parents=True, exist_ok=True)

    for item in source["packs"]:
        ident = require_text(item.get("id"), "id", 50)
        name = require_text(item.get("name"), "name", 100)
        country = require_text(item.get("country"), "country", 20).upper()
        url = require_text(item.get("url"), "url", 2048)
        attribution = require_text(item.get("attribution"), "attribution", 240)
        license_name = require_text(item.get("license"), "license", 100)
        if not re.fullmatch(r"[a-z0-9_-]{2,50}", ident):
            raise ValueError(f"Identifiant invalide : {ident}")
        if not re.fullmatch(r"[A-Z]{2,3}(-[A-Z0-9]{2,8})?", country):
            raise ValueError(f"Code pays invalide : {country}")
        parts = urlsplit(url)
        if parts.scheme.lower() != "https" or not parts.netloc or parts.username or parts.password:
            raise ValueError(f"URL HTTPS publique obligatoire pour {ident}")
        if ident in ids or country in countries:
            raise ValueError(f"Pays ou identifiant en double : {country}/{ident}")
        ids.add(ident)
        countries.add(country)

        delimiter = item.get("delimiter", ";")
        columns = item.get("columns")
        if not isinstance(delimiter, str) or len(delimiter) != 1 or not isinstance(columns, dict):
            raise ValueError(f"Séparateur ou colonnes invalides : {ident}")
        if any(k not in columns or not isinstance(columns[k], str) or not columns[k]
               for k in ("id", "type", "lat", "lon")):
            raise ValueError(f"Colonnes id/type/lat/lon requises : {ident}")
        type_map = item.get("type_map", {})
        if not isinstance(type_map, dict):
            raise ValueError(f"type_map doit être un objet : {ident}")

        input_path = (manifest_path.parent / require_text(item.get("file"), "file", 300)).resolve()
        if not input_path.is_file() or input_path.stat().st_size > MAX_BYTES:
            raise ValueError(f"CSV source absent ou supérieur à 32 Mio : {input_path}")
        target = output / f"{ident}.csv"
        staged = output / f"{ident}.csv.part"
        count = 0
        try:
            with input_path.open("r", encoding="utf-8-sig", newline="") as source_file, staged.open("w", encoding="utf-8", newline="") as dest:
                reader = csv.DictReader(source_file, delimiter=delimiter)
                if not reader.fieldnames or any(columns[k] not in reader.fieldnames for k in ("id", "type", "lat", "lon")):
                    raise ValueError(f"Colonnes source manquantes : {ident}")
                writer = csv.writer(dest, delimiter=";", lineterminator="\n", quoting=csv.QUOTE_MINIMAL)
                writer.writerow(["id", "type", "lat", "lon"])
                for row_number, row in enumerate(reader, start=2):
                    if row_number > MAX_ROWS + 1:
                        raise ValueError(f"Plus de {MAX_ROWS} lignes source : {ident}")
                    row_id = (row.get(columns["id"]) or "").strip()
                    source_type = (row.get(columns["type"]) or "").strip()
                    radar_type = str(type_map.get(source_type, source_type)).strip()
                    if not row_id or len(row_id) > 100 or ";" in row_id or "\n" in row_id:
                        continue
                    if not radar_type or len(radar_type) > 80 or ";" in radar_type or "\n" in radar_type or not RADAR.search(radar_type):
                        continue
                    try:
                        lat = float((row.get(columns["lat"]) or "").strip())
                        lon = float((row.get(columns["lon"]) or "").strip())
                    except ValueError:
                        continue
                    if not (-90 <= lat <= 90 and -180 <= lon <= 180):
                        continue
                    writer.writerow([row_id, radar_type, f"{lat:.7f}", f"{lon:.7f}"])
                    count += 1
                    if count > MAX_ROWS:
                        raise ValueError(f"Plus de {MAX_ROWS} radars valides : {ident}")
        except Exception:
            staged.unlink(missing_ok=True)
            raise
        if count == 0:
            staged.unlink(missing_ok=True)
            raise ValueError(f"Aucun radar valide après normalisation : {ident}")
        size = staged.stat().st_size
        if size > MAX_BYTES:
            staged.unlink(missing_ok=True)
            raise ValueError(f"Base normalisée supérieure à 32 Mio : {ident}")
        digest = hashlib.sha256(staged.read_bytes()).hexdigest()
        staged.replace(target)
        entries.append({
            "id": ident, "name": name, "country": country,
            "format": "roadguard-radar-csv-v1", "url": url,
            "size": size, "sha256": digest,
            "attribution": attribution, "license": license_name,
            "count": count,
        })

    catalog = {"schema": 1, "packs": entries}
    catalog_path = output / "catalog.json"
    temp = output / "catalog.json.part"
    temp.write_text(json.dumps(catalog, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    temp.replace(catalog_path)
    return catalog_path, entries


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("manifest", type=Path, help="JSON source manifest with CSV column mappings")
    parser.add_argument("--out-dir", type=Path, required=True, help="Output directory to publish over HTTPS")
    args = parser.parse_args()
    try:
        catalog, entries = normalize(args.manifest.resolve(), args.out_dir.resolve())
    except Exception as exc:
        print(f"ERREUR: {exc}", file=sys.stderr)
        return 2
    print(f"Catalogue : {catalog}")
    for item in entries:
        print(f"{item['name']} ({item['country']}) : {item['count']} radars, {item['size']} octets, SHA-256 {item['sha256']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
