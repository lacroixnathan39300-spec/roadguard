from pathlib import Path
import xml.etree.ElementTree as ET
r=Path(__file__).resolve().parents[1];a="{http://schemas.android.com/apk/res/android}"
m=ET.parse(r/"app/src/auto/AndroidManifest.xml").getroot()
permissions={x.get(a+"name") for x in m.findall("uses-permission")}
assert {"androidx.car.app.NAVIGATION_TEMPLATES","androidx.car.app.ACCESS_SURFACE"} <= permissions
services={x.get(a+"name"):x for x in m.find("application").findall("service")}
assert services[".RoadguardCarAppService"].get(a+"exported")=="true"
assert services[".CarLocationService"].get(a+"exported")=="false"
assert services[".CarLocationService"].get(a+"foregroundServiceType")=="location"
source=(r/"app/src/auto/java/fr/roadguard/app/RoadguardCarAppService.java").read_text()
assert "ALLOW_ALL_HOSTS_VALIDATOR" not in source and "roadguard_allowed_hosts" in source
car=(r/"app/src/auto/java/fr/roadguard/app/RoadguardCarScreen.java").read_text()
assert "navigationStarted()" in car and "navigationEnded()" in car and "updateTrip(" in car
assert "AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK" in car and "USAGE_ASSISTANCE_NAVIGATION_GUIDANCE" in car
assert "onAutoDriveEnabled()" in car and "if(closed||simulation||" in car
assert "postDelayed(this,2000)" in car and "now-lastReports>=120000" in car
assert "requestBytes" not in car and "credential" not in car and "POST" not in car
assert "io.shutdownNow()" in car and "map.close()" in car and "removeUpdates(listener)" in car
cache=(r/"app/src/main/java/fr/roadguard/app/CommunitySync.java").read_text()
assert "CommunityFiles.write(" in cache and "CommunityFiles.read(" in cache
print("PASS Android Auto static contract: manifest, host validation, navigation metadata, audio focus, read-only reports and lifecycle release")
