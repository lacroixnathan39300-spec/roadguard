# ROADGUARD 11.14.163 — import POI-Warner séparé

Projet Android source à importer dans CodeAssist; aucun APK compilé.

Un nouveau bouton **Importer un ZIP POI-Warner · Suisse** lit uniquement les CSV de radars listés, filtre les coordonnées dans l’emprise géographique suisse et retire les doublons. Le ZIP fourni contient 287 positions uniques dans un rectangle géographique autour de la Suisse, susceptible d’inclure les régions frontalières voisines. Les familles source comprennent fixes avec limitation, feux rouges, chantiers, débuts/fins de tronçon, passages à niveau et covoiturage.

Attention : les noms et libellés du jeu sont FR et le ZIP ne précise ni licence ni date de mise à jour. L’import est conservé sous une entrée séparée, marqué non vérifié et exclu de la carte et des alertes ROADGUARD. Il ne se mélange pas à Lufop.

Lufop CSV/ASC et la carte libre jusqu’au recentrage restent inclus. Avancement : global 93 % (+0), radars 99 % (+0), carte 87 % (+0), navigation 100 % côté code. Compilation et essais appareil restent à faire.
