# ROADGUARD 11.14.172 — carte lisible, maquette de référence

La capture Xiaomi 11.14.171 confirme la nouvelle navigation basse et le bouton Signaler central, mais montre des centaines d’icônes qui masquent entièrement la carte. La maquette validée des dix écrans (`design/reference-validee/ROADGUARD-MAQUETTE-PREMIUM-10-ECRANS.png`) reste la cible visuelle. Cette version traite le défaut de densité des marqueurs de la carte; elle ne constitue pas encore une reproduction identique des dix écrans.

Sur Leaflet, MapLibre et la carte locale, les points proches à l’écran sont représentés par un cercle rouge foncé portant leur nombre. Sur Leaflet et MapLibre, appuyer sur le groupe agrandit la carte; les pictogrammes vectoriels individuels réapparaissent dès qu’ils sont séparés. À zoom maximal, le groupe indique le nombre de points. L’inventaire FR/CH, la sélection historique et les alertes GPS ne changent pas. Le cercle mesure les points présents dans la zone de carte, pas des radars actifs confirmés.

La navigation basse affiche « Menu », « Alertes », « Signaler », « Itinéraire » et « Options » conformément à la maquette. Le suivi de projet montre désormais aussi les diminutions estimées. Les couleurs et le trajet bleu de 11.14.171 restent. L’affichage d’une manœuvre, de la route et de la vitesse dépend d’un trajet réel et des données disponibles. Une comparaison des dix écrans sur Xiaomi sera nécessaire pour ajuster les écarts de la maquette.

| Module | Estimation | Δ / 11.14.171 | État / limite |
|---|---:|---:|---|
| GPS bleu | 100 % | +0 pt | Confirmé fonctionnel par utilisateur sur une APK antérieure; nouvelle APK non testée |
| Carte en ligne MapLibre | 90 % | +0 pt | Zoom utilisateur suspend le suivi; changements de style et panneau de trajet déjà corrigés. Vérification WebView/Xiaomi requise. |
| Cache normal des tuiles consultées | 35 % | +0 pt | WebView en ligne utilise son cache HTTP habituel; tuiles visibles précédemment susceptibles de rester disponibles. Durée/couverture non garanties; essai appareil et coupure réseau à faire. |
| APK et tests Xiaomi | 10 % | +0 pt | 11.14.171 installée et capturée par l’utilisateur; 11.14.172 non assemblée ni essayée sur Xiaomi. |
| Navigation | 100 % | +0 pt | Implémentation navigation comptée à 100 % côté code : recherche et géocodage, itinéraires alternatifs, sauvegarde/reprise, instructions OSRM, projection GPS sur segments, progression monotone, recalcul prudent, anticipation/cap et guidage vocal présents. Compilation et essai Xiaomi explicitement reportés à l’étape suivante. |
| Radars | 99 % | +0 pt | Activation FR/CH de 11.14.167 conservée. Cette version change uniquement les pictogrammes cartographiques; 7325 FR + 1777 CH par défaut, présence mobile non confirmée. |
| Caméra | 70 % | +0 pt | Suivi, exploration manuelle, zoom, recentrage et dégagement du panneau alignés sur Leaflet, MapLibre et carte locale. Tests de fonctions réelles avec adaptateurs; fluidité en conduite et APK restent à confirmer. |
| Interface | 80 % | -5 pt | Capture Xiaomi 11.14.171 : commandes basses présentes, mais centaines de pictogrammes occultent la carte. Regroupement implémenté sur les trois cartes, à confirmer après compilation. |
| Architecture visuelle premium | 100 % | +0 pt | Typographie, groupes, tuiles, cartes, raccourcis et onglets harmonisés en 11.14.116; rendu réel à vérifier après build. |
| **Global** | **94 %** | **+0 pt** | Estimation historique non auditée; fidélité visuelle distincte : 80 %. |

Chantier actif : comparaison de l’écran Carte et des neuf autres écrans à la maquette après compilation; corriger ensuite les écarts mesurés. Estimations historiques non auditées; interface 85→80 % (−5 pts après capture de la saturation), global 94→94 % (+0 pt).

**INTERVENTION OBLIGATOIRE — APRÈS LA LIVRAISON :** importer le ZIP 11.14.172 dans CodeAssist, compiler, installer sur Xiaomi, puis comparer la carte à la maquette jour/nuit. Vérifier à zoom large les compteurs, leur ouverture au toucher, les pictogrammes à zoom rapproché et une alerte GPS réelle. Envoyer une capture sans trajet et une avec trajet pour régler les écarts restants.
