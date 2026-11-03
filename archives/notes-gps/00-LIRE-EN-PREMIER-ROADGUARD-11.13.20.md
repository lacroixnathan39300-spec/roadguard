# ROADGUARD 11.13.20 — Passation

## État réel
- Source modifiée; APK non compilée; aucune carte France complète fournie.
- GPS bleu déclaré fonctionnel par l'utilisateur dans la version installée; code GPS non modifié.
- Catalogue France: entrée acceptée uniquement si `country=france` et `scope=country`; un identifiant trompeur ne suffit plus.
- Si aucune entrée valide: message unique, sans téléchargement vers serveur mort, indiquant le nombre d'entrées rejetées.
- Trois tests ciblés réussis: test_france_country_scope_111320.py, test_france_catalog_fallback_111319.py, test_france_button_111318.py.

## Blocage et priorité
1. Obtenir légalement et préparer une véritable archive vectorielle PMTiles France métropolitaine + Corse, contrôler format, couverture, licence, taille et SHA-256.
2. Héberger archive et catalogue HTTPS fiable, permettre reprise, intégrité et stockage persistant.
3. Tester les couches MapLibre avec ce fichier (routes, labels) en mode avion.
4. Compiler APK et valider sur Xiaomi sans régression GPS, caméra, radars.
5. Compléter les autres pays et les tests de bout en bout.

## Suivi utilisateur
Avancement global 65 %: estimation héritée non auditée, ne pas augmenter sans preuve. 35 % estimés restants. Pastille rouge uniquement si action utilisateur réellement obligatoire; aucune action utilisateur nécessaire pour ce correctif source.
