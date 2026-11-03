# ROADGUARD 11.14.107 — lot visuel multi-écrans

- Version source : 11.14.107 / versionCode 307.
- Écrans retravaillés : Paramètres, Apparence, Radars et alertes, Itinéraires; composants communs utilisés aussi par Accueil et Signaler.
- Radars : compteur désormais aligné sur les points locaux valides, non expirés et visibles selon le filtre.
- Itinéraires : état clairement différencié entre attente, destination prête et trajet calculé; données affichées issues du parcours existant.
- Paramètres/Apparence : résumé dynamique en quatre pastilles et hiérarchie des cartes harmonisée.
- Progression estimée du code : global 44→46 (+2); architecture visuelle 28→34 (+6); écrans détaillés +20 points cumulés. Ces valeurs ne remplacent pas une compilation ni une validation sur téléphone.
- Contrôles statiques et tests Python ciblés : voir `TESTS-ET-BLOCAGES.md`.
- APK non compilée; rendu Xiaomi non vérifié. La caméra et le moteur GPS restent hors de ce lot.
