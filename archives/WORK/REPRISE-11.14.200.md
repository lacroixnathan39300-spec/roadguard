# ROADGUARD 11.14.200 — Suivi visible et protection des radars

## Changements
- Panneau rouge INTERVENTION OBLIGATOIRE dans le tableau de suivi pour la compilation et les tests.
- Encadré de protection des bases radar intégrées sur l'écran d'installation.
- Barre d'avancement réelle du nombre de bases supplémentaires installées (sur 7 pays de référence), sans confondre avec la couverture radar.
- Panneau rouge si un téléchargement radar est en cours.

## Bases intégrées conservées
FR, CH, DE, BE, ES, IT, LU : fichiers `app/src/main/assets/radars_embedded_*.csv` inchangés.
Les bases mises à jour restent dans le stockage privé de l'application. Le repli sur les bases intégrées est préservé.

## Validation
- Modification statique du code effectuée.
- Vérification présence et empreintes des 7 bases effectuée.
- Compilation APK : non effectuée.
- Test Xiaomi et Android Auto : non effectués.
- Avancement global : non réévalué sans test, les anciennes estimations restent historiques.

## Intervention obligatoire
Compiler sur CodeAssist et tester le tableau de suivi et l'écran Installer les radars. Signaler les erreurs de compilation et captures d'écran.

## À faire
- Progression en octets des téléchargements réseau (les opérations existantes ont un indicateur indéterminé).
- Intégrer les sources supplémentaires autorisées, valider leurs licences et leurs formats.
- Tester en conditions réelles, sans retirer les bases d'origine.
