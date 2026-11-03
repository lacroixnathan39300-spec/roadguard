# ROADGUARD 11.13.82 — raccordement MapLibre / passerelle de cache

## Réalisé
- `map_maplibre.html`: fonction `setLicensedEmergencyMap(enabled)` change réellement la source raster MapLibre vers `https://roadguard.local/emergency/{z}/{x}/{y}.png` lorsque la source HTTPS XYZ sous licence est configurée; retour à OSM en consultation normale sinon.
- `MainActivity.java`: sélection de la source lors du chargement WebView en lisant la préférence existante `licensed_emergency_xyz_template`.
- La route locale est interceptée par `serveEmergencyTile` existant depuis 11.13.81; cache temporaire disque, aucun téléchargement en masse OSM.
- GPS bleu et caméra non modifiés.

## Tests
- `javac` + `EmergencyTileGatewayTest` réussis (JVM).
- Contrôle de présence de la nouvelle fonction et de son appel Android effectué.
- PAS de compilation Android / APK; PAS de validation sur Xiaomi; PAS de source commerciale configurée.

## Blocages et priorité
1. Fournisseur de tuiles raster HTTPS XYZ avec permission explicite de stockage et préchargement; ne jamais faire de bulk fetch sur tile.openstreetmap.org.
2. Ajouter configuration utilisateur/administrateur sécurisée de l'URL (la préférence existe, mais pas d'écran de configuration).
3. Connecter le planificateur 50 km à une file de préchargement autorisée, progressive et bornée en volume, réseau et batterie; ne pas prétendre couvrir 50 km à tous les zooms.
4. Construire l'APK et tester avec réseau puis coupure sur Xiaomi.
5. Contrôler le changement de style MapLibre et la continuité des marqueurs en conditions réelles.

## Avancement honnête
- GPS 100% historique non revalidé; carte 85% historique; réseau 50% historique; planification 45%; cache 50%; passerelle 60% (raccordée au rendu mais non validée); préchargement réel 0%; APK Xiaomi 0%.
- Aucun pourcentage global fiable.
