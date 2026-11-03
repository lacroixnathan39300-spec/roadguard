# ROADGUARD 11.14.173 — maquette avant compilation

Référence : `design/reference-validee/ROADGUARD-MAQUETTE-PREMIUM-10-ECRANS.png`. Nathan demande la ressemblance avant de compiler. La version 11.14.172 n’a pas été demandée en compilation. Cette source 11.14.173 est une reprise étendue des dix compositions, sans demande de compilation à l’utilisateur à ce stade.

La carte garde les pictogrammes vectoriels et le regroupement de 11.14.172. Lorsqu’un itinéraire existe réellement, le bandeau de manœuvre prend place en haut, avec le trajet bleu et un résumé de durée, distance et heure d’arrivée en bas. Sans trajet, la recherche de destination reste accessible. Huit écrans secondaires utilisent une présentation locale graphite/rouge : accueil illustré, itinéraire, signaler, radars, alerte, cache, paramètres, apparence. Les boutons passent par un pont d’actions Android limité et les écrans avancés d’origine restent accessibles.

Le fond d’accueil `app/src/main/assets/roadguard_home_scene.png` a été créé pour le projet avec l’outil ImageGen intégré. Prompt final : voiture graphite vue de l’arrière sur route de montagne au crépuscule, feux rouges, palette noire et rouge, espaces sombres réservés au logo et aux menus, sans texte ni interface. Les autres pictogrammes proviennent des SVG locaux ou sont dessinés en SVG natif dans l’écran Signaler.

| Panneau de référence | Version source | Données / limite |
|---|---|---|
| Accueil | Visuel voiture, logo et quatre cartes d’accès | GPS réel, accès carte/radars/cache/paramètres. |
| Navigation jour | Carte claire, route bleue, bandeau et résumé conditionnels | Manœuvre et durée uniquement avec un trajet calculé. |
| Navigation nuit | Même disposition en carte sombre | Préférence nuit ou automatique selon le thème du système au démarrage. |
| Alerte radar | Anneau rouge, type/distance, confirmation | Limite affichée seulement si la base contient une valeur. |
| Signaler | Grille et bouton de confirmation | Signalement enregistré localement; pas de communauté fictive. |
| Itinéraire | Départ/destination, parcours réels sélectionnables | Pas de péages évités artificiellement si le serveur ne le permet pas. |
| Cartes hors ligne | Présentation du cache temporaire | Pas de pays à télécharger, conformément au choix plus récent. |
| Radars | Onglets, liste proche et distances réelles | Emplacements mobiles non confirmés. |
| Paramètres | Commutateurs principaux, thème et options | Réglages avancés par « Plus de fonctions ». |
| Apparence | Trois tuiles de thème, accents, styles, bascules | Satellite/3D et autres accents indisponibles, indiqués comme tels. |

| Module | Estimation | Δ / 11.14.172 | Limite |
|---|---:|---:|---|
| GPS bleu | 100 % | +0 pt | Confirmé fonctionnel par utilisateur sur une APK antérieure; nouvelle APK non testée |
| Carte en ligne MapLibre | 90 % | +0 pt | Zoom utilisateur suspend le suivi; changements de style et panneau de trajet déjà corrigés. Vérification WebView/Xiaomi requise. |
| Cache normal des tuiles consultées | 35 % | +0 pt | WebView en ligne utilise son cache HTTP habituel; tuiles visibles précédemment susceptibles de rester disponibles. Durée/couverture non garanties; essai appareil et coupure réseau à faire. |
| APK et tests Xiaomi | 10 % | +0 pt | 11.14.172 non demandée en compilation; 11.14.173 vérifiée en source Java/JS seulement. Aucun APK 11.14.173. |
| Navigation | 100 % | +0 pt | Implémentation navigation comptée à 100 % côté code : recherche et géocodage, itinéraires alternatifs, sauvegarde/reprise, instructions OSRM, projection GPS sur segments, progression monotone, recalcul prudent, anticipation/cap et guidage vocal présents. Compilation et essai Xiaomi explicitement reportés à l’étape suivante. |
| Radars | 99 % | +0 pt | Activation FR/CH de 11.14.167 conservée. Cette version change uniquement les pictogrammes cartographiques; 7325 FR + 1777 CH par défaut, présence mobile non confirmée. |
| Caméra | 70 % | +0 pt | Suivi, exploration manuelle, zoom, recentrage et dégagement du panneau alignés sur Leaflet, MapLibre et carte locale. Tests de fonctions réelles avec adaptateurs; fluidité en conduite et APK restent à confirmer. |
| Interface | 80 % | +0 pt | Huit vues secondaires recomposées et carte jour/nuit adaptée; tests source réussis, aucune capture Xiaomi 11.14.173. Ressemblance exacte non établie. |
| Architecture visuelle premium | 100 % | +0 pt | Typographie, groupes, tuiles, cartes, raccourcis et onglets harmonisés en 11.14.116; rendu réel à vérifier après build. |
| **Global** | **94 %** | **+0 pt** | Estimation historique non auditée. |

Chantier actif : validation visuelle sur un rendu Android des dix panneaux et retouches des écarts avant toute compilation demandée à l’utilisateur. Les pourcentages restent des estimations historiques non auditées; interface 80→80 % et global 94→94 % faute d’une validation Xiaomi. L’identité pixel par pixel n’est pas établie par les contrôles de source.

Aperçu autonome : `design/previews/ROADGUARD-11.14.173-APERCU.html` (données d’exemple, hors APK). Il montre les écrans secondaires avant toute compilation, mais ne remplace pas un rendu WebView Xiaomi.

Aucune intervention de Nathan demandée maintenant. La compilation reste en attente de la vérification de ressemblance.
