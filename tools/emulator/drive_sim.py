#!/usr/bin/env python3
"""Essai automatique dans l'émulateur : lance l'appli, simule un trajet en voiture (NMEA avec vitesse
et cap) vers un vrai radar fixe du Jura, prend des captures et relève les plantages.

Usage : drive_sim.py <paquet> <activité> <dossier_sortie>
"""
import math, os, subprocess, sys, time, datetime

PKG, ACTIVITY, OUT = sys.argv[1], sys.argv[2], sys.argv[3]
os.makedirs(OUT, exist_ok=True)
# Radar fixe 70 km/h près de Lons-le-Saunier, contrôle le sens 205° (base embarquée rg-23221914238eedc5fb8a).
RADAR = (46.714, 5.5848)
HEADING = 205.0
SPEED_KMH = 70.0

def adb(*args, check=False):
    return subprocess.run(["adb", *args], capture_output=True, text=True, check=check).stdout

def shot(name):
    with open(os.path.join(OUT, name + ".png"), "wb") as f:
        f.write(subprocess.run(["adb", "exec-out", "screencap", "-p"], capture_output=True).stdout)

def offset(lat, lon, bearing_deg, meters):
    r = 6371000.0
    b = math.radians(bearing_deg); la = math.radians(lat); lo = math.radians(lon); d = meters / r
    la2 = math.asin(math.sin(la) * math.cos(d) + math.cos(la) * math.sin(d) * math.cos(b))
    lo2 = lo + math.atan2(math.sin(b) * math.sin(d) * math.cos(la), math.cos(d) - math.sin(la) * math.sin(la2))
    return math.degrees(la2), math.degrees(lo2)

def nmea(lat, lon, speed_kmh, course):
    now = datetime.datetime.utcnow()
    def dm(v, w):
        a = abs(v); d = int(a); m = (a - d) * 60
        return f"{d:0{w}d}{m:07.4f}"
    body = "GPRMC,{},A,{},{},{},{},{:.1f},{:.1f},{},,,A".format(
        now.strftime("%H%M%S.00"), dm(lat, 2), "N" if lat >= 0 else "S", dm(lon, 3), "E" if lon >= 0 else "W",
        speed_kmh / 1.852, course, now.strftime("%d%m%y"))
    cs = 0
    for ch in body: cs ^= ord(ch)
    return f"${body}*{cs:02X}"

adb("logcat", "-c")
for perm in ["android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION",
             "android.permission.POST_NOTIFICATIONS", "android.permission.RECORD_AUDIO"]:
    adb("shell", "pm", "grant", PKG, perm)
adb("shell", "am", "start", "-n", f"{PKG}/{ACTIVITY}")
time.sleep(12)
shot("01-demarrage")

# Trajet : de 1 500 m avant le radar jusqu'à 150 m après, à 70 km/h (une position par seconde).
start = offset(*RADAR, (HEADING + 180) % 360, 1500)
step = SPEED_KMH / 3.6
dist = 0.0
shots = {600: "02-approche-600m", 300: "03-approche-300m"}
while dist < 1650:
    lat, lon = offset(*start, HEADING, dist)
    adb("emu", "geo", "nmea", nmea(lat, lon, SPEED_KMH, HEADING))
    remaining = 1500 - dist
    for at, name in list(shots.items()):
        if remaining <= at:
            time.sleep(1.5); shot(name); del shots[at]
    time.sleep(1.0)
    dist += step
shot("04-apres-radar")

log = adb("logcat", "-d")
crash = [l for l in log.splitlines() if "FATAL EXCEPTION" in l or ("AndroidRuntime" in l and " E " in l)]
with open(os.path.join(OUT, "rapport.txt"), "w") as f:
    f.write(f"Appli : {PKG}\n")
    f.write("Plantage : " + ("OUI" if crash else "non") + "\n\n")
    f.write("\n".join(l for l in log.splitlines() if "AndroidRuntime" in l or "roadguard" in l.lower())[-20000:])
print("Plantage :", "OUI" if crash else "non")
