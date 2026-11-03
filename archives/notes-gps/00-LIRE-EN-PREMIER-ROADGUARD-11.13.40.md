# ROADGUARD 11.13.40 — publication atomique des cartes

Base : 11.13.39.

Modification : tools/prepare_france_pmtiles.sh publie maintenant les trois fichiers (archive, en-tête, SHA-256) avec sauvegarde et restauration des versions précédentes si une opération échoue.

Limites : ce mécanisme ne protège pas d’une coupure électrique au milieu de trois renommages. Aucun PMTiles France réel, hébergement, APK ni test Xiaomi. GPS non modifié.

Prochaines priorités : produire une vraie carte France + Corse depuis un build Protomaps autorisé, héberger le PMTiles et catalogue HTTPS, tester HTTP Range, compiler APK, valider rendu et GPS sur Xiaomi.
