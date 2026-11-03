"""Regression checks for verified, local, country-level radar databases."""
from pathlib import Path
import json

ROOT=Path(__file__).resolve().parents[1]
MAIN=(ROOT/'app/src/main/java/fr/roadguard/app/MainActivity.java').read_text()
SERVICE=(ROOT/'app/src/main/java/fr/roadguard/app/BackgroundAlertService.java').read_text()
GRADLE=(ROOT/'app/build.gradle').read_text()
SPEC=(ROOT/'RADAR-COUNTRY-CATALOG.md').read_text()
EXAMPLE=json.loads((ROOT/'catalogs/radar-country-catalog.example.json').read_text())
BUILDER=(ROOT/'tools/build_radar_country_catalog.py').read_text()

VERSION=json.loads((ROOT/'app/src/main/assets/roadguard_progress.json').read_text())
assert ("versionCode "+str(VERSION['versionCode'])) in GRADLE and ("versionName '"+VERSION['version']+"'") in GRADLE
assert 'String type=lufopSwissCsvType(entry.getName());ByteArrayOutputStream csv=type==null?null:new ByteArrayOutputStream();' in MAIN
assert 'renderRadarCountryDownloads(content)' in MAIN
assert 'Fournisseurs de bases radar' in MAIN and 'showRadarProviders()' in MAIN
assert 'Lufop' in MAIN and 'ODbL 1.0' in MAIN and '200 résultats par appel' in MAIN
assert 'SCDB.info' in MAIN and 'GPS Data Team' in MAIN and 'Kostenlos DB' in MAIN
assert 'ouvrir le site' not in MAIN.lower() or 'Ouvrir le site' in MAIN
assert 'openRadarProviderUrl' in MAIN and 'ACTION_VIEW' in MAIN
assert 'archive standard « Zones de danger EU CSV »' in MAIN
assert 'Configurer le catalogue' not in MAIN and 'Choisir un pays' in MAIN
assert 'SOCLE OFFICIEL' in MAIN and 'radarFilterLabels' in MAIN
assert 'SOCLE OFFICIEL' in MAIN
assert 'La Suisse interdit les avertisseurs de contrôles radar' not in MAIN and 'La base suisse sera ajoutée à la carte et aux alertes GPS' in MAIN
assert 'Importer un catalogue JSON' in MAIN and 'ACTION_OPEN_DOCUMENT' in MAIN
assert 'application/json' in MAIN and 'request==62' in MAIN
assert 'parseRadarCountryCatalog(byte[] bytes)' in MAIN and 'importRadarCountryCatalog(data.getData())' in MAIN
assert 'Catalogue refusé' in MAIN and '256 Kio' in MAIN
assert 'RADAR_COUNTRY_CODES={"FR","CH","BE","DE","IT","ES"' in MAIN
assert 'OpenStreetMap · téléchargement direct' in MAIN and 'mettre à jour' in MAIN.lower()
assert 'downloadOsmCountry(String country,String name)' in MAIN
assert 'downloadRadardroidLufopCountry(' not in MAIN
assert 'https://lufop.net/radardroid-lufop-' not in MAIN
assert 'Lufop Radardroid · téléchargement direct sans clé' not in MAIN
assert 'openRadardroidLufopConnection(' not in MAIN
assert 'parseLufopCsvRow(String line)' in MAIN
assert 'void chooseLufopSwissCsvZip()' in MAIN and 'void importLufopSwissCsvZip(Uri uri)' in MAIN
assert 'request==63&&result==RESULT_OK' in MAIN and 'importLufopSwissCsvZip(data.getData())' in MAIN
assert 'CHFIXECH' in MAIN and 'CHFEUROUGECH.' in MAIN and 'CHMOBILECH.' in MAIN and 'CHTUNNELCH.' in MAIN
assert 'endsWith(".ASC")?"ASC"' in MAIN and 'endsWith(".CSV")?"CSV"' in MAIN
assert 'duplicates_removed' in MAIN and 'lufopSwissCategoryCount(typeCounts,"Radar fixe")' in MAIN
assert 'Lufop Radardroid export' not in MAIN
assert 'mapatlas.eu/data/camera-index/catalog.json' in MAIN and 'mapatlas.eu/data/camera-index/"+country+".geojson' in MAIN
assert 'mapAtlasPackId(String country)' in MAIN and 'downloadMapAtlasCountry(String country,String name)' in MAIN
assert 'showMapAtlasRegionPicker(JSONArray regions)' in MAIN and '106' in MAIN
assert 'Choisir un pays · bases radar' in MAIN and 'MapAtlas + OpenStreetMap + Lufop API' in MAIN
assert 'CC BY 4.0' in MAIN and 'MapAtlas (communautaire)' in MAIN
assert 'Radar feu rouge (MapAtlas)' in MAIN and 'Radar vitesse moyenne (MapAtlas)' in MAIN and 'Radar variable (MapAtlas)' in MAIN
assert 'Base communautaire MapAtlas Camera Index' in MAIN
assert 'total<150000' in MAIN
assert 'MapAtlas Camera Index' in MAIN and 'Open GATSO POI' in MAIN and 'Base officielle France' in MAIN and 'Licence Ouverte 2.0' in MAIN
assert 'github.com/1e1/Open-GATSO-POI/releases/latest' in MAIN
assert 'https://overpass-api.de/api/interpreter' in MAIN and 'https://overpass.kumi.systems/api/interpreter' in MAIN and 'https://z.overpass-api.de/api/interpreter' in MAIN and 'https://maps.mail.ru/osm/tools/overpass/api/interpreter' in MAIN
assert 'String[] endpoints=' in MAIN and 'Serveurs Overpass temporairement indisponibles' in MAIN
assert 'for(int attempt=0;attempt<endpoints.length;attempt++)' in MAIN and 'Thread.sleep(700L*(attempt+1))' in MAIN
assert 'highway' in MAIN and 'speed_camera' in MAIN and 'ISO3166-1' in MAIN
assert '© OpenStreetMap contributors' in MAIN and 'ODbL 1.0' in MAIN
assert 'Les bases OpenStreetMap se téléchargent directement sans clé API' in MAIN
assert 'SOCLE OFFICIEL' in MAIN
assert 'radar_country_catalog_url' in MAIN and 'refreshRadarCountryCatalog()' in MAIN
HTTP=(ROOT/'app/src/main/java/fr/roadguard/app/SecureHttp.java').read_text()
assert 'https".equalsIgnoreCase(url.getProtocol())' in HTTP
assert 'return SecureHttp.open(url,15000,30000,' in MAIN
assert 'SecureHttp.open("https://mapatlas.eu/data/camera-index/"+country+".geojson",20000,60000,' in MAIN
assert 'radarCountryConnection=c;c.setConnectTimeout(20000)' not in MAIN
assert 'if(pack!=null){labels.add("Catalogue ROADGUARD' in MAIN
assert 'Option avancée Lufop API · clé personnelle nécessaire' in MAIN
assert 'Aucune fiche vérifiée n’est publiée pour ce pays' not in MAIN
assert 'sha256' in MAIN and 'DigestInputStream' in MAIN and 'MessageDigest.getInstance("SHA-256")' in MAIN
assert 'MAX_RADAR_COUNTRY_BYTES=32L*1024*1024' in MAIN
assert 'validateRadarCountryCsv(part)' in MAIN and 'id;type;lat;lon' in MAIN
assert 'radarCountryDownloadRunning' in MAIN and 'radarCountryDownloadCancel' in MAIN
assert 'radarCountryConnection' in MAIN and 'active.disconnect()' in MAIN
assert 'radarCountryDownloadCancel?"Téléchargement annulé"' in MAIN
assert 'Annulation en cours' in MAIN and 'setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)' in MAIN
assert 'downloaded*1000/expected' in MAIN and 'base précédente est conservée' in MAIN
assert 'isCurrentRadarPack(pack)' in MAIN and 'radarCatalogRequestToken' in MAIN
assert 'Catalogue modifié pendant le téléchargement' in MAIN
assert 'radar_country_installed' in MAIN and 'radar_countries' in MAIN
assert 'loadInstalledRadarCountries();' in MAIN and 'removeRadarCountry(id)' in MAIN
assert 'loadInstalledRadarCountries();' in SERVICE and 'radar_country_installed' in SERVICE
assert 'DangerTypePolicy.isRadar(type)' in SERVICE
assert EXAMPLE=={'schema':1,'packs':[]}
assert 'attribution' in SPEC and 'license' in SPEC and 'SHA-256' in SPEC
assert 'build_radar_country_catalog.py' in SPEC and 'hashlib.sha256' in BUILDER
assert 'https' in BUILDER and 'MAX_ROWS = 100_000' in BUILDER
print('Bases radar : MapAtlas multi-pays, CC BY, téléchargement interne et catégories, catalogue HTTPS protégé contre les réponses périmées, progression annulable, SHA-256 et CSV local vérifiés')

