# ROADGUARD 11.13.8 — Correctif tailles des packs pays

- Corrigé : les fiches France et catalogue international acceptent jusqu’à 2 Gio pour `pmtiles`, contre 600 Mio pour `xyz-zip`.
- Préservé : limite XYZ, téléchargement HTTPS, SHA-256, GPS, caméra et interface existante.
- Ajout : `tools/test_pmtiles_country_size_limits.py`.
- **Non validé** : téléchargement réel d’un pays, rendu hors ligne et APK sur Xiaomi.
- **BLOQUÉ** : aucun pack pays complet téléchargé et validé depuis cet environnement, pas de SDK Android.
- Work : prioriser la vérification de la licence et du rendu du pack réel France, puis Suisse/Allemagne.
