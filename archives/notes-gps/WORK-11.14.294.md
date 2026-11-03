# ROADGUARD GPS 11.14.294 — fonctions audio du véhicule sur mobile

27 octobre 2026. Application GPS autonome, sans launcher. Paquet fr.roadguard.app, code494, AndroidAPI26 minimum. Signature de mise à jour conservée.

## Modifications réellement intégrées
- Studio audio : courbe utilisateur dix bandes, préréglages Neutre/Voix/Rock/Jazz/Électronique, renfort des basses et réserve de niveau. Les réglages sont mémorisés. Ils sont adaptés aux fréquences et limites des bandes réelles du téléphone.
- Traitement audio réel sur les sessions non nulles annoncées par les lecteurs compatibles ; fermeture des effets à la fin des sessions et nombre d’effets borné. L’interface indique l’absence de session traitée. Aucun traitement global par session0 ni promesse d’égaliseur universel. Un bouton ouvre le panneau Android si disponible.
- Choix du lecteur : Spotify par défaut, lecteur actif automatique, sessions actives et applications musicales détectées, notamment YouTube Music/Deezer/VLC lorsqu’installées. Une source choisie explicitement ne commande jamais une autre application par défaut.
- Mini lecteur déplaçable existant désormais lié à la source choisie. Les commandes sont celles exposées par le lecteur. Le téléphone peut transmettre son son à l’autoradio par les réglages Bluetooth Android.
- Nettoyage des fenêtres musicales et de leurs rappels à la fermeture de l’activité. Aucun accès aux notifications supplémentaire si l’autorisation est déjà accordée ; l’option reste volontaire et sert aux sessions musicales.

## Accès
Réglages → Plus d’options → Musique et égaliseur. Choisir le lecteur ; ouvrir le Studio audio ou afficher le mini lecteur sur la carte. Activer les commandes musicales si Android le demande. Pour qu’un lecteur compatible annonce sa session au Studio, relancer sa lecture après ouverture de ROADGUARD.

## Fonctions déjà communes et conservées
Bips radar progressifs jusqu’au son continu à proximité, tonalité de dépassement connu, cartes téléchargées prioritaires puis complément réseau, palettes jour/nuit et couleurs, signalements, journal GPS, compte et téléchargement des mises à jour depuis le site. Le site15 retire la limite d’appareils pour les comptes administrateurs vérifiés.
Le moteur de carte et ses ressources restent identiques. Les sept bases radars embarquées restent identiques. La version11.14.293 est conservée dans les archives du site.
Le tableau de bord launcher, les réglages MCU de l’autoradio et le panneau récepteur de projection téléphone restent dans DRIVE ; ils ne sont pas ajoutés au GPS autonome.

## Validation
48tests JUnit réussis, aucun échec/erreur/ignoré : musique16, audio mobile16, bips8, palettes6, bootstrap local2. Scénarios audio sur API26 et36 ; les tests radar/cartes incluent API27/28. Lint NewApi/InlinedApi : aucun problème. Identité/versions, CRCZIP, DEX038, signaturesV1/V2 et certificat conservé vérifiés ; données radars et ressources du moteur comparées aux sources293 et à l’APK.
Pas d’essai sur le téléphone de Nathan, de DSP physique, de qualité sonore ou de Bluetooth réel. Un test logiciel ne démontre pas qu’un lecteur particulier expose une session égalisable.

## Reconstruction
JDK21, Gradle8.13, SDK36, build-tools35. :app:assembleDebug. La clé de signature reste privée, fournie par ROADGUARD_GPS_DEBUG_KEYSTORE ; aucune clé ni APK dans l’archive source. Voir verification/release-11.14.294/VERIFICATION.json.

🔴 INTERVENTION OBLIGATOIRE : mettre à jour le GPS avec l’APK294 sans désinstaller l’ancienne application, puis tester le lecteur choisi, le mini lecteur et le statut réel du Studio audio sur le téléphone.
