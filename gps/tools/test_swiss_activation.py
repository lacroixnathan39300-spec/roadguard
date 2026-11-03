"""Keep foreground, map data and background Swiss activation aligned."""
from pathlib import Path
import json

ROOT=Path(__file__).resolve().parents[1]
folder=ROOT/'app/src/main/java/fr/roadguard/app'
main=(folder/'MainActivity.java').read_text()
service=(folder/'BackgroundAlertService.java').read_text()
policy=(folder/'RadarSourcePolicy.java').read_text()
for source in [main,service]:
    assert 'BundledRadarCatalog.active(name->getAssets().open(name),historical,swissHistorical)' in source
    assert 'getBoolean(BundledRadarCatalog.SWISS_HISTORICAL_PREFERENCE,true)' in source
    assert 'RadarSourcePolicy.include(country,type,' in source
    assert 'RadarSourcePolicy.displayLabel(country,type)' in source
    assert 'RadarSourcePolicy.quarantined(id)' in source
    assert 'startsWith("CH"))continue;' not in source
assert 'bundledLoadedSwissCount' in main and 'bundledLoadedFranceCount' in main
assert 'Lieux mobiles signalés · Suisse' in main
assert 'affichage et alertes CH désactivés' not in main
assert 'Les données suisses restent stockées' not in main
assert 'https://overpass.osm.ch/api/interpreter' in main
assert 'lufopSwissCategoryCount(typeCounts,"Radar feu rouge")' in main
assert '"poi_warner_ch".equals(id)' in policy
manifest=json.loads((ROOT/'app/src/main/assets/radar_bundle_manifest.json').read_text())
assert manifest['default_selection']['total']==9102
assert manifest['countries']['CH']['count']==1777
assert 'active' in manifest['countries']['CH']['app_usage']
print('Swiss activation integration OK: common FR/CH loader, defaults and mobile labels; imported overlays active with legacy quarantine; UI and update endpoint coherent')
