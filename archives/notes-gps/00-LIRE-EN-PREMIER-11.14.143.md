# ROADGUARD 11.14.143 — sources radar et Overpass

- Version source Android 11.14.143 / code 343. ZIP source, pas d’APK compilée.
- Retire `overpass.nchc.org.tw`, dont le nom d’hôte ne se résolvait pas sur le téléphone; remplace par des instances mondiales mentionnées dans le répertoire officiel OpenStreetMap.
- L’écran Fournisseurs présente Overpass/OSM (déjà intégré), la base officielle française du Ministère (déjà embarquée, mise à jour publiée le 30/12/2025, Licence Ouverte 2.0), Open GATSO POI (lien de téléchargement externe; licence du jeu agrégé non établie) et Lufop (API intégrée avec clé).
- Aucune base tierce aux droits incertains n’est empaquetée. OSM reste la source mondiale libre téléchargeable directement dans ROADGUARD sans clé.
- Tests Python/statique à exécuter; compilation et test Xiaomi requis. Estimations non auditées : radars 90→91 %, global 79→80 %.
