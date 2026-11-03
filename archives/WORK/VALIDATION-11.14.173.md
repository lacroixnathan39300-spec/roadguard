# Validation ROADGUARD 11.14.173

- Comparaison manuelle des dix panneaux de la maquette à la structure des écrans source.
- `tools/test_reference_ui.js` : huit pages, emplacements de données réelles, actions du pont Android et échappement HTML.
- `tools/test_reference_navigation.py` : carte, Menu, Signaler central, route bleue, GPS bleu.
- Scripts de carte, catégories et visibilité radars testés; les ensembles FR/CH embarqués ne sont pas modifiés.
- Aperçu autonome de la présentation des écrans secondaires avec données fictives clairement indiquées; script parsé.
- ECJ Java 8 avec Android API 34 : compilation des sources sans erreur.

Il n’y a pas d’APK assemblé ni de capture WebView Android 11.14.173. Une ressemblance exacte ne peut donc pas être annoncée. Les commandes satellite/3D et les téléchargements de pays présents dans l’illustration ne sont pas implémentés; le cache temporaire est le choix validé. Les données GPS/routières réelles restent nécessaires pour remplir le HUD de navigation.
