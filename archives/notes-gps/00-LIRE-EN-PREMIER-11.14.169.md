# ROADGUARD 11.14.169 — cadrage de la caméra vectorielle

Base 11.14.168, versionCode 369. Projet source pour CodeAssist; pas d’APK assemblé dans cet environnement.

Le retour utilisateur indique que la version précédente fonctionne sur téléphone. Cette version ajuste le suivi de la carte vectorielle MapLibre : la position GPS se place au-dessus du panneau de trajet, sa hauteur est recalculée quand le panneau change, et l’aperçu de l’itinéraire laisse une marge en bas. Après déplacement manuel ou aperçu de l’itinéraire, l’exploration reste libre pendant les mises à jour GPS et les changements de style. Le bouton ⌖ reprend le suivi à la dernière position, avec anticipation du trajet.

Les cartes Leaflet et locale, les pictogrammes, la navigation, les CSV FR/CH, les réglages et les alertes restent sur leur comportement précédent. Données par défaut : 7325 FR + 1777 CH = 9102 points, dont 194 lieux mobiles CH sans confirmation de présence actuelle.

Les essais adaptateurs et de régression sont détaillés dans WORK/VALIDATION-11.14.169.md. Ils ne reproduisent pas la WebView du Xiaomi.

**INTERVENTION OBLIGATOIRE :** importer le ZIP 11.14.169 dans CodeAssist et compiler. Sur Xiaomi, afficher la carte vectorielle, déplacer la carte à la main, changer jour/nuit, vérifier qu’elle ne revient pas seule au GPS; toucher ⌖ pour reprendre. Ouvrir et agrandir le panneau de trajet et vérifier que le point bleu reste visible; vérifier l’aperçu de l’itinéraire.

Le suivi global reste à 94 % (+0 point), estimation historique non auditée.
