# ROADGUARD 11.13.88 — curseur du tampon 20 km

## Réalisé
- Nouveau `EmergencyPlanCursor.java` : empreinte déterministe du plan de tuiles et du fournisseur.
- Reprise du curseur uniquement si le plan ET le fournisseur sont inchangés. Sinon, redémarrage à zéro : évite d'ignorer des tuiles près de la nouvelle position GPS.
- Correction du compteur de tuiles parcourues lors d'une interruption ou d'une perte de réseau : ne saute plus la tuile non examinée.
- Test Java dédié `EmergencyPlanCursorTest.java`.

## Limites réelles
- Préchargement soumis à un fournisseur HTTPS dont les conditions autorisent explicitement le stockage anticipé; aucun fournisseur configuré par défaut.
- Rayon 20 km : plan limité zooms 9–11 et 240 tuiles, **pas** une couverture exhaustive des rues.
- Compilation APK et validation Xiaomi non effectuées. Le GPS et la caméra n'ont pas été modifiés.

## Prochaines étapes
1. Obtenir/configurer une source avec droits de préchargement.
2. Tester les requêtes réseau et le rendu Leaflet/MapLibre, puis la coupure réseau sur appareil.
3. Compiler et installer l'APK sur Xiaomi, tester le point GPS bleu et la caméra.
4. Augmenter progressivement la couverture et afficher le diagnostic réel.
