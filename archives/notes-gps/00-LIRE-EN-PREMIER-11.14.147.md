ROADGUARD 11.14.147 · RADARS PAR CATÉGORIE

Projet Android à ouvrir dans CodeAssist. Version source, pas un APK.

Changements : filtres sur les radars des bases chargées; marqueurs différenciés sur la carte; alertes nommant le type; détection des tunnels quand la source les nomme ou fournit le tag `tunnel`; listes des radars les plus proches dans un rayon de 50 km.

Filtres : Tous, Fixes, Mobiles, Tunnels, Feux, Tronçons, Variables, Zones, Passages à niveau et Chantiers. Les points filtrés et leurs compteurs viennent des bases actuellement chargées.

Le tunnel est un attribut descriptif fourni par la base, pas une catégorie universelle : si le fournisseur ne l’indique pas, ROADGUARD ne peut pas le déduire.

Compilation : ouvrir le projet puis reconstruire l’APK avec CodeAssist. L’environnement de cette session ne contient pas Gradle/Android SDK/javac; la compilation et l’essai du téléphone restent à faire.
