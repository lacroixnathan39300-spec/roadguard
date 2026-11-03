# ROADGUARD 11.13.66 — PASSATION

## Fait et testé
- Ajout `tools/plan_gps_pmtiles.py` : prépare une boîte englobante conservatrice pour un rayon de 50 km GPS et génère des commandes VersaTiles vers PMTiles.
- Ajout `tests/test_plan_gps_pmtiles.py` : quatre tests Python réussis (Jura, haute latitude, validation, commande).
- Ajout `GPS-50KM-EXTRACTION-EXEMPLE.json` : plan de démonstration Jura, statut PLANNED_NOT_DOWNLOADED.
- Version Android mise à jour à 11.13.66; pas d'APK compilée.

## Bloqué / restant
- Aucun vrai pack de carte extrait ou téléchargé, aucune source HTTPS de packs hébergés, aucun téléchargement automatique validé sur Android.
- Le générateur traite un point et une zone; le découpage en cellules stables de 5 km, les chevauchements et les régions traversant le méridien ±180° nécessitent des développements supplémentaires.
- La boîte englobante est volontairement plus large qu'un disque de 50 km; ce n'est pas une extraction géographique exacte.
- MapLibre doit encore lire les packs locaux avec leurs styles, glyphes et sprites; vérifier le fonctionnement sur Xiaomi.
- Le serveur de téléchargement VersaTiles est une source de préparation, PAS un hébergement de packs ROADGUARD.

## Reprise
1. Installer le CLI VersaTiles sur une machine avec accès Internet, tester une extraction réduite et vérifier le PMTiles.
2. Créer un index de cellules stables, générer les packs et empreintes SHA256, héberger en HTTPS.
3. Connecter Android aux packs, bibliothèque cumulative et affichage MapLibre hors ligne.
4. Compiler APK et tester sur Xiaomi.

## Avancement historique (estimations non auditées)
Global 42%; GPS 100%; carte en ligne 85%; planification 50 km 70%; mémoire 55%; téléchargement réel 20%; arrière-plan 5%; hors ligne 30%; APK/tests Xiaomi 10%.
Aucune hausse sans preuve de fonctionnalité Android.
