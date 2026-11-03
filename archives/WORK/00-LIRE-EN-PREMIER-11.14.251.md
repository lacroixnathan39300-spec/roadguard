# ROADGUARD 11.14.251 — cartes téléchargeables

## Changements

- Catalogue intégré de 93 archives PMTiles régionales et urbaines réellement présentes dans la publication GitHub Project N.O.M.A.D. v1 ; taille exacte et SHA-256 de chaque asset enregistrés. Priorité au nord-est de la France et à la Suisse. Source : https://github.com/p-souto/project-nomad-extra-maps/releases/tag/v1 . Données issues de Protomaps Basemap v4 / OpenStreetMap, attribution conservée sur la carte.
- Dans Cache de la carte, rechercher, télécharger, sélectionner ou supprimer chaque carte indépendamment. Le téléchargement peut reprendre après interruption si le fichier partiel correspond à la même source, et refuse les redirections hors des hôtes GitHub prévus. Les données téléchargées sont stockées séparément du cache temporaire de tuiles.
- L'affichage hors connexion utilise le lecteur PMTiles / MapLibre déjà présent, avec les palettes Jour/Nuit. Le paquet reste un projet Android à une seule application téléphone ; Android Auto est reporté.

## Vérifications

- Les 93 entrées JSON ont des identifiants distincts, des URLs conformes au catalogue publié, des tailles bornées et des empreintes SHA-256 de 64 caractères ; cohérence avec les métadonnées GitHub inspectée.
- Syntaxe JavaScript des deux pages de carte et de l'interface contrôlée par Node.
- Compilation Android tentée, bloquée avant le compilateur : la distribution Gradle 8.11.1 ne peut pas être téléchargée depuis l'environnement. Aucun APK 11.14.251 ni test matériel fourni.

## À finir

- Compiler dans CodeAssist et essayer un petit pack, puis le nord-est France et Suisse sur le téléphone.
- Affichage continu de plusieurs packs à la frontière et itinéraire entièrement hors réseau non validés. Une carte téléchargée peut être sélectionnée individuellement.
- Un téléchargement est lancé à la demande, un pack à la fois ; garder l'app active pour un gros pack, puis reprendre si le processus est interrompu.

🔴 INTERVENTION OBLIGATOIRE — PLUS TARD : importer ce projet dans CodeAssist, compiler 11.14.251, ouvrir « Cache de la carte → Télécharger des cartes hors connexion », choisir une petite carte pour valider l'installation et l'affichage, puis tester France Nord-Est / Suisse. Ne pas utiliser un APK 11.14.250 pour ce test.
