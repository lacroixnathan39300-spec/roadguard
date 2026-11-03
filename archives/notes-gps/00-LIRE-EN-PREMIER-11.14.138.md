# ROADGUARD 11.14.138 — téléchargement radar depuis l’application

- Version source Android 11.14.138 / code 338. Archive source, pas d’APK compilée.
- Alertes → Bases radar par pays : configure une clé API Lufop, sélectionne un pays puis télécharge la base en HTTPS.
- ROADGUARD convertit les coordonnées et types radar reconnus, valide le CSV interne, puis remplace atomiquement la base locale. Une base installée peut être mise à jour ou supprimée hors ligne.
- La base France officielle reste déjà incluse. La Suisse reste bloquée : avertisseurs de contrôles radar interdits.
- Lufop exige un compte/une clé. Free limite à 200 résultats par appel; Pro à 10 000 et Business à 30 000. ROADGUARD refuse une réponse égale à 200 ou atteignant 10 000 (Pro) ou 30 000 (Business) résultats, car elle peut être tronquée. Le pays doit être couvert par le forfait; 58+ pays annoncés ne signifie pas que tous les pays proposés ici seront autorisés par chaque clé.
- Tests Python radar et absence de préchargement réussis. Pas de clé Lufop fournie, pas d’appel API réel, pas de build Android ou test Xiaomi.
- Estimations non auditées : radars 83→87 %, global 75→76 %. Affichage reste estimé 100 % côté code; validation APK toujours en attente.
- Aucun préchargement de cartes; caméra reportée à la fin.
