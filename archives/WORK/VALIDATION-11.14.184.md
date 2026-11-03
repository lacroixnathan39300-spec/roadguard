# ROADGUARD 11.14.184 · validation et essai téléphone

## Réalisé

- Bulle radar automatique : activation avec autorisation de superposition, service GPS préparé au premier plan, affichage au passage en arrière-plan et masquage au retour. Le bouton d'arrêt et l'action de notification désactivent le mode automatique. Une fermeture forcée d'Android arrête le service.
- Recherche : autocomplétion Géoplateforme IGN, repli de recherche géocodée IGN puis géocodeur Android; liste de suggestions défilante plafonnée à 210 dp et fenêtre ajustée au clavier.
- Carte : commande d'arrêt de trajet pendant le guidage, conservation de la destination; retrait du préfixe/logo Leaflet bleu et jaune, conservation de l'attribution OpenStreetMap.
- Raccourcis Domicile et Travail : choix de la destination enregistrée; à la première utilisation, enregistrement de la destination courante ou de la position GPS récente. Appui long pour redéfinir.
- Bases radar : cinq pays voisins intégrés indiqués distinctement des bases téléchargées, avec nombre et date du relevé; sélecteur de pays indiquant les mises à jour réellement installées, toutes sources locales confondues.
- Menus compacts : hauteur de viewport contrôlée et conservation du défilement pendant les rafraîchissements GPS.
- Apparence : palette Android de couleur libre pour interface/curseur et itinéraire, en plus des pastilles; réglage du débit et de la hauteur de synthèse vocale française, utilisé dans l'application et dans le service d'alerte.

## Contrôles

- `:app:assembleDebug` réussi; APK versionName 11.14.184, versionCode 384.
- APK signé v2/v3 avec le certificat SHA-256 `52ff1a034ff05a11847435e25c0f2077614fe278290818b42b4bad198502ffa3`, identique à 11.14.183.
- Analyse syntaxique JavaScript et rendu des écrans « Bases radar » / « Apparence » avec données représentatives réussis.
- `:app:testDebugUnitTest` tenté : 87 tests Robolectric ne peuvent télécharger leurs artefacts SDK à cause d'un `UnknownHostException` dans cet environnement. Aucun de ces échecs ne constitue un résultat fonctionnel de l'application.

## À vérifier sur le Xiaomi 15T Pro

1. Installer l'APK par-dessus 11.14.183 sans supprimer les données; autoriser GPS précis et affichage superposé.
2. Activer la bulle depuis la carte, ouvrir Waze : vérifier apparition et position; rouvrir ROADGUARD : vérifier masquage. Vérifier l'alerte à proximité d'un point de test sans conduire.
3. Saisir « Champagnole » dans la recherche; vérifier propositions malgré le clavier, sélection, arrêt du trajet et reprise.
4. Ouvrir Apparence et Bases radar, descendre jusqu'en bas; choisir une couleur libre et une voix, puis revenir à la carte.
5. Contrôler les compteurs de pays intégrés et des bases réellement mises à jour après un téléchargement; les données historiques n'attestent pas une présence de radar mobile.

La récupération des signalements des conducteurs de Waze n'est pas implémentée : les interfaces officielles ne donnent pas cet accès. Les contributions ROADGUARD restent distinctes.
