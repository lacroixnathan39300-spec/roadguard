# ROADGUARD 11.14.241 — point de sauvegarde de la reprise

**État : contrôles Android en cours. Cette copie conserve le développement ; le bilan sera actualisé à la fin des tests.**

La source 11.14.240 fournie a été reprise intégralement, sans recréation du projet. Son SHA-256 est `c987f78d1d73a170993d7b41007fc50e74f85760a37eed36ac4f102033b26b24`. La racine ancienne 11.14.224 était bien celle de la livraison 240.

## Modifications réalisées

- Version Gradle alignée : 11.14.241, code 441, applicationId fr.roadguard.app inchangé ; SDK 36, minimum 26 conservés.
- Démarrage local : la carte n’attend plus la mise à jour communautaire HTTP ; cache puis ouverture, actualisation en arrière-plan.
- Géométrie de caméra commune, centrage horizontal sous rotation, hauteur réglable dans Apparence, prise en compte du panneau inférieur, pause du suivi et de l’orientation pendant l’exploration manuelle. Filtrage visuel sans double lissage de caméra.
- Distance arrondie commune au bandeau, à la bulle et à la voix. Refus des mesures GPS périmées, futures ou imprécises ; effacement des états d’alerte périmés.
- Feux rouges et passages à niveau sans limitation artificielle ; mention de présence à confirmer pour les radars chantier.
- Paliers partagés par type et position ; arrêt des avertissements de l’ancien propriétaire lors d’une transition premier plan / arrière-plan.
- Catalogues FR/CH, cinq voisins et Lufop regroupés ; surcouches autorisées additives, repli sur les ressources d’origine en cas d’erreur.
- Pays triés, recherche sans accents, comptages calculés hors du fil UI, archive reçue distinguée de l’activation. Sept sources contrôlées et épinglées en taille/SHA-256. Appels Insets récents protégés pour Android 8.
- Tableau pondéré sur 100 points, 39 tâches et preuves datées, calculé dans l’application ; ancien JSON historique conservé.

## Vérifications déjà réalisées

- 59 fichiers CSV/ASC/JSON d’origine comparés : 59 inchangés. Empreintes dans `verification/original-data-11.14.240.json`.
- Compilation Java des 53 classes téléphone réussie.
- Premier APK téléphone produit ; package/version/SDK contrôlés et signature vérifiée. Un second build avec la correction Android 8 est en cours.
- 405 cas de géométrie projetée, pause manuelle, rotation, zoom adaptatif, contrôle syntaxique des deux pages : réussis.
- Tests Java de politique des alertes, mouvement GPS, téléchargements épinglés, HTTP et progression : réussis.
- Première série Android : 111 cas, 107 réussis, 4 échecs. Deux échecs ont révélé un appel API 30 non protégé dans l’écran pays sur SDK 26, corrigé. Deux provenaient d’une fixture de test qui désactivait la communauté, rectifiée. Reprise ciblée en cours.
- Sept archives publiques réellement reçues, SHA-256 de release et CRC vérifiés ; extraction hors Android et SQLite quick_check réussis. Ce contrôle ne constitue pas une activation dans l’application.
- Aucun essai Xiaomi/autoradio réalisé dans cet environnement.

## Chantier suivant

Intégrer l’extraction 7z bornée dans Android, lire SQLite en lecture seule, normaliser avec provenance et champs inconnus, puis activer une surcouche atomique. La table réelle est `cameras(id, lat, lon, type)` ; elle ne fournit ni vitesse ni sens. Ne pas en inventer. Les schémas, nombres réels et SHA-256 figurent dans `verification/speedalert-source-audit.json`. Commons Compress 1.28.0 (Apache 2.0, 1 117 221 octets) et XZ 1.12 (0BSD, 168 792 octets) ont été examinés, sans ajout comme dépendances APK ; compatibilité Android et dépendances transitives à valider.

Restent aussi la fusion avec sens/fiabilité/conflits, le rechargement complet en premier plan, les scénarios d’annulation/échec/reprise, les routes parallèles, les transitions Android Auto et les essais matériels. Le module vidéo reste en fin de projet. Le cache cartographique temporaire existant reste la base ; aucun téléchargement de région permanente n’est réactivé.

## Protection et livraison

Le ZIP contient le projet Gradle complet, les ressources et l’historique. Les sept bases embarquées, FR/CH et Lufop restent présentes et inchangées. Les données de test et outils Android téléchargés pour cette vérification ne sont pas ajoutés à l’application. L’ancien APK 11.14.197 dans `LIVRAISON/` est une référence historique conservée.

Le variant Android Auto reste conditionnel et activé par défaut, comme dans la source reçue ; `-ProadguardAndroidAuto=false` permet de vérifier le téléphone séparément. Aucun affichage d’une WebView arbitraire sur Android Auto n’est promis.

Voir `AVANCEMENT-11.14.241.md` et le ledger versionné pour les calculs à quatre décimales. 48,0000 % reste une estimation historique non auditée. Les audits globaux ne sont pas répétés à moins d’environ une heure ; les reprises ciblées après échec servent à corriger le lot.

**🔴 INTERVENTION OBLIGATOIRE — PLUS TARD :** importer le lot final dans CodeAssist avec la configuration de signature habituelle, compiler une fois à la fin du lot, installer la mise à jour et vérifier le centrage, les alertes, le paysage et Android Auto sur matériel compatible. Aucune action n’est indispensable pour poursuivre les contrôles en cours.
