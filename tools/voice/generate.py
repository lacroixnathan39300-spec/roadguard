#!/usr/bin/env python3
"""Génère la voix réaliste (Piper, voix « Jessica » fr_FR-upmc) pour toutes les phrases de script.json.

Clés produites (fichiers <clé>.mp3) :
  t{ton}.radar.{i}          phrase d'intro avant un radar
  r.{TYPE}.{v|x}            « Radar fixe, limité à quatre-vingts » (x : sans limite)
  t{ton}.{limit|over|alarm}.{i}.{v}   phrase complète avec la limitation à la fin
Usage : generate.py script.json modele.onnx dossier_sortie
"""
import json, subprocess, sys, os, concurrent.futures as cf

script, model, out = sys.argv[1], sys.argv[2], sys.argv[3]
os.makedirs(out, exist_ok=True)
S = json.load(open(script, encoding="utf-8"))
nums = S["numbers"]
SPEED = {"0": 1.0, "1": 1.1, "2": 1.15, "3": 1.2}  # plus lent = plus posé, plus sensuel

jobs = []
for t, groups in S["tones"].items():
    for i, p in enumerate(groups["radar"]):
        jobs.append((f"t{t}.radar.{i}", p, SPEED[t]))
    for g in ("limit", "over", "alarm"):
        for i, p in enumerate(groups[g]):
            for v, w in nums.items():
                jobs.append((f"t{t}.{g}.{i}.{v}", f"{p} {w}.", SPEED[t]))
for k, label in S["kinds"].items():
    jobs.append((f"r.{k}.x", f"{label}.", 1.1))
    for v, w in nums.items():
        jobs.append((f"r.{k}.{v}", f"{label}, {S['limited']} {w}.", 1.1))

def run(job):
    key, text, speed = job
    wav = os.path.join(out, key + ".wav")
    subprocess.run(["piper", "--model", model, "--speaker", "0", "--length_scale", str(speed),
                    "--sentence_silence", "0.15", "--output_file", wav], input=text.encode(), check=True,
                   stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    # MP3 mono 22 kHz : lu partout (Android 8 compris), petit.
    subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-i", wav, "-af", "silenceremove=start_periods=1:start_threshold=-45dB,areverse,silenceremove=start_periods=1:start_threshold=-45dB,areverse",
                    "-ac", "1", "-ar", "22050", "-b:a", "40k", os.path.join(out, key + ".mp3")], check=True)
    os.remove(wav)

with cf.ThreadPoolExecutor(4) as ex:
    list(ex.map(run, jobs))
print(len(jobs), "phrases générées")
