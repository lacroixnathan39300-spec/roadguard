# WORK — ROADGUARD11.14.193

## Changements terminés

Cache temporaire partagé téléphone/Android Auto porté à4294967296octets (4Gio, affichés4Go). Une seule capacité par dossier; un budget de64bits compte les tuiles entre les écritures et se réconcilie après une modification externe du dossier. Le nettoyage complet n'est plus déclenché toutes les32 écritures : scan/tri d'éviction uniquement à la limite ou sous pression d'espace.

À la limite, nettoyage vers environ90% du plafond avec priorité aux tuiles les moins récemment consultées. Une tuile en cours de remplacement reste protégée pour conserver sa bonne version si l'écriture est impossible. Avant chaque écriture, l'espace disponible est contrôlé pour tenter de préserver512Mio libres; les autres applications peuvent naturellement consommer de l'espace indépendamment. Une nouvelle tuile peut être affichée sans être stockée si le cache ne peut pas l'accueillir.

La page cache affiche4Go et la réserve512Mo. Aucun changement de fournisseur, préchargement géographique, téléchargement de pays, règle de fraîcheur HTTP ou moteur de caméra.

## Contrôles effectués

- Gradle8.11.1/AGP8.10.1, SDK/target36 : assembleDebug réussi.
-188 tests Robolectric réussis, zéro échec, API26/28/34/36 selon suites;52 tests cache.
- Vérifications de64bits avec un fichier creux3Gio, plafond partagé entre écritures concurrentes, éviction des anciennes consultations, espace libre insuffisant, ancienne tuile protégée, compteurs après effacement système, no-store et tuile plus grande que le budget.
- Contrôles JavaScript des13 écrans et du libellé4Go/réserve512Mo réussis; audit des points d'accès aux tuiles OSM réussi.
- Lint sans erreur bloquante. Résultats et avertissements dans WORK/lint-11.14.193.xml et WORK/tests-11.14.193/.

## Reste / blocages

Aucun essai physique ni remplissage réel de4Go de tuiles. Le test de grand volume est un fichier creux pour vérifier les compteurs longs, pas un téléchargement de données. Android peut supprimer son cache applicatif selon sa pression de stockage.

Le rayon50km n'est pas garanti par un plus grand plafond; une source autorisant le préchargement serait nécessaire. Les limites Android Auto (hôte réel, canal Play approuvé, essais véhicule et compte communautaire) restent celles détaillées en192. Aucun dépôt Play effectué. Le mode téléphone seul optionnel n'a pas été recompilé séparément dans cette passe; les mêmes classes téléphone sont compilées et leurs tests exécutés dans la variante Android Auto.

## Suivi

Cache90→92% (+2 points de réalisation estimés); global pondéré95,22→95,36% (+0,13point), arrondi95% inchangé. Ce ne sont pas des mesures de fiabilité physique.

## APK finale

Assets comparés aux sources, classes voiture et cache présentes dans le DEX, version193 contrôlée; signature v2/v3 et alignement4octets vérifiés. SHA256 : `0d406adf37127c312b083b446b018b273141f7c8f371a0381ca4bb0f37e0ade3`.
