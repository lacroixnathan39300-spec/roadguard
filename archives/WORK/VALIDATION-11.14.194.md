# WORK — ROADGUARD11.14.194

Changements : restriction des pages WebView et de leurs accès fichiers/contenu, vérification du propriétaire des réponses et des ponts JavaScript; liste explicite des hôtes/certificats Android Auto; actions accessibles de la bulle et de la poignée, RTL, icône monochrome, nettoyage des branches API obsolètes et contrôle d'écriture des identifiants communautaires. Cache4Go et moteurs carte/caméra conservés.

Compilation SDK36 réussie.206 tests Android simulés réussis (API26/28/34/36 selon suite), dont99 dans les suites voiture et14 pour les pages locales. Zéro échec. Lint :0 erreur,37 avertissements contre61 en193, sans baseline ni suppression globale. Restent30 textes français construits dans le code (internationalisation),4 contrôles conservateurs d'espace disponible,1 JavaScript nécessaire aux cartes embarquées,1 service exporté nécessaire à Android Auto protégé par validation d'hôte et1 attribut de retour utilisé par les API modernes. Ces avertissements ne constituent pas une validation terrain. Contrats Auto,13 écrans JavaScript et audit tuiles passés.

Terminé : corrections ci-dessus, tests et assemblage. Reste : DHU/voiture, canal Play et clé de publication, trajet Xiaomi et compte communautaire réel. Aucun de ces systèmes n'est accessible ici; aucun dépôt effectué. Le mode téléphone seul n'a pas été recompilé séparément dans cette passe. Cache rempli progressivement par consultation; ni remplissage réel4Go ni préchargement50km validé.

Estimations non auditées inchangées : global95%, +0point; chantier actif validation Android Auto sur hôte et canal Play.

APK finale : version394/11.14.194, cible36, assets comparés, classes voiture présentes, signature v2/v3 et alignement4octets vérifiés. SHA256 : `d38f00e8d730326c179800a145b97187ab3c9705f591f3d0b27200a07e592935`.
