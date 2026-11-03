# Validation ROADGUARD 11.14.171

- Comparaison visuelle des deux captures 11.14.170 aux écrans Navigation jour/nuit de la maquette validée; constats et choix dans la note de reprise. Il s’agit de captures de la version antérieure.
- `tools/test_reference_navigation.py` : contrôle du Menu, de l’accès Avancement, de Signaler central, du panneau compact et du trajet bleu sur les trois moteurs; GPS bleu préservé.
- 15 groupes Python de navigation visuelle, carte, GPS, radars FR/CH et passation ont réussi, dont `tools/test_visual_handoff_current.py` et `tools/test_maplibre_local_startup.py`.
- Les adaptateurs Node de caméra Leaflet/MapLibre/Canvas et de pictogrammes ont réussi; syntaxe des scripts des cartes vérifiée par Node. Java vérifié avec ECJ/Android API 34 sans erreur. Aucun assemblage APK/dex/signature.

Limites : disposition de 11.14.171 non rendue dans une WebView Xiaomi. La maquette représente une navigation guidée et des données de vitesse fictives; aucun affichage sans donnée réelle. Carte en ligne, cache, bases FR/CH et alertes inchangés.
