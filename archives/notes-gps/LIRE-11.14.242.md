# ROADGUARD 11.14.242 — projet complet pour CodeAssist

Importer ce projet Gradle complet. La reprise 11.14.241 envoyée par lien est incluse ; aucun autre patch n’est à superposer.

- Dernière note : `WORK-11.14.242.md`.
- Suivi complet : `AVANCEMENT-11.14.242.md`.
- Bases et ressources d’origine : préservées et contrôlées par SHA-256.
- SDK : compile/target 36, minimum 26 ; package `fr.roadguard.app` ; version 11.14.242 / 442.
- Android Auto : configuration reçue conservée (`roadguardAndroidAuto=true` dans gradle.properties). `false` isole le téléphone.

**Pas d’APK 242 dans cette livraison.** Le build local a été bloqué avant compilation par l’accès réseau à Gradle ; le SDK Android n’est pas disponible. L’APK 197 dans LIVRAISON est une référence historique. Voir la note WORK pour les tests exécutés et ceux qui restent à faire.

Les archives 7z sont vérifiées et conservées ; leur extraction SQLite et leur activation Android restent inachevées. Une archive reçue ne signifie pas de nouveaux radars actifs.

**🔴 CONSERVATION DE LA LIVRAISON :** l’espace de fichiers du compte était plein lors de la livraison ; l’enregistrement durable a été refusé. Télécharger le ZIP pour le conserver, puis libérer de la place si un enregistrement ultérieur est souhaité.

**🔴 INTERVENTION OBLIGATOIRE — PLUS TARD :** compiler une fois le lot dans CodeAssist avec la signature habituelle, installer sans effacer les données et effectuer les contrôles Xiaomi décrits dans WORK. Les audits globaux restent espacés d’environ une heure.
