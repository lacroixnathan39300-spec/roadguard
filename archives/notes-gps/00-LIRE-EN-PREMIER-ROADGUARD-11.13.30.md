# ROADGUARD 11.13.30 — PASSATION

## État réel
- Base: archive 11.13.29 fournie dans la conversation.
- Modifié: `tools/audit_france_release.py` : validation explicite de l'URL, contrôle HTTP Range désormais obligatoire pour déclarer READY, prise en charge d'un rapport JSON de recette Xiaomi.
- Ajouté: `tools/test_audit_france_release_111330.py` : six tests unitaires, tous réussis.
- Aucun pack France+Corse PMTiles présent. Aucun serveur HTTPS validé. Aucune APK compilée ni testée sur Xiaomi.
- Point GPS bleu: aucun changement de code, état déclaré fonctionnel par l'utilisateur.

## Avancement
- Estimation historique globale 42 % NON AUDITÉE; ne pas la faire augmenter sans preuve.
- Barres individuelles: conserver dans les réponses et mettre à jour seulement après vérifications.
- Intervention utilisateur: ne demander une action qu'en cas de blocage réel, avec vignette rouge « INTERVENTION OBLIGATOIRE » et consigne précise.

## Commande de contrôle
`cd tools && python -m unittest -v test_audit_france_release_111330.py`

## Priorités
1. Obtenir réellement une archive France+Corse vectorielle et vérifier les couches/zoom et la licence.
2. Héberger en HTTPS avec HTTP Range et vérifier l'intégrité de bout en bout.
3. Connecter le téléchargement dans l'application, reprise et stockage persistant.
4. Compiler APK et vérifier en mode avion sur Xiaomi (routes, villes, GPS bleu).
5. Auditer navigation, alertes et autres pays.

## Blocages
Aucune source de carte hébergée et validée; la carte hors ligne n'est PAS opérationnelle. Ce ZIP n'est PAS une APK.
