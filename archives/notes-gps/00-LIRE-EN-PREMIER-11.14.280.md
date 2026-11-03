# ROADGUARD 11.14.280 — fiabilisation carte, réseau et suivi

## Changements intégrés

- Le moteur vectoriel signale son démarrage à Android. En cas d'échec ou de démarrage sans réponse après environ 15–16 secondes, une seule bascule vers la carte 2D est prévue. Le moteur vectoriel reste écarté pour cette session jusqu'à « Relancer la carte ». Les callbacks d'une ancienne WebView sont ignorés.
- Un changement de réseau pendant le chargement du style ne modifie plus prématurément les couches vectorielles. Ce cas d'erreur a été reproduit et corrigé avec un adaptateur événementiel.
- Les erreurs JavaScript et l'état du moteur sont consultables et copiables dans ⋯ > État GPS et radars. Le code ne transmet pas ce diagnostic à un service. Son format structuré n'inclut pas les coordonnées GPS.
- Hors ligne manuel reste prioritaire sur les événements réseau de la WebView. Android distingue la présence d'un réseau d'un accès Internet validé, notamment pour un Wi-Fi sans Internet. Les demandes réseau des autres fonctions de l'application ne sont pas désactivées par le mode carte.
- L'état réseau n'est envoyé à la page qu'au chargement et lors d'une transition, au lieu de chaque position GPS.
- Un ancien pack raster n'est plus superposé au fond en ligne. Le pack raster actif est ajouté lorsque la connexion cartographique est indisponible, puis retiré à son retour. Les zones sans pack ni cache restent indisponibles.
- Le zoom vectoriel réutilise l'hystérésis, la durée de stabilisation et la cadence minimale existantes. Une vitesse absente ne provoque plus un changement vers le zoom d'arrêt. Le suivi reste suspendu lors du déplacement manuel de la carte.
- Les polices locales sont déclarées dans le style raster MapLibre pour les nombres des regroupements radar. Les informations de tuiles Leaflet suivent la hauteur du panneau du haut et restent compactes.

Les modes Auto/En ligne/Hors ligne, l'écran allégé et le crédit ⓘ miniature de la version précédente sont conservés.

## Vérifications et limites

Voir verification/VALIDATION-11.14.280.md. Des erreurs reproductibles de chargement ont été corrigées ; la cause exacte du bandeau de la capture Xiaomi n'est pas établie sans diagnostic de cet appareil. Aucun APK, rendu WebGL réel ou essai en véhicule n'a été validé dans cet environnement.

## 🔴 INTERVENTION OBLIGATOIRE — étape bloquante

1. Importer ce ZIP dans CodeAssist, compiler et installer la mise à jour sans désinstaller ROADGUARD. Si la compilation échoue, transmettre le premier bloc d'erreur complet.
2. Ouvrir la carte en Auto. Vérifier qu'une carte apparaît, ou qu'un repli 2D se produit en cas d'échec du moteur vectoriel.
3. Sur une zone déjà consultée, activer le mode avion, puis le retirer. Tester aussi le mode Hors ligne avec le réseau actif.
4. Déplacer la carte à la main, attendre une position GPS, puis toucher Recentrer. Vérifier vitesse et orientation à l'arrêt ; les essais en mouvement doivent se faire sans manipuler le téléphone en conduisant.
5. Si un problème persiste : ⋯ > État GPS et radars > Copier diagnostic, puis joindre le texte et une capture de l'écran concerné.

Le rendu, le tactile, la localisation et le volume des alertes nécessitent l'appareil réel avant de poursuivre les réglages avec des résultats fiables.
