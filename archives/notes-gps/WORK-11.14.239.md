# ROADGUARD 11.14.239 — Contrôle des archives 7z
- Contrôle de signature 7z et CRC32 du StartHeader avant remplacement d'une archive locale.
- La liste des pays distingue une archive vérifiée (verte, non activée) d'une archive corrompue (orange, à retélécharger).
- Le téléchargement utilise le même validateur, au lieu de vérifier uniquement six octets de signature.
- Aucun changement aux bases intégrées ni aux seuils GPS.
- ATTENTION : CRC32 de l'en-tête n'est pas une vérification cryptographique complète ni une extraction SQLite ; activation des nouveaux radars non réalisée.
- Vérification ciblée du ZIP uniquement, pas d'audit horaire ni de compilation Android.
