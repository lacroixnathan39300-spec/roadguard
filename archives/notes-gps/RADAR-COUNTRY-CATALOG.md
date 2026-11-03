# État courant — ROADGUARD 11.14.167

La Suisse est activée dans la carte et les alertes GPS au premier plan et en service. Les bases embarquées FR/CH sont chargées sans téléchargement initial : 7325 FR + 1777 CH par défaut. Les 194 lieux mobiles CH restent non confirmés, avec un réglage séparé. Les bases CH installées OSM, MapAtlas, Lufop et catalogues vérifiés sont aussi utilisables. L’ancien paquet poi_warner_ch, de territoire ambigu, reste en quarantaine.

L’OFROU indique que les avertisseurs de contrôles routiers sont interdits dans un véhicule en Suisse : https://www.astra.admin.ch/fr/appareils-gps. L’activation technique ne vaut pas autorisation d’usage.

Les paragraphes versionnés ci-dessous décrivent l’historique; toute mention d’une Suisse bloquée ou exclue est remplacée par cet état courant. Le lien d’export explicitement réservé à Radardroid reste retiré de ROADGUARD.

---

# Catalogue des bases radar par pays

ROADGUARD peut télécharger des bases radar nationales depuis une adresse HTTPS configurée dans **Alertes → Bases radar par pays**. Aucun fournisseur mondial n’est présumé : chaque entrée doit être publiée par un hébergeur autorisé, avec sa provenance et sa licence.

## Catalogue JSON

Publier un fichier JSON UTF-8 sous la forme suivante. `size` correspond à la taille exacte en octets du CSV; `sha256` à son empreinte hexadécimale SHA-256. Le champ `country` utilise un code pays ISO (par exemple `CH`, `DE`, `BE`). Une seule base nationale par code pays est acceptée.

```json
{
  "schema": 1,
  "packs": [
    {
      "id": "switzerland",
      "name": "Suisse",
      "country": "CH",
      "format": "roadguard-radar-csv-v1",
      "url": "https://exemple.org/radars/ch.csv",
      "size": 12345,
      "sha256": "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
      "attribution": "Organisme source, jeu de données et date",
      "license": "Licence et conditions d’usage"
    }
  ]
}
```

La fiche est refusée si l’URL n’est pas HTTPS, si la taille dépasse 32 Mio, si l’empreinte est absente ou si la source/licence ne sont pas renseignées. Le fichier doit correspondre exactement à la taille et à l’empreinte annoncées.

## CSV téléchargé

Le CSV est UTF-8, séparé par des points-virgules et commence par l’en-tête exact `id;type;lat;lon`. Chaque ligne valide contient un identifiant, un libellé classé comme radar par ROADGUARD, une latitude entre -90 et 90 et une longitude entre -180 et 180. Maximum : 100 000 lignes et 32 Mio par pays. Les lignes incorrectes sont ignorées; une base sans radar valide n’est pas installée.

Les fichiers sont installés dans l’espace privé de l’application. Ils alimentent les alertes sur la carte et le service GPS en arrière-plan, même sans réseau. La suppression d’un pays retire sa base locale et ses alertes.

Le fichier `catalogs/radar-country-catalog.example.json` est un modèle vide : il ne constitue pas un catalogue actif. Les conditions légales de collecte et de redistribution restent à confirmer pays par pays avant publication.

## Préparer le catalogue depuis des sources CSV

Le script `tools/build_radar_country_catalog.py` transforme un fichier source tabulaire vers le CSV attendu par Android et calcule taille et empreinte. Il prend un manifeste JSON dont les chemins `file` sont relatifs au manifeste :

```json
{
  "schema": 1,
  "packs": [{
    "id": "switzerland",
    "name": "Suisse",
    "country": "CH",
    "url": "https://radars.exemple.org/switzerland.csv",
    "attribution": "Source officielle, nom du jeu, date de collecte",
    "license": "Licence de redistribution",
    "file": "sources/switzerland.csv",
    "delimiter": ";",
    "columns": {"id": "ID", "type": "TYPE", "lat": "LATITUDE", "lon": "LONGITUDE"},
    "type_map": {"FIXED": "Radar fixe", "MOBILE": "Radar mobile"}
  }]
}
```

Génération : `python3 tools/build_radar_country_catalog.py manifest.json --out-dir publication/`. Le dossier produit contient `catalog.json` et les CSV normalisés. Héberger ces fichiers ensemble à l’URL HTTPS indiquée, puis saisir l’URL de `catalog.json` dans Alertes. Le script filtre les lignes non radar et les coordonnées hors limites; il ne récupère ni n’héberge aucune donnée source.


## Importer dans l’application

Dans **Alertes → Bases radar par pays → Importer un catalogue JSON**, sélectionne un `catalog.json` obtenu auprès d’un fournisseur de données autorisé. L’import applique les mêmes contrôles que l’actualisation HTTPS et limite le fichier à 256 Kio. Chaque URL de base reste obligatoirement HTTPS; l’app vérifie taille et SHA-256 avant installation. Le modèle vide `catalogs/radar-country-catalog.example.json` n’ajoute aucun pays.


## Parcours de l’application 11.14.136

