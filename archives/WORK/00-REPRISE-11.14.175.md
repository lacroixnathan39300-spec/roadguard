# ROADGUARD 11.14.175 — finition, cache et APK de vérification

Demande : terminer tout à 100 %. Base : 11.14.174, version 36 du projet enregistré. Cette livraison corrige les manques réalisables ici; elle ne déclare pas un 100 % global sans essai sur Android ni source autorisant un rayon préchargé de 50 km.

## Réalisé

- Onze écrans unifiés et vérifiés en navigateur à deux largeurs; pictogrammes des incidents réellement utilisés.
- Favoris cohérents avec la destination, historique privé, journal GPS, commandes directes et réglages fonctionnels.
- Signalements précis ajoutés comme marqueurs locaux; incidents temporaires expirants, export CSV et suppression ciblée.
- Cache disque 256 Mo des tuiles consultées avec TTL, ETag, mode coupure et contrôles de corruption/concurrence.
- Leaflet JS/CSS/images et licence embarqués : démarrage du moteur sans CDN.
- Progression sur segments, manœuvres réelles, distance/temps restant et arrivée corrigés.
- Navigation libre pendant les mises à jour GPS, focus radar après chargement et recentrage testés.
- Icône d’application, thème natif et construction d’une APK signée de vérification.

- Localisation approximative : repli sur le fournisseur réseau; essai de permission Android restant.

## Ce qui est réellement vérifié

- Compilation des sources Java avec Android API 34.
- Compilation et liaison des ressources, production DEX, assemblage APK, alignement et signature APK v2/v3.
- Tests exécutables du cache : persistance après redémarrage, TTL/ETag, perte réseau, corruption, concurrence, budget de stockage.
- Tests exécutables de navigation : progression entre sommets, distance restante, temps restant, départ accentué, arrivée et hors-trajet.
- 11 pages dans un navigateur réel à 393 et 320 px : images, actions, échappement, pagination et filtres.
- Carte réelle en navigateur avec tuiles chargées : gestes, réception GPS pendant exploration, focus radar, recentrage, réseau et modes jour/nuit. Moteur également initialisé avec toutes les requêtes HTTPS bloquées.
- Régressions des données radars, imports, filtres, stabilité GPS et adaptateurs de carte.

Les tests navigateur utilisent des données d’exemple explicitement indiquées. Ils ne sont pas des captures Android. Les alertes mobiles restent des lieux historiques, pas une présence en temps réel.

## Limites bloquant 100 %

1. Aucun Android/Xiaomi accessible : exécution de l’APK, rendu natif, guidage vocal, réseau mobile et suivi écran éteint restent à vérifier.
2. La source publique OSM interdit le préchargement de zones. Le cache 256 Mo conserve les tuiles consultées; il ne garantit aucun rayon de 50 km. Aucun téléchargement manuel de France, aucun pack permanent n’a été ajouté.
3. Les services actuels ne donnent pas de trafic temps réel, de satellite/3D ni d’exclusions de péage garanties. Ces commandes ne sont pas affichées comme actives. Les fonctions caméra ont été traitées à la fin de cette passe.

Référence de la politique de tuiles : https://operations.osmfoundation.org/policies/tiles/ (consultée le 17/10/2026). La source peut être remplacée dans une évolution dédiée seulement après vérification de ses droits et de son intégration.

## APK et signature

`ROADGUARD-11.14.175-VERIFICATION.apk` est une APK debug assemblée et signée. Elle n’a pas été exécutée sur un téléphone. La clé de test locale est conservée dans `signing/verification-debug.keystore` pour pouvoir reproduire cette APK. Cette clé n’est pas une clé de publication.

CodeAssist conserve sa signature habituelle. Une APK signée avec une autre clé ne peut pas mettre à jour l’installation existante : pour conserver les données, reconstruire ce ZIP avec la configuration CodeAssist déjà utilisée. Il n’est pas demandé de désinstaller l’application.

## Vérification finale à effectuer sur Xiaomi

- Ouvrir les écrans, le clavier de recherche, les favoris et le menu illustré.
- Vérifier le GPS, l’exploration libre et le recentrage.
- Afficher un secteur, relancer l’application sans réseau, puis rétablir le réseau. Les zones non consultées auparavant ne sont pas garanties.
- Vérifier les annonces vocales, les alertes et le suivi écran éteint.

## Suivi complet

| Module | Avant | Maintenant | Variation |
|---|---:|---:|---:|
| GPS bleu | 100 % | 100 % | +0 pts |
| Carte en ligne MapLibre | 90 % | 95 % | +5 pts |
| Cache normal des tuiles consultées | 35 % | 80 % | +45 pts |
| APK et tests Xiaomi | 10 % | 75 % | +65 pts |
| Navigation | 100 % | 100 % | +0 pts |
| Radars | 99 % | 99 % | +0 pts |
| Caméra | 70 % | 90 % | +20 pts |
| Interface | 80 % | 95 % | +15 pts |
| Architecture visuelle premium | 100 % | 100 % | +0 pts |
| **Global estimé non audité** | **94 %** | **94 %** | **+0 pt** |

Les nombres restent des estimations, et certains 100 % historiques désignent uniquement l’implémentation du code. Le global est maintenant calculé par pondération documentée dans le JSON (5/15/10/5/20/15/10/15/5 %), arrondi à 94 %. L’ancienne estimation globale de 94 % n’avait pas de formule connue : son égalité ne signifie pas absence de travail.

Chantier actif : validation sur appareil et choix d’une source autorisant le préchargement requis. Aucun résultat physique sur Xiaomi ne doit être inventé par une reprise ultérieure.
