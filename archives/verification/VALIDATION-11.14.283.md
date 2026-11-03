# Contrôles ciblés du lot 283

| Exécution locale | Résultat |
|---|---|
| TestSpeechMilestones avec classes réelles et frontières Android injectées | 31 contrôles réussis : radar et navigation, erreurs, arrêt, fin, annulation, garde de 60 s, nettoyage des tâches, registre partagé et réarmement |
| TestMotion avec GpsMotion réel, cible Java 8 | Scénarios GNSS/réseau et perte/réacquisition, vitesse/cap inconnus et garde contre les données dans le désordre réussis |
| test_map_startup.js avec adaptateur événementiel | 12 scénarios réussis, dont état GPS frais/périmé ; pas de rendu WebGL |
| test_gps_visual_state.js | Fonction Leaflet réelle : état périmé, retour frais et absence de marqueur, réussie avec frontières de marqueur injectées |
| TestRadarMetadata avec classes réelles, cible Java 8 | Codec et propagation de l'orientation/qualification, valeurs inconnues, chargement des catalogues FR/CH existants : 26 206 points lus |
| test_radar_metadata_ui.js | Affichage connu/inconnu, formulation de l'orientation et échappement HTML réussis |
| test_tile_recovery_pack.js | 11 scénarios existants de reprise des tuiles et interface réussis après les changements de fiche |
| Analyse syntaxique Java et scripts de carte | Réussie sur les fichiers modifiés ; la syntaxe Java ne remplace pas la vérification des types Android |

Pas de compilation Android complète, de mesure sonore, d'essai Bluetooth, de rendu matériel ni de simulation du service de téléchargement Android. Les scripts d'adaptation vocale ne sont pas intégrés à l'application : ils restent sous tools/speech-tests. Les contrôles ne sont pas un nouvel audit global de tous les modules.

Les bases embarquées n'ont pas été remplacées. La préservation des métadonnées ne constitue pas un ajout de 26 206 radars : il s'agit du catalogue existant chargé pendant le contrôle.

Comparaison binaire des données embarquées CSV/DB/SQLite avec le ZIP 282 : 11 fichiers identiques.