L’écran Alertes affiche la base française et les points personnels dans des zones séparées. Le compteur « Mes radars » ne mesure que les points saisis localement; il ne signifie pas que la base officielle est vide. Aucun bouton ne demande désormais une URL de fournisseur quand aucune source n’est configurée. La Suisse apparaît comme pays sans base proposée : l’OFDF indique que les avertisseurs de contrôles radar y sont interdits. Les autres pays restent indisponibles tant qu’une base et des droits de redistribution adaptés n’ont pas été établis.


## Fournisseurs listés dans ROADGUARD 11.14.137

Le bouton « Fournisseurs de bases radar » ouvre une fiche pour chaque fournisseur. Lufop publie une API : sa documentation indique un accès Free avec clé, France/Belgique/Suisse, jusqu’à 200 requêtes par jour et 200 résultats par requête, et des données ODbL 1.0 avec attribution. Cela n’installe pas encore une base : ROADGUARD attend toujours un CSV `id;type;lat;lon` publié dans un catalogue avec URL HTTPS, empreinte SHA-256, attribution et licence. Les ZIP au format RadarDroid ne sont pas le format du catalogue.

SCDB.info annonce une base payante couvrant 113 pays; ROADGUARD ne présume pas que la licence d’achat autorise une redistribution ou un embarquement dans l’application. GPS Data Team et Kostenlos DB, visibles dans l’ancienne capture RadarDroid, sont présentés sans prétendre qu’un fichier actuel ou une licence ROADGUARD a été vérifié.


## Téléchargement intégré 11.14.138

ROADGUARD peut aussi interroger directement l’API Lufop depuis Alertes → Bases radar par pays. Une clé Lufop est requise; le pays sélectionné est demandé individuellement en JSON, puis converti et validé comme base locale. L’API officielle indique Free jusqu’à 200 résultats par appel, Pro 10 000 et Business 30 000; la disponibilité pays dépend du forfait. ROADGUARD refuse une réponse vide, exactement 200 ou 10 000, ou 30 000 résultats, car la limite peut masquer une liste tronquée. La clé est stockée dans les préférences privées Android de l’application. La France officielle reste embarquée; la Suisse est bloquée. Aucun appel avec clé n’a été réalisé pour cette version.


## Base suisse en 11.14.139

Le téléchargement et le stockage d’un pack CH sont autorisés depuis l’application. Pour distinguer ces opérations de l’utilisation d’un avertisseur, le chargeur de l’écran et le service GPS ignorent les éléments dont le pays commence par CH. ROADGUARD ne les montre donc pas sur la carte et ne déclenche pas d’alerte radar à partir de cette base. Le fichier peut être supprimé comme les autres bases installées.


## Téléchargement direct OpenStreetMap sans clé

ROADGUARD propose également une requête publique Overpass par pays pour les nœuds cartographiés highway=speed_camera. Elle ne demande ni compte ni clé API. Les données sont contributives, potentiellement incomplètes, et ne garantissent pas les radars mobiles. Attribution enregistrée dans chaque base : © OpenStreetMap contributors, https://www.openstreetmap.org/copyright; licence ODbL 1.0. Les bases CH sont conservées localement mais les chargeurs de la carte et des alertes les ignorent.


## Sources Suisse groupées en 11.14.160

Depuis Alertes → Bases radar par pays → Suisse → « Télécharger les 3 bases publiques en série », ROADGUARD peut télécharger MapAtlas (CC BY 4.0), les nœuds radar fixes OpenStreetMap via Overpass (ODbL 1.0) et le CSV Lufop Radardroid (ODbL 1.0). Chaque source est stockée dans un fichier distinct avec son attribution; les nombres ne sont pas additionnés, car les jeux peuvent se recouper. Les données suisses demeurent exclues de l’affichage cartographique et des alertes GPS. Les radars mobiles et certains types dépendent de la couverture effective du jeu Lufop; aucune source ne garantit l’exhaustivité.


## Import de l’archive CSV Lufop Suisse — 11.14.162

L’écran Alertes propose **Importer un ZIP CSV/ASC Lufop · Suisse**. Sélectionne l’archive ZIP multi-pays obtenue depuis le centre de téléchargement Lufop. ROADGUARD extrait les seuls fichiers suisses nommés `CHFixeCH*.csv` ou `.asc`, `CHFeuRougeCH.csv` ou `.asc`, `CHMobileCH.csv` ou `.asc` et `CHTunnelCH.csv` ou `.asc`; les autres pays ne sont pas importés. Les coordonnées dupliquées sont fusionnées et le compte par famille est présenté après l’import. Le fichier est conservé sous `lufop_csv_ch.csv`, sous licence ODbL 1.0 avec attribution Lufop. Les données suisses restent désactivées sur la carte et dans les alertes.

Le téléchargement automatique `https://lufop.net/radardroid-lufop-XX` est réservé par Lufop à l’application Radardroid et n’est pas utilisé. L’archive ZIP standard s’obtient depuis le centre officiel Lufop; une connexion au site peut être nécessaire. Il faut ensuite la choisir dans ROADGUARD. Ce parcours ne contourne pas le contrôle d’accès au flux Radardroid.
