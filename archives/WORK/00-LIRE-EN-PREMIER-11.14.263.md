# ROADGUARD 11.14.263 — première annonce typée et bips renforcés

Projet Android complet, successeur de 11.14.262. Pour chaque approche radar, le premier palier vocal prononce le type court et la distance (« Radar fixe à 1 000 mètres ») ; les paliers suivants prononcent seulement la distance. Si le premier GPS est déjà à 500 m, ce palier est la première annonce typée. Le suivi partagé téléphone/service évite de répéter le type lors du changement d'écran. La voix attend toujours la fin de l'annonce précédente avant de prendre le palier courant.

Volume interne ToneGenerator du bip normal : 42 → 67 / 100 ; double bip dépassement d'une limitation connue : 90 → 100 / 100. Le volume multimédia du téléphone reste déterminant ; le double bip reste distinct et les règles GPS/direction ne changent pas.

Contrôles locaux : compilation Java ciblée et exécution des scénarios TestAlertPolicies et TestCatalogCore (première alerte, suivis, dédoublonnage) ; analyse syntaxique de 68 sources Android et 18 sources de tests. Aucun SDK Android ni essai matériel pour cette version.

🔴 INTERVENTION OBLIGATOIRE — APRÈS INSTALLATION : compiler le ZIP dans CodeAssist, contrôler à l'arrêt ou comme passager que seule la première annonce indique le type du radar, puis écouter le bip normal et le double bip avec le volume multimédia réglé. Confirmer que le volume convient sans écraser la voix.
