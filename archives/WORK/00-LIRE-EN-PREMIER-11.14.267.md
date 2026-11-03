# ROADGUARD 11.14.267 — lieux conservés et nom du contributeur

Projet Android complet pour CodeAssist. Le site ROADGUARD garde désormais les lieux communautaires de radar fixe, radar mobile, feu rouge et contrôle routier sans échéance automatique. Les autres événements expirent de la carte après deux heures mais leur enregistrement reste dans la base. Les votes de confirmation, absence et contestation ne suppriment pas automatiquement un lieu. Une contribution explicitement retirée par son auteur est masquée, et les anciennes suppressions déjà effectuées par les versions précédentes ne sont pas reconstituables.

La présence actuelle d'un radar mobile ou d'un contrôle ne reste récente que pendant deux heures après son signalement ou une confirmation. Au-delà, le lieu est mémorisé sur la carte sans être annoncé comme présence actuelle. La base fixe de référence reste distincte des points ajoutés par la communauté.

Une connexion au compte ROADGUARD puis un seul champ « Nom affiché » sur le site permettent de choisir un prénom ou pseudonyme public. Le téléphone lié reprend ce nom lors de la synchronisation des préférences ; le signalement Android partagé exige que ce choix soit fait. Le bouton « + » sous la bulle radar demeure utilisable à l'arrêt, après association et synchronisation du nom. Les anciens signalements sans nom restent attribués à « un membre ROADGUARD ».

Vérification locale : 32 groupes de tests API ; contrôles ciblés Android avec analyse syntaxique Java. L'APK et les échanges réels entre téléphones ne sont pas vérifiés ici. L'inventaire public initial du site contient 8 908 radars de référence France/Suisse ; les 104 838 points Android n'ont pas tous été publiés sur le site.

🔴 INTERVENTION OBLIGATOIRE — Compiler ce ZIP comme projet Gradle complet dans CodeAssist. Associer le téléphone au site, choisir le nom affiché sur le site, rouvrir ROADGUARD pour synchroniser, puis, véhicule arrêté, publier un contrôle depuis le « + ». Vérifier sur un deuxième téléphone que le nom apparaît et que le lieu reste visible après deux heures sans nouvelle confirmation, sans alerte de présence actuelle.
