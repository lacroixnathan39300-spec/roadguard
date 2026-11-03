# ROADGUARD 11.14.289 — hors ligne et marqueurs

Projet complet, versionCode 489. Aucun APK inclus. Installer en mise à jour avec la signature habituelle pour conserver les cartes et les données. Ne pas effacer les données de l’application.

Le correctif 288 conservait le moteur raster déjà ouvert même en présence de cartes vectorielles téléchargées. Une seule tuile en cache empêchait ensuite la détection du manque de couverture. En 289, sélectionner Hors ligne active le moteur local lorsqu’un pack vectoriel complet est disponible ; une couverture raster partielle peut également déclencher le secours local en Auto sans réseau. Les packs manuels de pmtiles/ et les secteurs terminés enregistrés dans offline-cells/ participent à la découverte. Les fichiers partiels et les fichiers non enregistrés du second magasin sont exclus. La découverte contrôle la taille enregistrée et l’en-tête sans recalculer un gros fichier sur le fil d’interface ; elle ne constitue pas une nouvelle vérification SHA-256. L’import conserve sa vérification complète. Le chemin direct par identifiant conserve également sa vérification.

Les trois cartes utilisent maintenant un filtre commun avant le regroupement des marqueurs : même catégorie normalisée et même site. Les copies exactes du catalogue se regroupent dans un mètre ; un doublon personnel/communautaire compatible peut se regrouper dans douze mètres. Sens et vitesses connus contradictoires, catégories différentes et identifiants communautaires distincts sont conservés. Les libellés Zone de danger et Zone dangereuse sont équivalents. Aucune donnée d’inventaire n’est supprimée et les alertes GPS conservent leur index complet. Les ronds chiffrés peuvent toujours représenter plusieurs lieux distincts. Cette règle prudente ne prétend pas résoudre des observations ambiguës simplement proches.

## Vérifications

Tests locaux réussis : lecture de vrais octets PMTiles synthétiques via le registre et la mosaïque ; exclusions partiels/orphelins ; résolution de fichiers ; douze scénarios de marqueurs ; callbacks de couverture complète, vide et partielle ; scripts complets des cartes avec adaptateurs de rendu ; tests du catalogue sur les fichiers embarqués. Syntaxe Java vérifiée. Compilation Android, rendu matériel et essai Xiaomi non exécutés dans cet environnement sans SDK Android utilisable. Détails dans verification/VALIDATION-11.14.289.md.

## 🔴 INTERVENTION OBLIGATOIRE — validation sur le téléphone après compilation

1. Compiler 11.14.289 dans CodeAssist et l’installer avec la signature habituelle, sans désinstaller la version actuelle.
2. Garder les packs existants. Ouvrir une zone couverte, sélectionner Hors ligne, puis activer le mode avion. Fermer et rouvrir ROADGUARD, déplacer la carte dans la zone téléchargée et changer le zoom.
3. Vérifier les signalements à plusieurs niveaux de zoom. Un groupe de lieux distincts doit conserver son nombre ; les copies du même lieu doivent diminuer.
4. Si la carte reste vide, relever « … → État et diagnostic → Diagnostic de l’appareil » et le nom exact du pack téléchargé. Cela permettra de distinguer un pack non découvert, une zone non couverte et une panne du moteur graphique.

Le rendu du téléphone reste à confirmer ; cette livraison ne garantit pas à elle seule que le défaut montré sur la capture a disparu. Fréquence communautaire conservée à deux minutes, Spotify et Android Auto inchangés par ce lot.
