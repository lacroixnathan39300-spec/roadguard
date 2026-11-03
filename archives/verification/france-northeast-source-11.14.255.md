# Vérification ciblée du 22 octobre 2026

Source : publication GitHub p-souto/project-nomad-extra-maps v1, requêtes HTTPS Range limitées à 127 octets, sans téléchargement des fichiers complets.

| Archive | Taille annoncée (octets) | 8 premiers octets | Conclusion |
|---|---:|---|---|
| france_northeast.pmtiles | 1 436 213 468 | `00 00 00 00 00 00 00 00` | En-tête invalide |
| france_northwest.pmtiles | 905 035 408 | `50 4d 54 69 6c 65 73 03` | Signature PMTiles v3 |
| france_southeast.pmtiles | 1 039 088 322 | `50 4d 54 69 6c 65 73 03` | Signature PMTiles v3 |
| france_southwest.pmtiles | 985 863 096 | `50 4d 54 69 6c 65 73 03` | Signature PMTiles v3 |
| switzerland.pmtiles | 575 369 730 | `50 4d 54 69 6c 65 73 03` | Signature PMTiles v3 |

La spécification PMTiles v3 définit un en-tête de 127 octets à l'offset zéro et une signature ASCII `PMTiles` de sept octets, suivie du numéro de version. Une signature valide seule ne prouve pas que l'archive entière est saine. La source Nord-Est doit être corrigée par son éditeur.

Contrôles du code : JSON du catalogue 93 entrées dont 1 indisponible ; 9 tests d'en-tête ; vérifications de la reprise HTTP ; syntaxe JavaScript de la carte. Pas de compilation Android ici.
