# ROADGUARD 11.13.25 — Diagnostic cartographique

## Modifications réelles
- map_maplibre.html : journal limité à 20 erreurs MapLibre, classification TUILES/POLICES/MOTEUR, message d’erreur visible pour un pack actif, fonction getMapDiagnostics() exposée pour inspection WebView.
- Code du marqueur GPS bleu non modifié.
- Aucun moteur OPFS tiers intégré : compatibilité avec l’interception HTTPS native Android à évaluer avant migration.

## Tests et limites
- Contrôles statiques et intégrité ZIP seulement.
- Aucune carte France + Corse fournie ; aucun APK compilé ; aucun test Xiaomi.
- Les estimations de progression antérieures ne sont pas une mesure vérifiée.

## Priorités
1. Obtenir une vraie archive France + Corse, vérifier schéma, couverture, taille et SHA-256.
2. Connecter un hébergement HTTPS durable et reprise HTTP Range.
3. Tester l’interception de tuiles, les polices, le GPS et le mode avion sur APK réelle.
4. Intégrer les téléchargements autres pays après validation France.

## Intervention utilisateur
Pas nécessaire pour la présente modification du code. Test Xiaomi indispensable pour validation finale.
