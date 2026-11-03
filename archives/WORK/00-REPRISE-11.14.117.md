# Reprise ROADGUARD 11.14.117

## Travaux
- `RouteRequestPolicy` : URL OSRM construite depuis des coordonnées validées; endpoint strict HTTPS; réponse JSON limitée à 1,5 Mo; distance et durée contrôlées.
- `calculateRoadRoute()` : libération de l’état pending aux issues de la requête, notamment quand destination change pendant l’appel; itinéraire sauvegardé et message d’état clair après succès.
- Validation provider : interdit `userinfo`, fragment ou query dans les templates génériques; reconnaissance Stadia limitée à ses hosts exacts/sous-domaines.
- Caméra inchangée.

## Avancement estimé
GPS 100; MapLibre 85; planification cache 50 km 74 (+3); secours coupure 35 (+5); APK/Xiaomi 10; navigation 40 (+5); radars locaux 35 (+5); caméra 40; interface/architecture visuelle 100; cache automatique opérationnel 0. Global 57 (+1).

Écrans : Carte 100; Accueil 100; Alertes 100; Signaler 100; Itinéraires 100; Paramètres 100 (tous +0 visuel cette version).

## Blocages connus
- `tools/preflight_android_release.py` confirme l’absence Android SDK platform 34 et de Gradle/wrapper : impossible de produire/essayer l’APK ici.
- Cache GPS automatique inactif jusqu’à configuration d’un fournisseur licencié permettant préchargement et conservation.
- Routage actuel utilise un serveur de démonstration OSRM, sans garantie de service.
- Pas de radars communautaires connectés. La caméra est gardée pour la dernière étape.
