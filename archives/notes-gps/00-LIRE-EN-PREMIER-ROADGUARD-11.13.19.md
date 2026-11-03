# ROADGUARD 11.13.19 — passation

## Travail effectué
- Corrigé la sélection du pack France : une première entrée de catalogue invalide ne bloque plus une entrée valide ultérieure.
- Contrôles avant proposition du téléchargement : HTTPS sans identifiants, SHA-256 64 hexadécimaux, taille positive sous limite, format PMTiles/XYZ ZIP.
- Préserve la détection des archives France existantes et le code du GPS bleu.
- Ajouté test statique `tools/test_france_catalog_fallback_111319.py` (réussi).

## Résultats et limites
- Tests `test_france_catalog_fallback_111319.py` et `test_france_button_111318.py` : OK.
- Test historique `test_public_france_fallback.py` : ÉCHEC attendu car il exige l'ancien bouton qui utilisait map.comptoir.net, désactivé en 11.13.18. Test obsolète à réviser.
- APK non compilée; aucun test sur Xiaomi; aucune carte France complète créée, téléchargée ou validée.
- Le téléchargement France reste BLOQUÉ sans pack France+Corse hébergé et entrée de catalogue vérifiée.

## Progression (estimation, non certifiée)
- Ancienne estimation globale 65 % : non vérifiable sans cahier des charges exhaustif et recette. Ne pas la présenter comme un résultat mesuré.
- Fonction carte France hors ligne : non validée. Reste à produire/obtenir un pack, l'héberger, le cataloguer, tester installation et rendu en mode avion.
- GPS bleu : fonctionnel selon le dernier retour utilisateur; ne pas régresser.

## Pastille rouge / intervention utilisateur
- Aucune intervention utilisateur obligatoire à ce stade pour continuer le développement source.
- Afficher rouge seulement si action indispensable et non réalisable autrement : autorisation, accès d'hébergement, ou test physique final sur Xiaomi.

## Priorités
1. Trouver une source légale et fiable d'archive France+Corse (ou la produire depuis une archive mondiale), vérifier licence, taille et emprise.
2. Publier archive et catalogue HTTPS avec taille + SHA-256.
3. Vérifier lecture locale PMTiles par la WebView MapLibre et les styles.
4. Compiler l'APK et tester en mode avion sur Xiaomi; vérifier GPS, caméra, alertes.
5. Actualiser tests historiques incompatibles avec les corrections de sécurité.
