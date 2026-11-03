# ROADGUARD 11.13.52 — file GPS et inventaire persistant

## Réalisé
- `NearbyOfflinePlanner`: grille géographique stable en longitude, cohérente avec `cellId`, même après déplacement GPS. La recherche couvre toujours un rayon géographique de 50 km (nombre de cellules supérieur à la version précédente).
- `MainActivity.updateNearbyOfflinePlan`: élimine de la file les cellules enregistrées et vérifiées SHA-256 dans `OfflineCellStore` (`files/offline-cells`). Si l'inventaire n'est pas accessible, conserve la planification sans effacer de données.
- GPS et caméra inchangés.

## Vérifications réellement effectuées
- `javac` des deux classes Java sans dépendance Android : réussi.
- Test de stabilité des identifiants de cellules et de recouvrement après déplacement GPS : réussi (532 cellules, 509 communes).
- Test de persistance, suppression des doublons et détection de corruption : réussi.
- Intégrité du ZIP vérifiée.

## Non réalisé / blocages
- Aucun fournisseur cartographique autorisé ni téléchargement automatique des cellules n'est connecté à cette file.
- Aucune tuile ni carte France + Corse réelle incluse.
- Aucune compilation APK ni test Xiaomi. `MainActivity` n'a pas été compilée ici.
- L'inventaire vérifie les empreintes des fichiers existants, il ne les crée ni ne les télécharge.

## Prochaines priorités
1. Sélectionner un fournisseur de données autorisant explicitement le téléchargement hors ligne et un format de secteurs cohérent.
2. Raccorder un téléchargeur persistant Android aux secteurs, avec limite de stockage, reprises et réseau contraint.
3. Connecter les fichiers sectoriels réellement téléchargés au rendu MapLibre.
4. Compiler et tester une APK sur Xiaomi sans effacer les données existantes.
