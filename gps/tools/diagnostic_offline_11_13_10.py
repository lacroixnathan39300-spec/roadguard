#!/usr/bin/env python3
"""Diagnostic statique du correctif hors-ligne ROADGUARD 11.13.10."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
assets=root/'app/src/main/assets'
java=(root/'app/src/main/java/fr/roadguard/app/MainActivity.java').read_text()
html=(assets/'map_maplibre.html').read_text()
reader=(root/'app/src/main/java/fr/roadguard/app/PmtilesReader.java').read_text()
checks={
 'maplibre_js_embarque': (assets/'vendor/maplibre-gl.js').is_file(),
 'maplibre_css_embarque': (assets/'vendor/maplibre-gl.css').is_file(),
 'page_vectorielle_presente': (assets/'map_maplibre.html').is_file(),
 'appel_setVectorPmtiles': 'setVectorPmtiles(' in java,
 'intercepteur_local_present': 'serveOfflineTile(' in java,
 'import_local_sans_reseau': 'void importExistingPmtiles(Uri source)' in java and 'getContentResolver().openInputStream(source)' in java,
 'cors_tuiles_webview': 'Access-Control-Allow-Origin' in java,
 'style_source_layer_hypothese': "'source-layer':'transportation'" in html,
 'labels_villes_routes': "'source-layer':'place'" in html and "'source-layer':'transportation_name'" in html,
 'glyphes_fr_locaux': all((assets/'glyphs/Noto Sans Regular'/f'{r}.pbf').is_file() for r in ('0-255','256-511','512-767')),
 'brotli_reader': 'BrotliInputStream' in reader,
 'auto_activation_pack_local': 'activateExistingFrancePmtiles();' in java,
 'carte_connectee_par_defaut': 'file:///android_asset/map.html' in java,
}
for k,v in checks.items():print(('OK' if v else 'MANQUANT'),k)
if not checks['maplibre_js_embarque'] or not checks['maplibre_css_embarque']:
 print('BLOQUANT : MapLibre manque dans les assets Android.')
if not any(assets.rglob('*.pmtiles')):
 print('NON DISPONIBLE ICI : le PMTiles réel n’est pas inclus dans le projet; il doit être conservé/importé depuis le téléphone sans nouveau téléchargement.')
