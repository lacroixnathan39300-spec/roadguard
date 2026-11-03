# Vérifications ciblées 11.14.280

Ces résultats proviennent d'exécutions locales de ce lot. Ce ne sont pas des validations Android ou Xiaomi.

| Vérification exécutée | Résultat |
|---|---|
| tools/test_map_startup.js | 10 scénarios du script complet MapLibre avec adaptateur : initialisation, panne GPU simulée, timeout, réponse tardive, fermeture, rechargement de style, perte réseau pendant chargement, pause manuelle, erreur d'icônes facultatives, zoom |
| tools/test_map_native_network.js | Autorité réseau Android respectée ; événements navigateur autonomes toujours fonctionnels |
| tools/test_tile_recovery_pack.js | 11 scénarios de reprise de tuiles, packs et interface ; assertion de texte ancien mise à jour vers le texte déjà livré |
| tools/test_vector_camera.js | Suivi et pause manuelle, dégagement du panneau, dernière position au recentrage, aperçu de trajet, erreurs radar facultatives |
| tools/test_local_camera.js | Suivi Canvas, pause, mise à jour des données, aperçu et recentrage ; adaptateur DOM manquant ajouté au test |
| tools/test_camera_controls.js | Répartition des commandes de zoom et pause entre moteurs |
| tools/test_camera_motion.js | Hystérésis, temporisation et orientation courte du moteur Leaflet |
| tools/TestMapConnection.java + classes réelles MapConnectionPolicy/VisibleTileCache | Coupure, reconnexion, hors ligne manuel, zone manquante et nombres de demandes ; transport HTTP injecté |
| tools/TestTileCacheStandalone.java | 17 scénarios avec vrais fichiers et transport HTTP injecté |
| tools/motion-tests/TestMotion.java | GNSS/réseau, vitesse absente, arrêt confirmé, signal périmé, cap, intentions de navigation et projection |
| Analyse syntaxique Java MainActivity | Réussie avec le compilateur JDK ; ne vérifie pas les types Android |

Les classes de cache et de mode ont été compilées avec la cible Java 8. La compilation de l'application entière reste bloquée par l'absence de distribution Gradle/SDK Android et par l'accès réseau indisponible aux dépendances. Aucun navigateur local de rendu n'est installé. Pas de nouveau grand audit de tous les modules dans ce lot.
