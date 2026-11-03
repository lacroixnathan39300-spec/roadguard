# Validation ROADGUARD 11.14.178

- APK 11.14.178 / code 378 : Java compilé, ressources liées, DEX généré, alignement et signatures v2/v3 vérifiés. Même certificat de vérification que 11.14.177. Actifs embarqués identiques aux sources livrées.
- 34/34 scripts de régression réussis, dont vérifications statiques et scripts des 12 écrans dans une VM Node. Le contrôle des sources vérifie le catalogue conditionnel, la distinction de la clé Lufop et le chemin MapAtlas préconfiguré.
- Test Java TestSecureHttp : reproduit l’erreur d’en-tête tardif, puis exécute le helper corrigé avec une connexion stricte. Réponse lisible, délais/en-têtes avant ouverture, erreurs 301/403/404/429/500, erreur de transport, déconnexion et refus HTTP/identifiants URL vérifiés. Cette connexion est simulée, elle ne constitue pas un essai réseau Android.
- Fichiers MapAtlas FR et CH téléchargés depuis le conteneur avec curl; format FeatureCollection, pays et licence CC BY 4.0 vérifiés. Métadonnées et empreintes dans mapatlas-network-11.14.178.json. Ce test ne garantit pas la connectivité du téléphone.
- Captures de l’utilisateur : écran Bases radar 11.14.177 ouvert sur Xiaomi, erreur MapAtlas identifiée. Aucun essai de l’APK 11.14.178 sur appareil effectué ici.
- Installation, réseau Android et nouvelles bases MapAtlas à confirmer sur Xiaomi. Aucun changement du site V3, de la caméra ou des réglages d’apparence.
- 500 000 utilisateurs simultanés toujours non validés. Aucun trafic de charge généré.
