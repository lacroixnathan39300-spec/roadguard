# ROADGUARD 11.14.181 — guide

Dans Radars → Réglages avancés → Bulle radar · avec Waze, toucher « Activer la bulle flottante ». Android demande l’autorisation « Afficher par-dessus les autres applications »; revenir puis toucher Activer de nouveau. Autoriser la localisation si nécessaire. Ouvrir ensuite Waze. La bulle est déplaçable, un toucher revient à ROADGUARD, une alerte radar l’agrandit et la fait pulser trois fois. « Arrêter » dans la notification ou dans ROADGUARD met fin au suivi. Android peut suspendre un service si le téléphone l’arrête; une fermeture forcée coupe le suivi.

Dans Bases radar, les bases Allemagne (4 714), Espagne (2 104), Italie (10 707), Belgique (2 701) et Luxembourg (37) sont déjà incluses, soit 20 263 points issus de MapAtlas datés du 21/05/2024. Les raccourcis permettent une actualisation volontaire des radars connus d’OpenStreetMap. Le sélecteur propose d’autres sources et « Autre pays » accepte un code ISO à deux lettres. Les téléchargements sont volontaires, par pays, limités en taille et conservés localement après validation; aucun téléchargement de carte routière ni fichier mondial n’est ajouté. Le service GPS utilise les bases incluses et recharge les mises à jour installées; les lieux mobiles ne constituent pas des présences confirmées.

L’utilisation des avertisseurs varie selon le pays. En Allemagne, le conducteur ne doit pas utiliser un dispositif signalant les contrôles de vitesse (§ 23 (1c) StVO); l’écran d’installation du pays le rappelle. Ne présume pas que les règles françaises s’appliquent partout.

Limites : pas de test Xiaomi/Waze ni de téléchargement réel depuis ce dispositif de développement; les inventaires embarqués de 2024 peuvent être incomplets ou obsolètes; une base OSM peut être absente, incomplète ou temporairement indisponible. Le ciblage Android 28 du projet devra être modernisé pour Play. Android Auto reste à tester sur un hôte réel.

Sources : https://developer.android.com/reference/android/view/WindowManager.LayoutParams ; https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start ; https://wiki.openstreetmap.org/wiki/Tag:highway=speed_camera ; https://osmfoundation.org/wiki/Licence/Licence_and_Legal_FAQ ; https://www.gesetze-im-internet.de/stvo_2013/__23.html

Attribution des cinq bases : MapAtlas, CC BY 4.0; voir app/src/main/assets/radar_neighbors_notice.txt et radar_neighbors_manifest.json pour les sources, sommes de contrôle et transformations. Couverture non exhaustive.
