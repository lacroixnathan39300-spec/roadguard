# WORK — État connu, limites et pistes

**Version d'entrée** : 11.13.9 / versionCode 170, étiquetée paquet 11.13.10 ; **version de correction source** : 11.13.10 / versionCode 171. Le ZIP avait des tests 11.13.8/code169; harmonisés. Carte annoncée existante sur Xiaomi : ~2,764 Go, pas jointe dans l'archive.

**Constats utilisateurs** : sur Xiaomi, catalogue France/Belgique/autres pays affiche « indisponible » car aucun catalogue ROADGUARD n'est lié ; le bouton France dispose désormais d'un candidat PMTiles public, à tester ; la 11.12.7 a été validée comme fluide. L'utilisateur veut **un seul bouton** pour tous les pays, sans préparation manuelle.

**Blocages actuels** : MapLibre GL JS/CSS 4.7.1 et plages de glyphes Noto Sans 0–767 sont embarqués avec licences et empreintes vérifiées. Décodeur Brotli local joint. Fichier PMTiles réel, métadonnées et vraies tuiles non accessibles depuis le workspace. Android SDK/Gradle et Xiaomi indisponibles; build et rendu hors ligne non testés. La source ne peut pas confirmer vraie lecture/rendu.

**Ressources techniques à évaluer** : Protomaps PMTiles et CLI `pmtiles extract` (avec licence/attribution à vérifier pour le jeu de données utilisé), MapLibre Native Android OfflineManager et son écosystème, données OSM Geofabrik `.osm.pbf` nécessitant transformation, OpenFreeMap seulement après vérification des droits de mise en cache offline. Ne pas affirmer que les sources fournissent des packs pays prêts à l'emploi sans vérification.

**Sécurité** : aucune clé API secrète intégrée au dépôt ; ne pas créer de liens fictifs ; contrôle des URLs HTTPS et certificats ; fichiers temporaires et protections ZIP/path traversal ; contrôle d'intégrité avant marquer « installé ».

**Réseau** : téléchargements Wi-Fi et mobiles. Le mobile est autorisé par défaut avec avertissement de taille et d'usage du forfait avant confirmation ; l'utilisateur peut imposer le Wi-Fi. Tester ces deux états et la perte de connexion pendant le transfert.

**Code ajouté** : import SAF local du PMTiles France (copie validée vers `files/pmtiles/france_local.pmtiles`, aucune connexion réseau), détection du fichier France vectoriel déjà stocké, activation MapLibre si ressources APK présentes, retour explicite à la carte en ligne, endpoint local glyphes/CORS et couches `transportation_name` / `place`. L'import depuis Téléchargements peut nécessiter une seconde copie de ~2,76 Go; l'archive d'origine n'est jamais supprimée. Les plages Noto Sans couvrent les caractères français usuels; l'affichage reste à confirmer contre le vrai schéma et en mode avion.

**Protocole de blocage** : Si aucun serveur externe autorisé ou aucun appareil de test n'est accessible, réaliser tout le travail local possible, livrer scripts et résultats vérifiés, puis consigner `BLOQUÉ : [raison exacte]`, accès nécessaires et solution reproductible. Ne pas demander à l'utilisateur de gérer des commandes si Work peut le faire.
