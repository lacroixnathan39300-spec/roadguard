from pathlib import Path
import json, re
ROOT=Path(__file__).resolve().parents[1]
JAVA=(ROOT/'app/src/main/java/fr/roadguard/app/MainActivity.java').read_text()
GRADLE=(ROOT/'app/build.gradle').read_text()
DATA=json.loads((ROOT/'app/src/main/assets/roadguard_progress.json').read_text())
VERSION=DATA['version']
assert VERSION=='11.14.181'
assert re.search(r"versionName '"+re.escape(VERSION)+r"'",GRADLE)
assert 'versionCode 381' in GRADLE
assert 'double meters=Double.isFinite(lastLat)&&Double.isFinite(lastLon)?geoMeters(lastLat,lastLon,radar.lat,radar.lon):Double.NaN;' in JAVA
assert 'Double.isFinite(meters)' in JAVA
assert 'int ti=rr+cc;String key=radarFilterKeys[ti];' in JAVA
assert DATA['global_percent_estimate']==95 and DATA['global_percent_previous']==95
assert DATA['global_delta_points']==0 and DATA['global_scope_rebased']
assert not DATA['global_percent_audited']
navigation=next(t for t in DATA['workstreams'] if t['name']=='Navigation')
assert navigation['previous_percent_estimate']==100 and navigation['percent_estimate']==100 and navigation['delta_points']==0
assert navigation['status']=='implementation_code_complete_validation_after_compile'
radar=next(t for t in DATA['workstreams'] if t['name']=='Radars')
assert radar['previous_percent_estimate']==99 and radar['percent_estimate']==99 and radar['delta_points']==0
assert DATA['visual_implementation_percent']==99
map_work=next(t for t in DATA['workstreams'] if t['name']=='Carte en ligne MapLibre')
assert map_work['previous_percent_estimate']==96 and map_work['percent_estimate']==96 and map_work['delta_points']==0
assert 'simulé' in DATA['visual_validation_status'] and not DATA['validation_11_14_180']['xiaomi_tested']
assert 'PRIVÉ · Enregistré sur ce téléphone' in JAVA
assert 'void showRoadguardDrawer()' in JAVA and 'openInlineDestinationSearch()' in JAVA
assert 'void showRadarDetails(Danger radar)' in JAVA and 'focusRadarOnMap(radar)' in JAVA
assert 'INFORMATIONS DU POINT' in JAVA and 'VOIR SUR LA CARTE' in JAVA
assert 'Aucun signalement pour le moment' in JAVA
assert 'previous_percent_estimate' in JAVA and 'delta!=0' in JAVA and 'int current=Math.max(0' in JAVA
assert '0xff72d7b4' in JAVA
assert 'confirmLocalReport(hazard)' in JAVA
signal=next(t for t in DATA['ui_workstreams'] if t['name']=='Signalements')
assert signal['previous']==100 and signal['current']==100
radars=next(t for t in DATA['ui_workstreams'] if t['name']=='Radars et alertes')
assert radars['previous']==100 and radars['current']==100
routes=next(t for t in DATA['ui_workstreams'] if t['name']=='Itinéraires')
assert routes['previous']==100 and routes['current']==100
assert 'BASES EMBARQUÉES' in JAVA and 'ITINÉRAIRE CALCULÉ' in JAVA and 'CONFIGURATION ACTUELLE' in JAVA
assert 'CACHE DE LA CARTE' in JAVA
assert 'CARTE EN LIGNE · AUCUN PRÉCHARGEMENT' in JAVA
assert 'Zoom adaptatif' in JAVA and 'Anticipation du trajet' in JAVA
assert 'Réduction des commandes en roulant' in JAVA
assert 'tuiles déjà consultées' in JAVA and 'durée et couverture ne sont pas garanties' in JAVA
assert 'WebSettings.LOAD_CACHE_ONLY' in JAVA and 'WebSettings.LOAD_DEFAULT' in JAVA
assert 'GPS À AUTORISER' in JAVA and 'GPS RÉCENT' in JAVA and 'GPS EN ATTENTE' in JAVA
assert 'Position GPS récente disponible' in JAVA
assert 'maybePrefetch50Km' not in JAVA and 'configureEmergencyBuffer' not in JAVA
MAP=(ROOT/'app/src/main/assets/map.html').read_text()
assert '11.14.129' in MAP and '#f04458' in MAP and 'LEAFLET' not in MAP
assert 'recenter.setBackground(accentPanel(23,false))' in JAVA
assert 'RECHERCHER UN TRAJET' in JAVA and 'destinationSearchInput=new EditText(this)' in JAVA and 'searchDestinationFromBar();return true;' in JAVA
assert 'Basculer entre le nord en haut et le suivi de la route' in JAVA
assert 'routeColor||"#2eafff"' in MAP  # Requested route palette retains a blue fallback.
assert 'roadguardSurfaceColor(int color,boolean border)' in JAVA
assert 'VOTRE ROUTE. EN DIRECT.' in JAVA and 'OUVRIR LA CARTE' in JAVA
assert 'content.setPadding(pxDp(12),pxDp(8),pxDp(12),pxDp(104))' in JAVA
assert 'setVerticalScrollBarEnabled(false)' in JAVA
assert 'tile.setMinimumHeight(pxDp(84))' in JAVA
assert 'glyphLp=new LinearLayout.LayoutParams(pxDp(46),pxDp(38))' in JAVA
assert 'pxDp(70)' in JAVA and 'ff253140' in JAVA
assert 'pxDp(48)' in JAVA
assert 'luma*(border?.82f:1.12f)+(border?25f:9f)' in JAVA
assert 'return color;\n  }' not in JAVA
ML=(ROOT/'app/src/main/assets/map_maplibre.html').read_text()
OFF=(ROOT/'app/src/main/assets/map_offline.html').read_text()
assert "'circle-color':'#20bfff'" in ML and "'circle-color':'#ef4458'" not in ML
assert 'background:#20bfff' in ML
assert "'raster-saturation',night?-.38:-.08" in ML
assert "dot(gps,'#20bfff',9)" in OFF and "dot(route[0],'#f04458',7)" in OFF
print('41 contrôles statiques ROADGUARD '+VERSION+' réussis')
