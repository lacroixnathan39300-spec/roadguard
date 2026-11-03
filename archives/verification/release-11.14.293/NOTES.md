# ROADGUARD — Audio, partage du téléphone et mises à jour

DRIVE 1.10.0 : ARM32/64, API27 minimum. GPS 11.14.293 : API26 minimum. Compagnon 1.1.1 : téléphone Android10 minimum. Signatures conservées.

## Modifications
- Radar : bips progressifs dès 1km, jusqu’au son continu dans les 50 derniers mètres. Dépassement d’une limitation connue : tonalité distincte, plus aiguë. Pas de limite inventée. Arrêt sur position périmée, sortie de zone ou désactivation. La parole garde la priorité.
- Studio audio DRIVE : courbe utilisateur 10 bandes, préréglages, renfort des basses si disponible, réserve de niveau. Traitement réel des sessions audio compatibles annoncées à Android, avec affichage du nombre de bandes matérielles. Le DSP propriétaire, la radio et le Bluetooth de certains postes peuvent contourner ces effets : aucun traitement global ni niveau de qualité professionnel ne peut être garanti sans essai matériel. Le panneau audio système peut être ouvert si disponible.
- Musique du téléphone : source Téléphone dans DRIVE, commandes via Compagnon. Le son reste transmis en Bluetooth A2DP au poste, indépendamment du lien Wi-Fi des applications. Les commandes dépendent des possibilités du lecteur et de l’accès aux notifications sur le téléphone.
- GPS : mini-lecteur Spotify déplaçable dans la carte, affichage/masquage dans les commandes Spotify. Il reste dans l’application, sans superposition au-dessus d’autres applications.
- Panneau téléphone : DRIVE > Réglages > Téléphone à côté de la carte. Dans Compagnon > Partager mon écran, accepter la demande Android et choisir écran entier pour le téléphone complet. Aperçu JPEG 480px, au plus 3 images/s, transmis uniquement au poste appairé sur le canal chiffré ; taille et file d’attente bornées. Ce premier mode n’est pas une projection vidéo fluide ni une commande tactile distante. Les surfaces protégées peuvent rester noires, notamment dans certaines applications vidéo. Le son reste en Bluetooth. Affichage vidéo suspendu en déplacement ; arrêt depuis Compagnon ou la notification Android.
- DRIVE et GPS : bouton Mises à jour, indication de disponibilité, vérification automatique environ toutes les 6h. Téléchargement authentifié depuis le site ; SHA256, identité du paquet, signature et version vérifiées avant ouverture de l’installateur Android. Installation toujours confirmée par l’utilisateur. Compagnon se met à jour avec l’APK fourni ou partagé depuis DRIVE ; l’ancien téléchargement GitHub reste désactivé dans l’édition personnelle.
- Radars : signalements du secteur automatiquement toutes les 2 à 3min ; inventaire permanent au démarrage puis environ toutes les 6h, avec décalage entre appareils. En cas de 429/503, respect de Retry-After et recul progressif avec délai aléatoire, jusqu’à 10min ou davantage si le serveur le demande. Données locales conservées. Ce mécanisme répartit les reprises ; il ne garantit pas une file centrale strictement ordonnée entre utilisateurs.
- Les cartes locales prioritaires et les palettes personnalisées de la version précédente sont conservées. Aucune donnée radar embarquée n’a été modifiée.

## Validation
Voir VERIFICATION.json et les résultats JUnit. Contrôles logiciels ciblés des cadences, limites connues/inconnues, versions, média API27 et messages du compagnon. Contrôle des signatures, formats DEX, bibliothèques ARM et intégrité ZIP. Vérification API Android sans problème signalé.
Le service de mises à jour du site a été testé et publié en version14 avant l’interruption. Ces tests serveur ne remplacent pas un essai du téléchargement/installation sur appareil.
Aucun essai audio réel, routier, du DSP de l’autoradio ou de Netflix n’est encore effectué sur ces nouvelles versions. Un ancien test statique d’import PMTiles, déjà obsolète avant ce chantier, n’est pas compté dans les vérifications réussies.

## Actions sur les appareils
Installer DRIVE sur l’autoradio et Compagnon sur le téléphone ; installer GPS séparément si utilisé. Autoriser les commandes musicales dans Compagnon, sélectionner la source Bluetooth du poste, puis Téléphone dans Musique de DRIVE. Pour partager l’écran, confirmer la capture sur le téléphone et tester à l’arrêt. Vérifier le statut et l’effet réel du Studio audio. Pour le mini-lecteur, ouvrir Spotify dans les commandes GPS puis choisir Afficher le mini lecteur sur la carte.

## Sources et reconstruction
Sources Compagnon : projet commun complet, module :companion. DRIVE : :app:assembleGithubDebug (fabrique le compagnon intégré). GPS : :app:assembleDebug. JDK21, Gradle8.13, SDK36 et build-tools35. AAPT2 du SDK35 utilisé dans cet environnement via android.aapt2FromMavenOverride.
155 ressources référencées contrôlées ; reference_ui.html modifié depuis GPS 11.14.292. Voir roadguard/AUDIO-UPDATE-1.10.0.json.
Aucune clé privée incluse. Fournir séparément ROADGUARD_DEBUG_KEYSTORE et ROADGUARD_GPS_DEBUG_KEYSTORE pour conserver la signature des installations existantes.
