# ROADGUARD — PASSATION PRIORITAIRE 11.13.54

**Source 11.13.53. ZIP de code source, aucune APK compilée.**

- Avancement global historique estimé **42 %**, non audité; voir `SUIVI-AVANCEMENT.md` et `app/src/main/assets/roadguard_progress.json`.
- Réparation du registre des secteurs : récupération de `completed-sectors.tsv.bak` au démarrage si le fichier principal manque; annulation des modifications en mémoire si l'écriture de l'index échoue.
- Test : `python3 tools/test_store_recovery.py` (JDK requis), puis `python3 tools/check_handoff.py`.
- GPS bleu et caméra non modifiés; ne pas désinstaller l'application existante.
- **À faire en priorité** : source de tuiles autorisée pour téléchargement sectoriel, branchement à `OfflineCellStore`, téléchargement de fond Android, rendu MapLibre hors ligne, compilation APK et tests Xiaomi.
- **Blocages** : aucune source de téléchargement cartographique par secteur raccordée; aucune APK récente testée.
- Ne pas confondre tests Java locaux et validation Android; ne pas augmenter les pourcentages sans preuve.
- Aucune intervention utilisateur obligatoire à ce stade.
