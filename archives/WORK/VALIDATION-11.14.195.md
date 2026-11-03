# WORK — ROADGUARD11.14.195

## Mission et comportement

Mémoire privée des lieux de contrôles police/gendarmerie et radars mobiles. Au troisième événement distinct, le lieu reste permanent. Avant trois événements, expiration24h par défaut, réglable7 ou30jours. Les mêmes identifiants reçus à nouveau ne s'ajoutent pas; les UUID Waze exposés par deux fournisseurs sont normalisés. Des identifiants différents reçus au même endroit à moins de2minutes d'intervalle sont regroupés comme une seule observation. Rayon de regroupement40m, ancré sur le premier point. Les compteurs ne prouvent pas l'identité de trois personnes différentes : les API ne fournissent pas les identités des votants.

Une confirmation manuelle demande un GPS de moins de30secondes, précision60m maximum et distance100m maximum du lieu. Les observations manuelles répétées sur ce téléphone sont espacées d'au moins30minutes. Une position permanente devient «Zone de contrôle possible · lieu mémorisé» lorsque le dernier signalement a expiré, sans annoncer une présence actuelle.

Stockage JSON privé atomique commun téléphone, surveillance en arrière-plan et Android Auto, limité à3000lieux. Les points permanents ne sont pas évincés automatiquement en cas de saturation. Fichier illisible conservé, aucune réinitialisation silencieuse. Clés externes chiffrées avec Android Keystore et absentes des assets, sources livrées et pont JavaScript.

## Sources externes optionnelles

Réglages : Paramètres → Contrôles mémorisés. OpenWeb Ninja et WazeAPI, collecte désactivée par défaut, clé du fournisseur choisi puis activation explicite. Requêtes HTTPS vers les deux hôtes fixes seulement, redirections refusées, réponse limitée2Mo, intervalles1/2/5/10minutes (2par défaut), suspension après429/401/403. Seuls les événementsPOLICE sont conservés; une présence police n'est pas transformée en radar confirmé. Accidents et bouchons sont ignorés. Les pouces/indices de fiabilité ne sont pas additionnés comme de nouveaux signalements.

Les signalements privés «Radar mobile» alimentent également la mémoire. Les bases radar existantes et les autres fonctionnalités communautaires restent distinctes. La collecte n'est pas une aspiration mondiale ni une garantie d'exhaustivité : zone locale autour du GPS, plafonds fournisseur; les zones transfrontalières et la couverture France/Suisse nécessitent un essai réel. Les droits de stockage durable des données externes restent à vérifier auprès du fournisseur avant diffusion d'un service.

## Reste / blocages

Aucune clé API fournie : aucun appel authentifié, aucun abonnement, aucune donnée de démonstration injectée dans la base réelle. Schémas documentés pris en charge (liste directe et enveloppe data). Validation API réelle, téléphone Xiaomi et véhicule/DHU restante. Aucun dépôt Play. Cache4Go et moteurs carte/caméra conservés.

## Build et vérifications

256 tests Android simulés réussis, dont50 dans ControlMemoryRuntimeTest sur API26 et36. Ensemble des suites sur API26/28/34/36 selon annotations. Zéro échec. Lint :0erreur et47avertissements, sans baseline/suppression globale. Les10 avertissements supplémentaires depuis194 concernent des textes français assemblés dans le nouvel écran natif; catégories :{'UnusedAttribute': 1, 'SetJavaScriptEnabled': 1, 'ExportedService': 1, 'UsableSpace': 4, 'SetTextI18n': 40}. Contrats Android Auto, scripts des13écrans et audit tuiles réussis. Empreinte APK ci-dessous après signature. Version395 /11.14.195, cibleAPI36. Voir les rapports de tests et lint associés.

Expiration : les points temporaires expirés sont supprimés du fichier lors de la lecture. Une suppression conserve un repère temporel local pour éviter leur réapparition par simple relecture; un nouveau signalement ultérieur peut recréer un lieu temporaire. Le changement de fournisseur efface uniquement son ancienne clé, désactive la collecte et remet à zéro sa temporisation; la mémoire des lieux reste conservée.

APK finale : version395/11.14.195, cible36, assets comparés, classes mémoire et voiture présentes, signature v2/v3 et alignement4octets vérifiés. SHA256 : `a3e8f9a721457b587a3404ff3a8e4285812a72987e66665b6c27d1eca33a8d99`.
