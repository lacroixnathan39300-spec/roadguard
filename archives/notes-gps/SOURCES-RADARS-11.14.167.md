# ROADGUARD 11.14.167 — recherche et intégration des radars France/Suisse

Assemblage réalisé le 16 octobre 2026 à partir de la version 11.14.166. Tous les comptes ci-dessous sont mesurés dans les CSV fournis. Ils ne constituent ni une mesure de couverture des radars réels, ni une confirmation de fonctionnement.

## Résultat livré

| Pays | Points après fusion | Inventaire de dispositifs permanents | Lieux historiques non confirmés | Usage dans ROADGUARD |
|---|---:|---:|---:|---|
| France | 24,429 | 7,325 | 17,104 | Inventaire chargé automatiquement; temporaires/chantiers en option, désactivée par défaut |
| Suisse | 1,777 | 1,583 | 194 | Carte et alertes GPS; 1777 points CH sélectionnés par défaut, lieux mobiles non confirmés réglables séparément |

Le premier lancement ne demande aucun téléchargement de ces bases. Les téléchargements et imports déjà proposés restent disponibles. Le lecteur et le sélecteur des données France/Suisse sont communs à l’activité et au service GPS. Par défaut, 7325 points FR et 1777 points CH sont sélectionnés, soit 9102 points. Les 194 lieux mobiles CH ont un réglage indépendant, activé par défaut; leur présence est toujours non confirmée. Les inventaires sont mis en cache en mémoire pour éviter de relire les CSV à chaque rafraîchissement du service.

La version 11.14.166 embarquait déjà les deux inventaires fusionnés, mais excluait CH de la carte et des alertes. La version actuelle active ces données sans modifier leurs coordonnées. Le socle officiel français initial comportait 3309 entrées. Les 3309 identités officielles restent présentes. Par rapport à ce seul socle, le catalogue France offre 4016 points d’inventaire supplémentaires après fusion, plus les lieux historiques; ces sources plus anciennes peuvent contenir des observations supprimées depuis.

## Sources réellement récupérées

Le tableau compte les observations des sources avant leur fusion. Ces colonnes ne doivent jamais être additionnées pour annoncer un nombre de radars.

