# ROADGUARD 11.14.261 — approche radar sonore

Ce dossier contient l'application Android complète. Il succède à 11.14.260 et conserve son correctif de rechargement radar hors du fil principal.

À partir de 1 km d'un radar pertinent devant le véhicule, le bip normal se répète environ toutes les 2,2 secondes jusqu'à ce que le radar ne soit plus pertinent. Si une limite fiable est connue pour le radar et que la vitesse GPS filtrée la dépasse, un double bip plus fort se répète environ toutes les 1,4 secondes. Une limitation inconnue ou un GPS de vitesse indisponible ne provoque jamais le double bip. La répétition s'arrête si les observations GPS datent de plus de cinq secondes, si les alertes sont désactivées ou si le radar sort du secteur/direction. Le service en arrière-plan conserve le même comportement, sans deuxième flux sonore lorsque la carte est visible.

La voix annonce le radar aux paliers 1 km, 750 m, 500 m, 300 m, 250 m, 200 m, 150 m, 100 m et 50 m. La distance prononcée est la mesure GPS arrondie au moment du franchissement, qui peut sauter un palier si la position arrive tardivement. Il n'y a pas de garantie de voie ou de sens de circulation à partir des seules coordonnées d'un radar ; la projection sur l'itinéraire et le cap fiable restent utilisés lorsqu'ils sont disponibles.

Tests ici : compilation JVM ciblée et exécution TestAlertPolicies, contrôle du radar mobile temporaire, analyse syntaxique des 68 sources Android et 18 sources de tests. Aucun SDK Android ni essai sur le téléphone : cadence et intelligibilité sonore à valider en conditions réelles.

🔴 INTERVENTION OBLIGATOIRE — APRÈS INSTALLATION : compiler ce ZIP dans CodeAssist, tester à pied ou comme passager avec la carte puis le mode en arrière-plan, et confirmer le bip normal, le double bip seulement avec une limite connue et une vitesse GPS supérieure, ainsi que les annonces jusqu'à 50 m. Ne pas manipuler l'écran en conduisant. Si l'import des radars fait encore geler l'application, transmettre le journal « Voir l'historique ».
