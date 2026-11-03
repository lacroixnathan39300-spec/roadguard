# ROADGUARD — préparation de l'import radar autorisé

Version de travail : 11.14.198 (nom de chantier, aucune compilation effectuée).

## Fait
- Ajout `AuthorizedRadarImporter.java` : téléchargement HTTPS sans redirection, limites de taille, SHA-256, validation du CSV FR/CH existant, écriture atomique.
- Les fichiers sources existants, la carte, la caméra et Android Auto sont préservés.

## À faire avant activation
- Fournir les URL et empreintes SHA-256 des bases que l'utilisateur est autorisé à utiliser, et leurs formats exacts.
- Adapter les formats des fournisseurs vers le CSV ROADGUARD, sans recopier de code propriétaire.
- Brancher l'importeur sur les paramètres et le catalogue runtime (actuellement le catalogue ne lit que les assets intégrés).
- Ajouter planification des mises à jour, tests de non-régression et gestion des versions de données.
- Vérifier le régime juridique local des avertisseurs.

## Tests
- Contrôle statique uniquement. Non compilé et non testé sur téléphone/autoradio.

## Blocages
- Aucune URL de téléchargement ni autorisation documentée/conditions d'accès n'a été fournie.
- Le nouvel importeur n'est pas encore connecté à l'interface ou au moteur d'alertes.