| Source | Observations FR | Observations CH | Date des observations / export | Droits et résultat |
|---|---:|---:|---|---|
| [Ministère de l’Intérieur / data.gouv.fr](https://www.data.gouv.fr/datasets/liste-des-radars-fixes-en-france) | 3309 | — | Publication 30/12/2025 | Licence Ouverte 2.0; sept types officiels, toutes les identités préservées |
| [Lufop et contributeurs](https://lufop.net/full-access/) | 20592 | 865 | Fichiers exportés le 01/10/2026, archives fournies | ODbL 1.0; CSV/ASC/POI-Warner comparés, catégories détaillées conservées |
| [MapAtlas Camera Index](https://mapatlas.eu/tools/speed-camera-map) | 3853 | 638 | Snapshot 21/05/2024; catalogue publié 27/08/2026 | CC BY 4.0; fixes, feux, vitesse moyenne et limitations variables |
| [OpenStreetMap via SpeedCams.world](https://speedcams.world/downloads) | 3822 | 607 | Date de snapshot inconnue, comme indiqué par le fournisseur | ODbL 1.0; une observation plus ancienne du même nœud est remplacée par l’extraction directe CH |
| [OpenStreetMap via Overpass Suisse](https://overpass.osm.ch/api/interpreter) | — | 1045 | Extraction 16/10/2026; numéros de réplication renvoyés sans date ISO interprétable | ODbL 1.0; nœuds et relations de contrôle, rôles des tronçons interprétés |

La date d’assemblage ou de récupération ne rajeunit pas les observations des sources. Aucune date de validation terrain n’est inventée.

## Références complémentaires et sources étudiées

- [SITG / État de Genève — socles de radars](https://sitg.ge.ch/donnees/otc-sl-reseau-radar) : 41 socles matériels récupérés; données de niveau A libre, source et date d’extraction indiquées. Le fichier séparé `radar_reference_geneva.geojson` reste exclu des 1777 points radar CH et des alertes. La présence d’un socle ne prouve pas celle d’un radar actif. [Conditions SITG](https://sitg.ge.ch/ressources/conditions-utilisation-donnees).
- [swisstopo via geoBoundaries CHE ADM0](https://www.geoboundaries.org/api/current/gbOpen/CHE/ADM0/) : frontière représentée en 2022, 51824 sommets. Sert au contrôle des étiquettes de pays; aucune conversion d’une simple fenêtre rectangulaire en « radars suisses ».
- [CoPilot fourni](https://lufop.net/) : les 20592 observations FR du fichier OV2 sont toutes corroborées à moins de 3 m par les exports Lufop. Aucun point supplémentaire. L’archive ne contient pas de licence autonome; elle sert de contrôle croisé, sans ajout de données non corroborées.
- POI-Warner fourni : les quinze CSV ont exactement les mêmes lignes que Lufop, après tri. Ils ne contiennent pas une base suisse supplémentaire. Leur ordre différent ne multiplie plus les observations.
- [SCDB](https://www.scdb.info/fr/plugin-csv/) : base supplémentaire repérée mais accès payant; aucune base obtenue ni droit de redistribution fourni. Non incorporée.
- [Open GATSO POI](https://www.data.gouv.fr/datasets/open-poi-stations-services-radars) : agrégateur de sources déjà étudiées; fiche data.gouv datée de 2018 avec licence non renseignée. Non incorporé comme source indépendante de droits ou de fraîcheur.
- [Dépôt mrx23dot/speedcameras](https://github.com/mrx23dot/speedcameras) : CSV mondial trouvé, mais absence de licence explicite et provenance non étayée. Non incorporé.
- [Compteurs de trafic suisses](https://data.opentransportdata.swiss/en/dataset/trafficcounters) et [compteurs zurichois](https://data.stadt-zuerich.ch/dataset/geo_standorte_der_verkehrszaehlungen_miv) : ils mesurent le trafic; ils ne sont pas intégrés comme radars verbalisateurs.
- Les extractions France depuis les instances Overpass essayées ont échoué (erreurs serveur ou délai dépassé). Le snapshot FR existant est conservé avec date inconnue; aucune extraction France récente n’est revendiquée.

## Catégories conservées

| Catégorie | France | Suisse |
|---|---:|---:|
| Fixe | 3914 | 1007 |
| Discriminant | 562 | 0 |
| Tourelle | 1492 | 0 |
| Urbain | 27 | 0 |
| Feu rouge | 963 | 523 |
| Vitesse moyenne (point de référence) | 132 | 5 |
| Tronçon début | 61 | 2 |
| Tronçon fin | 61 | 2 |
| Passage à niveau | 83 | 0 |
| Voie covoiturage | 29 | 0 |
| Limitation variable | 1 | 11 |
| Fixe en tunnel | 0 | 33 |
| Lieu mobile signalé, présence non confirmée | 0 | 194 |
| Lieu temporaire possible, présence non confirmée | 15689 | 0 |
| Lieu de chantier signalé, présence non confirmée | 1415 | 0 |

Les extrémités d’un tronçon sont des points distincts, pas deux radars fixes supplémentaires. Un tunnel décrit le site d’un dispositif, pas une garantie de type ou de fonctionnement. Les limites de vitesse et orientations ne sont conservées que lorsqu’elles figurent dans une source; leurs divergences restent dans le CSV.

## Méthode et traçabilité

1. Validation des coordonnées, catégories et pays; lecture du format OV2 sans deviner les positions.
2. Suppression des doublons d’exports après tri des lignes. **31 fichiers identiques** ignorés, malgré des formats ou ordres différents.
3. **571 observations OSM plus anciennes** remplacées par le même identifiant de nœud dans l’extraction directe suisse.
4. Isolement de **18 observations étiquetées FR dans le polygone suisse** et **43 observations étiquetées CH en dehors**. Ce sont des observations, pas un total de dispositifs distincts. Elles sont conservées dans `quarantined_country_labels.json`, sans réaffectation silencieuse à un autre pays.
5. Fusion de catégories compatibles à **25 m entre sources**, et à **2 m dans une même famille d’exports** pour gérer les différences de précision. Les directions opposées connues, catégories différentes, appareils officiels distincts et extrémités de tronçon sont préservés.
6. **7893 observations recoupées** par cette fusion. Le nombre final de points reste dépendant de ces règles : des radars voisins ou déplacés peuvent conserver plusieurs points, et d’anciens points peuvent être obsolètes.
7. Identifiant stable de point, identifiants des sources, provenance, catégorie, vitesse connue, divergences et empreinte SHA-256 exportés. Aucune clé API ni donnée personnelle intégrée.

Les deux CSV, le manifeste, les copies sources et le script hors réseau sont fournis. Reconstruction : `python3 tools/build_embedded_radars.py` depuis le dossier du projet. Le résultat est déterministe à sources identiques. Les enregistrements d’infrastructure SITG restent dans leur collection séparée.

## Contrôles réels et limites

- Contrôle des empreintes, pays, coordonnées, identités uniques, catégories, provenance et séparation des données historiques.
- Préservation vérifiée des 3309 identités officielles.
- Exécution Java du véritable lecteur de l’application sur les deux CSV : 24429 points FR, dont 7325 inventaire / 17104 historiques; 1777 points CH. Rejet vérifié d’un mélange de pays, d’identités répétées et de coordonnées non finies.
- Tests du dédoublonnage sur des appareils proches, des directions opposées et des catégories différentes; protection contre la perte de tags OSM lors de la récursion.
- Sources Java vérifiées avec ECJ 3.39.0, les classes Android API 34 et le runtime Java disponible. Ce contrôle ne constitue pas un assemblage APK; aucun Gradle/dex/packaging/signature ni essai Xiaomi effectué.
- Tests de régression sur les radars, filtres, imports existants, GPS et exploration manuelle de la carte; syntaxe des scripts des deux moteurs cartographiques vérifiée. Résultats exacts dans `WORK/VALIDATION-11.14.167.md`.

## Conditions des données

La base dérivée est proposée sous **ODbL 1.0**, avec les attributions et conditions d’origine conservées; les CSV et sources nécessaires à la reconstruction sont disponibles dans ce ZIP. Les données publiques ne garantissent ni exhaustivité, ni actualité, ni autorisation d’un usage donné. Les positions mobiles/temporaires ne confirment aucune présence actuelle.

Liens des licences : [ODbL 1.0](https://opendatacommons.org/licenses/odbl/1-0/), [OpenStreetMap](https://www.openstreetmap.org/copyright), [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/), [Licence Ouverte](https://www.etalab.gouv.fr/licence-ouverte-open-licence/), [SITG](https://sitg.ge.ch/ressources/conditions-utilisation-donnees).

## Activation Suisse en 11.14.167

Les deux CSV et leurs comptes restent identiques à 11.14.166. La carte et le service GPS chargent désormais CH par le même sélecteur. Les bases supplémentaires MapAtlas, OSM et Lufop CH sont aussi utilisables, avec un même réglage pour les lieux mobiles et des libellés de présence non confirmée. L’ancien paquet `poi_warner_ch`, construit à partir de libellés FR et d’un rectangle géographique, reste écarté tant que son pays n’est pas vérifié. Les 41 socles SITG restent une référence d’infrastructure, pas des radars activés.

Les réglages globaux des marqueurs, sons et voix sont conservés. Le service d’arrière-plan reste à activer depuis l’application avec les autorisations GPS Android; ses réglages sont relus périodiquement, au plus tard après 30 secondes lors d’un fix GPS.

L’[OFROU](https://www.astra.admin.ch/fr/appareils-gps) indique que les avertisseurs de contrôles routiers sont interdits dans un véhicule en Suisse. L’activation technique demandée ne constitue pas une autorisation d’usage.
