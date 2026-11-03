# ROADGUARD 11.14.277 — textes du haut sans chevauchement

Les commandes GPS, bulle radar, état des radars et guidage sont regroupées dans une colonne Android dont la hauteur suit le contenu. L'état GPS utilise la largeur disponible avec une ellipse. La page cartographique reçoit la hauteur mesurée à son chargement et à chaque changement de disposition ; ses messages commencent en dessous, avec un espace de 8 dp. Le bandeau rouge d'erreur reste visible.

Contrôles : syntaxe des scripts intégrés des trois pages cartographiques vérifiée. Compilation Android bloquée : distribution Gradle absente et réseau de téléchargement inaccessible. Le navigateur de rendu local est absent ; aucun essai visuel Android ou navigateur n'est revendiqué.

🔴 INTERVENTION OBLIGATOIRE : importer ce projet dans CodeAssist, compiler puis installer la mise à jour sans désinstaller l'application. Vérifier le haut de la carte en portrait, en paysage et avec une taille de texte agrandie. Le message « erreur de chargement » visible dans la capture est un diagnostic distinct : cette version corrige sa position, sans prétendre résoudre la cause du chargement.
