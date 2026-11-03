# Reprise ROADGUARD 11.14.138

## Demande
L’utilisateur veut télécharger les bases radar directement depuis ROADGUARD, comme dans son exemple Radardroid.

## Ce qui est maintenant codé
- Dans Alertes → Bases radar par pays, saisie/modification/suppression d’une clé Lufop et choix parmi les pays configurés.
- Requête HTTPS vers l’API Lufop en JSON; conversion en CSV ROADGUARD; validation des coordonnées/types; installation avec conservation de la version précédente jusqu’au succès.
- Mise à jour/suppression des bases et rechargement des alertes foreground/background via la liste locale existante.
- Les clés ne sont pas journalisées; les erreurs masquent les URLs et paramètres potentiellement secrets.
- France déjà incluse; Suisse reste interdite et désactivée.

## Limites et reprise
- Lufop exige une clé/compte; le forfait gouverne les pays et le volume. Free: 200 résultats/appel, Pro: 10 000, Business: 30 000. L’app demande jusqu’à 30 000 résultats; les retours exactement à 200 (Free), 10 000 (Pro) ou 30 000 (Business) sont refusés comme potentiellement plafonnés.
- La liste propose 19 pays téléchargeables par l’API, mais l’accès effectif dépend du fournisseur et du forfait; une source tierce/catalogue ROADGUARD reste disponible.
- Aucun test API réel (aucune clé disponible), compilation Gradle/Android et contrôle Xiaomi restent à faire. La source ne garantit pas qu’une clé Free donnera une base complète.
- Version source 11.14.138 / code 338; tests Python ciblés réussis; aucune APK.
- Estimations radars 87 %, global 76 %, non auditées. Caméra en dernier; aucun préchargement cartographique.
