# ROADGUARD 11.14.162 — ZIP Lufop ASC et carte libre

Projet Android source à importer dans CodeAssist; aucun APK compilé.

- **Radars** : l’import Lufop accepte maintenant les archives CSV et ASC. Comparaison des deux fichiers fournis : 865 lignes suisses valides, 863 positions uniques (408 fixes, 229 feux rouges, 194 mobiles, 32 en tunnel). L’ASC n’ajoute aucun emplacement par rapport au CSV. Les points suisses restent stockés sur le téléphone et exclus de la carte et des alertes.
- **Carte** : glisser ou zoomer la carte active l’exploration libre persistante; le GPS actualise toujours le marqueur sans ramener la caméra au point GPS. Appuyer sur ⌖ recentre et reprend le suivi.

Avancement estimé : global 93 % (+1), radars 99 % (+1), carte 87 % (+1), navigation 100 % côté code. Aucune compilation Android.
