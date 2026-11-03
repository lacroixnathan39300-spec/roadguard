#!/usr/bin/env python3
"""Regression guards for the no-catalog one-click France download path."""
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[1]
JAVA=(ROOT/'app/src/main/java/fr/roadguard/app/MainActivity.java').read_text(encoding='utf-8')
MANIFEST=(ROOT/'app/src/main/AndroidManifest.xml').read_text(encoding='utf-8')
SERVICE=(ROOT/'app/src/main/java/fr/roadguard/app/MapPackDownloadService.java').read_text(encoding='utf-8')
class PublicFranceFallbackTests(unittest.TestCase):
 def test_fixed_https_source_and_wifi_check_are_present(self):
  self.assertIn('https://map.comptoir.net/france-openmaptiles.pmtiles',JAVA)
  self.assertIn('setRequestMethod("HEAD")',JAVA)
  self.assertIn('"map.comptoir.net".equalsIgnoreCase(u.getHost())',JAVA)
  self.assertIn('if("france".equals(id)){downloadFranceOneTap();return;}',JAVA)
  self.assertIn('if("france".equals(id)?fullFrance:',JAVA)
  self.assertIn('France complète · télécharger · 2,70 Gio',JAVA)
  self.assertIn('if(publicFrance)connection.setRequestProperty("If-Match",PUBLIC_FRANCE_PMTILES_ETAG)',JAVA)
  self.assertIn('reader.vectorTile(10,518,352)',SERVICE)
  self.assertIn('reader.vectorTile(10,537,379)',SERVICE)
 def test_pmtiles_limit_structure_and_declared_france_bounds_are_checked(self):
  self.assertIn('MAX_PMTILES_DOWNLOAD_BYTES=8L*1024*1024*1024',JAVA)
  self.assertIn('checkRemotePmtilesHeader(new URL(url),expected)',SERVICE)
  self.assertIn('pmtilesCoversFranceAndCorsica(part)',SERVICE)
  self.assertIn('"Empreinte SHA-256 incorrecte"',SERVICE)
  self.assertIn('FOREGROUND_SERVICE_DATA_SYNC',MANIFEST)
 def test_country_dialog_is_closed_before_action_runs(self):
  block=JAVA[JAVA.index('void chooseCountryMap()'):JAVA.index('void searchCountryMap()')]
  self.assertIn('dialog.dismiss();afterCountryDialogDismiss(()->downloadCountryOneTap(id,name))',block)
  self.assertNotIn('downloadCountryOneTap(MAP_COUNTRY_IDS[which]',block)
 def test_wifi_state_permission_declared(self):
  self.assertIn('android.permission.ACCESS_NETWORK_STATE',MANIFEST)
 def test_cellular_download_is_default_and_warns_before_transfer(self):
  self.assertIn('getBoolean("maps_wifi_only",false)',JAVA)
  self.assertNotIn('getBoolean("maps_wifi_only",true)',JAVA)
  self.assertIn('ce téléchargement utilisera ton forfait de données',JAVA)
  self.assertIn('Taille annoncée : ',JAVA)

if __name__=='__main__':unittest.main()
