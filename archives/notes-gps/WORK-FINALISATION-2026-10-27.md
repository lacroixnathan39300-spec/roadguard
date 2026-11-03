# ROADGUARD — Lot du 27 octobre 2026
Versions : GPS11.14.295/code495/API26 ; DRIVE1.11.0/code2100/API27/ARM32+64 ; Compagnon1.2.0/code120/API29.
Ajouts communs : annonce explicite de dépassement à proximité d’un radar avec limite connue, désactivable ; cadence vocale au plus30s, priorité à première annonce radar, ledger commun premier/arrière-plan, reprise après échec audio. Horloge monotone.
DRIVE : musique/choix lecteur/Studio communs avec GPS ; un seul propriétaire AudioEffect pour éviter les contrôleurs concurrents. Effet réel seulement sur sessions annoncées et matériel compatible, pas de DSP universel garanti.
Compagnon : compte ROADGUARD pour les mises à jour du site ; vérifications SHA256, taille, paquet, certificat, minSDK, version avant installation Android confirmée. Aucun mot de passe mémorisé. Jeton conservé même si activité recréée pendant connexion. Recul réseau et Retry-After ; aucune file centrale strictement séquentielle.
Aperçu téléphone : qualité480px/3fps,480px/jusqu’à12fps,640px/jusqu’à6fps, consentement MediaProjection, flux chiffré appairé. Vidéo masquée pendant conduite. Ni contrôle distant tactile, ni vidéo30fps, ni contournement des surfaces protégées Netflix. Son Bluetooth séparé.
Liens : aide/téléchargements/support vers site ROADGUARD. Nouveaux QR roadguard:// ; mettre à jour les deux applications pour nouvel appairage. Identités de paquet internes conservées pour continuité des données et signatures ; licences tiers préservées.
Conduite : petites commandes journal et affichage panneau accessibles ; vidéo et réglages longs réservés à arrêt.
Conservés : moteur carte validé, sept bases radar embarquées, priorité cartes locales, palettes, bips progressifs, profils launcher, données utilisateur et signatures.
La compilation et les tests automatisés ne remplacent pas les essais physiques : installation sur8227L, voix/ducking/égaliseur Bluetooth, cartes téléchargées, appairage et mise à jour site restent à vérifier sur appareils.
Règle permanente : reporter les améliorations compatibles dans GPS ET DRIVE, sans launcher mobile, sans écraser les adaptations matérielles.
