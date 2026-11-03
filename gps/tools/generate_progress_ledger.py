#!/usr/bin/env python3
"""Rebuild the dated ledger from the fixed backlog and recorded verification results."""
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
proof_file=ROOT/'verification/results-11.14.241.json'
proof=json.loads(proof_file.read_text()) if proof_file.exists() else {}
modules=[]
def group(name,weight):
 result={'name':name,'weight':weight,'tasks':[]};modules.append(result);return result

def task(group,id,name,weight,baseline,coded,code,auto_weight,check,baseline_passed=False):
 record=proof.get(check,{})
 checks=[{'name':'Contrôle automatique défini','weight':auto_weight,'passed':bool(record.get('passed',False)) and coded,
          'baseline_passed':baseline_passed,'evidence':record.get('evidence',''),'contract':check}]
 if weight>auto_weight:checks.append({'name':'Essai réel Xiaomi / véhicule','weight':weight-auto_weight,'passed':False,'baseline_passed':False,'evidence':'','contract':'Essai utilisateur non réalisé pour cette version'})
 group['tasks'].append({'id':id,'name':name,'weight':weight,'baseline_coded':baseline,'coded':coded,
   'code_evidence':code,'date':'2026-10-20','version':'11.14.241','checks':checks})

g=group('Carte / GPS',20)
task(g,'M1','Démarrage local sans attendre les mises à jour réseau',3,False,True,'CommunitySync.prepareStartup; MainActivity.showMap; LocalPageRuntimeTest',2,'local-pages')
task(g,'M2','Filtrage de la vitesse, du cap et des sauts GPS',3,True,True,'GpsMotion; MainActivity.startGPS',2,'gps-motion')
task(g,'M3','Ancrage horizontal sous rotation et hauteur réglable',4,False,True,'camera_geometry.js; rgVisualCamera; rgCameraOffset; MainActivity.pushPosition',3,'camera-anchor')
task(g,'M4','Exploration manuelle et recentrage volontaire',2,True,True,'resumeCamera; rgManualZoom; showRouteOverview',1,'camera-manual',True)
task(g,'M5','Zoom adaptatif avec hystérésis',2,True,True,'camera_motion.js; updateGPS',1,'camera-zoom',True)
task(g,'M6','Cache temporaire de tuiles, limites et récupération',3,True,True,'VisibleTileCache; OfflineCellStore; TileCacheRuntimeTest',1.5,'tile-cache')
task(g,'M7','Itinéraire, cache et reprise du guidage',3,True,True,'RouteRequestPolicy; MainActivity navigation methods',1,'routing')
g=group('Alertes',20)
task(g,'A1','Refus du GPS périmé et effacement des avertissements',3,False,True,'AlertPresentation.fresh; gpsFreshnessPulse; BackgroundAlertService.autoPulse',2,'alert-freshness')
task(g,'A2','Même distance arrondie pour texte, bulle et voix',3,False,True,'AlertPresentation; MainActivity.checkDangers; BackgroundAlertService.check; RadarBubbleOverlay',2,'alert-distance')
task(g,'A3','Feu rouge et passage à niveau sans fausse limitation',3,False,True,'RadarAlertPolicy.applicableLimit; CarRouteData.Radar',2,'alert-types')
task(g,'A4','Paliers partagés par type et position',3,False,True,'AlertMilestones; RadarIdentity; ReferenceUi.radarSeen; RoadguardCarScreen',2,'alert-dedup')
task(g,'A5','Bandeau contextuel, hystérésis et état nettoyé',3,False,True,'MainActivity.checkDangers; clearDangerWarning',2,'alert-banner')
task(g,'A6','Sens source et routes parallèles réellement distingués',3,False,False,'Filtre cap/couloir présent, direction source et cas parallèles encore incomplets',2,'alert-direction')
task(g,'A7','Audio de proximité, vitesse inconnue et temporisation',2,True,True,'RadarAlertAudio; RadarAlertsRuntimeTest',1,'alert-audio')
g=group('Sources / activation radar',25)
task(g,'D1','Surcouches additives sans retirer un original',5,False,True,'UpdatedRadarCatalog.load; active; MainActivity.loadBundledRadarDatabase',4,'catalog-additive')
task(g,'D2','Catalogues d’origine et Lufop communs téléphone/service/voiture',3,True,True,'UpdatedRadarCatalog; BundledRadarCatalog; NeighborRadarCatalog; LufopEuropeCatalog',2,'catalog-common')
task(g,'D3','États véridiques et comptages du catalogue chargé',3,False,True,'RadarDownloadActivity.loadActiveCounts; renderCountries',2,'country-ui')
task(g,'D4','Sept sources publiques contrôlées et téléchargements épinglés',3,False,True,'RadarDownloadSource; verified public release europe-202606',3,'source-audit')
task(g,'D5','Extraction 7z bornée dans Android',3,False,False,'Schéma réel inspecté hors application; extracteur Android à intégrer',2,'sevenzip-android')
task(g,'D6','SQLite vers modèle canonique avec métadonnées',3,False,False,'cameras(id,lat,lon,type) observée; convertisseur Android à faire',2,'sqlite-activation')
task(g,'D7','Fusion avec sens/fiabilité et rechargement complet à chaud',3,False,False,'Déduplication type/position ajoutée; direction, identifiants et rechargement foreground à compléter',2,'canonical-reload')
task(g,'D8','File séquentielle, annulation et remplacement atomique',2,True,True,'RadarDownloadActivity.startCountryBatch; downloadCountryArchive',1,'download-errors')
g=group('UI / ergonomie',10)
task(g,'U1','Thème graphite et rouge conservé',3,True,True,'MainActivity; reference_ui.html; ressources visuelles d’origine',1,'theme-ui')
task(g,'U2','Insets Android récents et formats d’écran',3,True,True,'MainActivity.configureModernAndroid; RadarDownloadActivity.onCreate',1,'modern-insets')
task(g,'U3','Pays triés et recherche avec/sans accents',2,False,True,'RadarDownloadActivity.searchKey; Collator',1,'country-ui')
task(g,'U4','Revue globale d’accessibilité et paysage',2,False,False,'Descriptions ciblées présentes; revue complète non réalisée',1,'accessibility')
g=group('Android Auto',10)
task(g,'C1','Variant téléphone indépendant de la bibliothèque Car',2,True,True,'app/build.gradle conditional roadguardAndroidAuto',2,'phone-build')
task(g,'C2','Service et écrans natifs de navigation Car App',3,True,True,'app/src/auto; AndroidRuntimeTest; CarSurfaceRuntimeTest',1,'auto-runtime')
task(g,'C3','Propriété des alertes et guidage cohérents entre appareils',3,False,False,'Catalogue partagé et limites corrigées; transition multimode à finaliser',2,'auto-alerts')
task(g,'C4','Validation DHU et autoradio du parcours complet',2,False,False,'Essai DHU/autoradio non réalisé',1,'auto-device')
g=group('Qualité / tests / compilation',10)
task(g,'Q1','Version Gradle alignée sur la livraison',2,False,True,'app/build.gradle: 11.14.241 / 441',2,'version-build')
task(g,'Q2','Tests ciblés et progression pondérée reproductible',2,True,True,'tools/test_camera_anchor.js; AlertConsistencyRuntimeTest; ProgressLedger',2,'targeted-tests')
task(g,'Q3','APK téléphone produit avec SDK 36',2,False,bool(proof.get('phone-build',{}).get('passed')),'Gradle assembleDebug -ProadguardAndroidAuto=false',2,'phone-build')
task(g,'Q4','APK Android Auto compilé séparément',2,False,bool(proof.get('auto-build',{}).get('passed')),'Gradle assembleDebug -ProadguardAndroidAuto=true',2,'auto-build')
task(g,'Q5','Installation CodeAssist et retours du lot sur matériel',2,False,False,'Aucun retour utilisateur 11.14.241 à cette date',1,'device-install')
g=group('Sécurité / données',5)
task(g,'S1','Empreintes des 59 ressources d’origine et ZIP contrôlé',2,False,True,'verification/original-data-11.14.240.json; integrity report',2,'data-integrity')
task(g,'S2','Mises à jour séparées, atomiques et repli',1,True,True,'AuthorizedRadarImporter; LufopEuropeCatalog; RadarDownloadActivity',.5,'import-rollback')
task(g,'S3','Provenance et licence des nouvelles sources consignées',1,False,True,'RadarDownloadSource; verification/speedalert-source-audit.json',1,'source-audit')
task(g,'S4','Limites des imports et destinations HTTPS contrôlées',1,True,True,'RadarSourcePolicy; SecureHttp; RadarDownloadSource.allowed',1,'http-policy')
ledger={'version':'11.14.241','baseline_version':'11.14.240','date':'2026-10-20','historical_unverified_estimate':48,
 'method':'Développement: tâche codée selon son contrat. Validation: poids des contrôles réellement réussis; essais matériels séparés. Référence reconstituée par inspection de la source 240, sans reprendre ses affirmations de tests.',
 'active_work':'Finaliser l’import Android 7z → SQLite → surcouche canonique, puis les transitions d’alertes et les essais Xiaomi / Android Auto.', 'modules':modules}
assert sum(g['weight'] for g in modules)==100
for g in modules:
 assert sum(t['weight'] for t in g['tasks'])==g['weight']
 for t in g['tasks']:assert abs(sum(c['weight'] for c in t['checks'])-t['weight'])<1e-8
path=ROOT/'app/src/main/assets/roadguard_progress_11_14_241.json';path.write_text(json.dumps(ledger,indent=2,ensure_ascii=False)+'\n')
print('Ledger:',sum(len(g['tasks']) for g in modules),'tasks, 100 points')
