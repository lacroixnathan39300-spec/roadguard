# ROADGUARD 11.14.262 — voix radar courte

Ce projet complet conserve les changements de 11.14.261 : bips répétés à partir de 1 km, double bip uniquement si une limite connue est dépassée, et paliers vocaux rapprochés.

Correction de l'interruption vocale : sur la carte et en surveillance en arrière-plan, une alerte radar prononce uniquement la distance courante, par exemple « 250 mètres ». Tant que la synthèse vocale parle ou durant une courte marge après l'annonce précédente, le nouveau palier reste disponible pour la prochaine position GPS. Le moteur ne lance plus une seconde phrase radar qui coupe la première. Les bips restent indépendants. Les avertissements des autres dangers gardent leur texte descriptif.

La distance annoncée est une mesure GPS arrondie, et certains paliers peuvent être sautés si les positions GPS arrivent tard. Le filtrage de direction dépend de la qualité du cap et, si disponible, de l'itinéraire ; aucune voie exacte ne peut être garantie avec ces seules données.

Contrôles locaux : syntaxe des 68 sources Java Android et des 18 sources de tests. Aucun SDK Android ni téléphone ici ; fonctionnement sonore à confirmer sur l'appareil.

🔴 INTERVENTION OBLIGATOIRE — APRÈS INSTALLATION : compiler ce ZIP dans CodeAssist, puis essayer l'approche radar sur téléphone, carte visible et mode en arrière-plan. Vérifier qu'une distance est entièrement prononcée avant la suivante et que la voix ne dit plus « radar à… ». En cas de gel pendant l'import, envoyer l'historique Android de l'incident.
