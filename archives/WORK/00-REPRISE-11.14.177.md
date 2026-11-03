# ROADGUARD 11.14.177 — radars et personnalisation

Base : version 11.14.176, projet enregistré version 38. Aucun changement du site V3, du moteur cartographique, de la caméra, des fréquences de partage ou des comptes.

## Changements

- Bases radar : nouvel écran premium, accès depuis le menu, la liste des alertes et les paramètres. France et Suisse déjà incluses, points permanents séparés des lieux mobiles possibles.
- Un bouton vérifie l’inventaire partagé : empreinte et nombre de points vérifiés par le mécanisme existant, nouvelle version téléchargée si nécessaire, données conservées en cas d’échec. Aucun compte nécessaire.
- Autres pays : pays puis confirmation de téléchargement MapAtlas avec source/licence et accès à toutes les sources existantes. Aucun téléchargement de carte France.
- Curseur : point GPS rendu transparent quand la flèche ou la voiture est choisie; le choix Point conserve un point de la couleur choisie. Cercle de précision de la même couleur. La position et l’interpolation GPS restent actives.
- Accent appliqué aux commandes natives principales : bouton Signaler, compteur, recherche, commandes flottantes et menu. Les couleurs de danger et les pictogrammes de sécurité restent sémantiques.
- Accès direct Apparence et voix dans le menu. 6 accents, 4 couleurs de tracé, 3 curseurs, Jour/Nuit/Auto, fond de menu illustré activable. Voix françaises disponibles sur le téléphone, hors variantes CA; aucun timbre féminin garanti si le moteur installé n’en propose pas.

## Tests et limites

Voir VALIDATION-11.14.177.md et validation-11.14.177.json pour les résultats finaux. Les scripts JS s’exécutent dans une VM Node; ils ne constituent pas un essai sur Android ni une comparaison visuelle pixel par pixel. Aucun émulateur ni Xiaomi accessible ici.

La vérification de l’inventaire ne prouve pas une nouvelle présence de radar : les points sont des emplacements connus. Les bases publiques ne garantissent pas une couverture exhaustive. Aucun trafic de charge créé; 500 000 utilisateurs simultanés restent non validés.

## Actions utilisateur, à la fin

🔴 INTERVENTION OBLIGATOIRE — installer l’APK si la signature correspond ou compiler les sources avec la signature habituelle CodeAssist. Ne pas désinstaller pour contourner un conflit de signature : préserver les données existantes.

1. Menu → Installer les radars → Vérifier les mises à jour France + Suisse. Tester aussi sans réseau : les bases doivent rester disponibles et l’échec doit être indiqué.
2. Menu → Apparence et voix : changer l’accent puis choisir Flèche, Voiture et Point. Flèche/Voiture : aucun petit point bleu dessous. Point : couleur choisie visible.
3. Choisir la voix dans la liste et écouter l’échantillon. Les voix dépendent du moteur installé sur le téléphone.
