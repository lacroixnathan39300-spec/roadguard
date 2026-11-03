# ROADGUARD 11.14.117 — navigation et cache renforcés

Version source 11.14.117 / versionCode 317. La finition graphique des six écrans reste terminée dans le code. Ce lot sécurise le calcul de route en ligne (endpoint HTTPS, coordonnées, taille/type de réponse et chiffres de trajet), corrige l’état de chargement après succès/erreur/changement de destination, et resserre la validation de source et de plafond de cache temporaire.

## Avancement des modules

| Module | % estimé | Écart vs 11.14.116 | État |
|---|---:|---:|---|
| GPS bleu | 100 | +0 | Confirmé sur ancienne APK, build actuel à vérifier |
| Carte en ligne MapLibre | 85 | +0 | Moteur et tests présents, rendu Xiaomi à vérifier |
| Planification GPS 50 km | 74 | +3 | Plan et politique bornés; source autorisée absente |
| Affichage pendant coupure | 35 | +5 | Grace locale testée, couverture réelle non garantie |
| APK et tests Xiaomi | 10 | +0 | SDK 34/Gradle/wrapper indisponibles ici |
| Navigation | 40 | +5 | Garde-fous et reprise d’état renforcés; OSRM démo |
| Radars | 35 | +5 | Alertes locales seulement, pas de flux communautaire |
| Caméra | 40 | +0 | Inchangée, reportée à la fin |
| Interface | 100 | +0 | Implémentation graphique source terminée |
| Architecture visuelle | 100 | +0 | Implémentation graphique source terminée |
| Cache temporaire automatique | 0 | +0 | Bloqué faute de fournisseur autorisé |
| **Global estimé** | **57** | **+1** | Progression du code, non auditée |

## Écrans

| Écran | % | Écart vs 11.14.116 |
|---|---:|---:|
| Carte/navigation jour-nuit | 100 | +0 |
| Accueil | 100 | +0 |
| Alertes | 100 | +0 |
| Signaler | 100 | +0 |
| Itinéraires | 100 | +0 |
| Paramètres/apparence | 100 | +0 |

Le 100 % concerne la finition graphique source; aucun rendu compilé 11.14.117 n’a été contrôlé. Le cache 50 km ne peut pas être achevé sans une source autorisée, le build attend CodeAssist/SDK, et la caméra reste à la fin.
