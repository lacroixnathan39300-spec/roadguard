# ROADGUARD 11.13.51 — Inventaire durable des secteurs hors ligne

## Réellement effectué
- Ajout de `OfflineCellStore.java`, inventaire persistant des secteurs téléchargés et validés SHA-256.
- Détection des secteurs déjà disponibles et rejet des secteurs corrompus.
- Écriture de l'index avec fichier temporaire et sauvegarde de secours.
- Test Java autonome : enregistrement, persistance après redémarrage, corruption.
- VersionName Android 11.13.51.

## Limites et prochaines étapes
- Cette classe est une brique de stockage, PAS ENCORE connectée au téléchargement réseau, à MapLibre ni à un service Android d'arrière-plan.
- Aucune source de tuiles autorisant le téléchargement massif n'est configurée.
- Pas de carte France réelle, pas de nouvelle APK compilée ni test Xiaomi.
- Conserver l'APK installée et ses données. Ne pas promettre le téléchargement automatique.
- Étape suivante : intégrer le gestionnaire au moteur de téléchargement avec un format de secteurs réellement servi, une politique de stockage et WorkManager.
