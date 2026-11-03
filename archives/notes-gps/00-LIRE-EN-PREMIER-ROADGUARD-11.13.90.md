# ROADGUARD 11.13.90 — mode cache à la Waze

## Modifications réellement faites
- Suppression de l'appel au préchargement GPS 20 km lors de chaque nouvelle position.
- Désactivation du basculement de la carte vers le fournisseur XYZ de préchargement, même si un ancien réglage est actif.
- Retrait du bouton de configuration du préchargement 20 km dans l'écran hors ligne, remplacé par une explication du cache de la WebView.
- Affichage d'un état « CACHE · HORS RÉSEAU » dans le cockpit quand la connectivité est absente.
- Le mode WebView déjà présent conserve LOAD_DEFAULT en ligne et LOAD_CACHE_ONLY hors réseau. Il n'est pas garanti que les tuiles aient été mises en cache, ni que les entêtes HTTP du serveur permettent leur réutilisation.
- Les classes de préchargement historiques restent dans le projet pour éviter des régressions non testées mais ne sont plus invoquées par la boucle GPS.

## Tests
- 6 tests Java autonomes passés.
- 1 test statique Python passé.
- Pas de compilation Android, pas de test APK ni Xiaomi.

## Limites et prochaines étapes
- L'OS/WebView peut vider le cache. Aucune garantie de carte complète hors connexion.
- Vérifier réellement l'affichage des tuiles après coupure Internet sur Xiaomi.
- Vérifier que le changement de réseau se reflète rapidement hors mise à jour GPS et que les entêtes de cache des tuiles sont respectés.
- Valider GPS, radars et caméra sans régression.