# In-app Lufop download path: key is device-private, HTTPS, conversion and conservative cap checks.
assert 'configureLufopKey()' in MAIN and 'lufop_api_key' in MAIN
assert 'https://api.lufop.net/api?key=' in MAIN and '&format=json&nbr=30000&pays=' in MAIN
assert 'records.length()>=30000' in MAIN and 'records.length()==10000' in MAIN and 'records.length()==200' in MAIN
assert 'MAX_LUFOP_JSON_BYTES=8L*1024*1024' in MAIN and 'Réponse supérieure à 8 Mo' in MAIN
assert 'lufopRadarType(' in MAIN and 'validateRadarCountryCsv(part)' in MAIN
assert 'Données Lufop et contributeurs · ODbL 1.0' in MAIN
assert 'RadarSourcePolicy.quarantined(id)' in MAIN and 'Base radar chargée sur cet appareil, y compris pour la Suisse.' in MAIN
assert 'if(!country.matches("[A-Z]{2}"))return;' in MAIN
assert 'startsWith("CH"))continue;' not in MAIN and 'carte et alertes disponibles' in MAIN
SERVICE=(ROOT/'app/src/main/java/fr/roadguard/app/BackgroundAlertService.java').read_text()
assert 'startsWith("CH"))continue;' not in SERVICE and 'RadarSourcePolicy.quarantined(id)' in SERVICE
assert 'API key' not in MAIN.split('safeLufopError',1)[1].split('lufopRadarType',1)[0]
