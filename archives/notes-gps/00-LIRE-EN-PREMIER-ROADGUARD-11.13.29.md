# ROADGUARD 11.13.29 — Passation

## Réalisé
- Ajout tools/audit_france_release.py : audit reproductible avec sortie JSON et code de retour non nul tant que la livraison n'est pas validée.
- Ajout tools/test_audit_france_release.py : 3 tests de refus (archive absente, fausse archive, URL HTTP).
- Exécution : `python -m unittest -v test_audit_france_release test_verify_map_host` : 7 tests réussis.
- Aucun changement dans les fichiers GPS, MapLibre ou APK.

## Utilisation
Depuis tools : `python audit_france_release.py --archive /chemin/france.pmtiles --url https://serveur/france.pmtiles --network --out france-release-audit.json`
Le contrôle réseau demande uniquement 127 octets. L'audit reste non validé jusqu'au test Android hors ligne, même si les autres contrôles réussissent.

## Blocages
- Aucune archive France + Corse réelle n'est présente dans le projet.
- Aucune URL de pack France hébergé et vérifié.
- APK non compilée et non testée sur Xiaomi.
- Ne pas confondre validation des métadonnées PMTiles avec vérification de la couverture des routes.

## Priorités
1. Produire la carte France + Corse depuis une source autorisée et vérifier les couches.
2. Héberger avec HTTPS et support Range 206, vérifier le contenu et le SHA256.
3. Configurer le catalogue intégré, relier le téléchargement, reprendre les transferts interrompus.
4. Compiler et tester en mode avion sur Xiaomi sans régresser le GPS bleu.

## Suivi
Progression globale historique 42 % (estimation non auditée), inchangée. Aucun utilisateur requis pour cette étape. Pastille rouge uniquement si action utilisateur réellement indispensable.
