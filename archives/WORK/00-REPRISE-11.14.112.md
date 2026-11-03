# Reprise ROADGUARD 11.14.112

## Modification substantielle du cockpit
- Barre « Où va-t-on ? » toujours visible au bas de la carte, même repliée.
- Panneau développé : favoris, domicile et travail. Interaction en deux états via glissement/toucher.
- Rail vertical aligné à droite : orientation de la carte, signaler un danger, recentrer le GPS; zooms avancés optionnels.
- Vitesse déplacée/taillée comme pastille de navigation en bas à gauche, au-dessus du panneau.
- Itinéraire en rouge Roadguard sur les moteurs disponibles; GPS bleu préservé.

## Tests / limites
- Contrôles automatiques statiques, scripts des trois vues et tests Python indiqués dans TESTS-ET-BLOCAGES.md.
- APK non compilée. Affichage et interaction doivent être essayés sur Xiaomi après compilation par l’utilisateur.
- Carte raster : impossible de recolorer indépendamment toutes les routes; style vectoriel possible seulement avec pack et source compatibles.

## Reste ensuite
- Comparer la prochaine capture Xiaomi aux nouvelles captures de référence et corriger dimensions/placement.
- Harmoniser les écrans Accueil, Alertes, Signaler, Trajet et Paramètres avant toute validation finale du graphisme.
- Caméra en dernier, selon la consigne de projet.
