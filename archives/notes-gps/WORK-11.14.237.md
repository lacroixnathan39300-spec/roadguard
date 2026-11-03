# ROADGUARD 11.14.237 — Fiabilité des téléchargements
- Remplacement atomique des archives .7z : une ancienne archive n'est plus supprimée avant la mise en place de la nouvelle.
- Si le système de fichiers ne permet pas le remplacement atomique, l'opération échoue explicitement et l'ancienne archive est conservée.
- Rejet des téléchargements trop courts et des transferts incomplets lorsque Content-Length est disponible.
- Bouton de téléchargement groupé conservé, sans changement du moteur d'alertes.
- 7z téléchargé ≠ base radar installée ; décompression/import SQLite restent non implémentés.
- Aucune compilation Android complète ni validation réseau sur Xiaomi. ZIP vérifié.
- Modification ciblée ; pas d'audit général anticipé.
