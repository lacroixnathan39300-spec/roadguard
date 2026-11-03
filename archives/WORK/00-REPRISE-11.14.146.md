# Reprise ROADGUARD 11.14.146

- Écran corrigé d’après la capture : le titre est « Choisir un pays · bases radar » et chaque pays affiche MapAtlas, Lufop Radardroid et OSM.
- Toucher un pays ouvre la liste de fournisseurs; Lufop Radardroid télécharge la base directement par pays sans clé API.
- Compilation et test sur le Xiaomi restent à faire.

## Vérification
1. Compiler version 11.14.146 / code 346 dans CodeAssist.
2. Alertes → Bases radar → choisir un pays; vérifier que MapAtlas, Lufop Radardroid et OSM sont lisibles.
3. Choisir Lufop Radardroid pour la France, puis tester un autre pays.
4. Vérifier compteurs, alertes, annulation, mise à jour/suppression; aucune carte hors ligne permanente ni préchargement.
