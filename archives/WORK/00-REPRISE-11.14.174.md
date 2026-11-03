# ROADGUARD 11.14.174 — fond voiture au démarrage et dans le menu

Demande de Nathan : utiliser l’image fournie 144270.png à l’écran de démarrage et en fond de menu. Base : archive 11.14.173, version Library 35.

## Réalisé

- Image fournie copiée sans changement d’octets dans roadguard_home_scene.png.
- Écran de démarrage natif : illustration, titre ROADGUARD et indicateur de chargement. L’image est affichée avant les opérations d’initialisation; la carte s’ouvre ensuite. Aucun temps d’attente artificiel ajouté.
- Accueil illustré : même image, dégradé réduit pour conserver le paysage et la voiture.
- Menu latéral : image recadrée proportionnellement à 70 % horizontal, voile sombre, cartes translucides et raccourci Accueil. Bouton de fermeture désormais visible.
- Bitmap natif décodé une seule fois, partagé par démarrage et menu.

## Validation et limites

Compilation de toutes les sources Java en Java 8 avec Android API 34 via ECJ réussie. Contrôles existants de navigation visuelle, interface et passation réexécutés. Intégrité de l’image et des autres données d’assets vérifiée. Aucun APK assemblé ni essai Xiaomi 11.14.174.

L’aperçu HTML est une composition pour navigateur : accueil issu du HTML réel; démarrage et menu latéral sont une représentation du code natif, pas des captures Android. Le recadrage dépend du format d’écran. Le bref écran système Android précédant l’Activity dépend du téléphone.

Le navigateur de capture est indisponible dans cet environnement : aperçu HTML non rendu automatiquement.

Les corrections générales d’itinéraire, d’icônes de signalement et de fenêtres Favoris relevées dans les captures précédentes ne font pas partie de cette livraison centrée sur le fond.

## À faire / chantier actif

Vérifier le rendu Android des fonds et poursuivre la fidélité de l’interface. Aucune intervention ni compilation demandée maintenant. Les autres fonctions ne sont pas revalidées par ce changement visuel.

| Module | Estimation héritée | Δ / 11.14.173 |
|---|---:|---:|
| GPS bleu | 100 % | +0 pt |
| Carte en ligne MapLibre | 90 % | +0 pt |
| Cache normal des tuiles consultées | 35 % | +0 pt |
| APK et tests Xiaomi | 10 % | +0 pt |
| Navigation | 100 % | +0 pt |
| Radars | 99 % | +0 pt |
| Caméra | 70 % | +0 pt |
| Interface | 80 % | +0 pt |
| Architecture visuelle premium | 100 % | +0 pt |
| **Global historique non audité** | **94 %** | **+0 pt** |

Tous les pourcentages ci-dessus sont les estimations historiques non auditées de 11.14.173; aucune hausse n’est attribuée à cette retouche.
