# ROADGUARD 11.14.193 — cache4Go

Le cache des tuiles consultées passe de256Mo à4Go (4096Mio), soit16fois la capacité précédente. Le téléphone et Android Auto utilisent le même dossier et un plafond commun; aucune réservation immédiate de4Go ni téléchargement d'une région n'est effectué.

Les tuiles les moins récemment consultées sont supprimées lorsque la capacité est atteinte. Sous pression de stockage, le cache tente de libérer ses anciennes tuiles et suspend les nouvelles écritures si512Mo libres ne peuvent pas être conservés. La carte reçue du réseau continue à s'afficher même si elle ne peut pas être enregistrée. Les règles HTTP du fournisseur restent appliquées.

La page «Cache de la carte» affiche le plafond4Go. Les tuiles existantes restent compatibles. Android peut également vider ce dossier de cache lorsque le système en a besoin : le plafond n'est pas une garantie de conservation permanente.

APK fourni directement et dans LIVRAISON. Même clé de vérification que192; ne pas désinstaller une ancienne application de signature différente pour forcer la mise à jour et risquer ses données.

Compilation : JDK17 minimum, Gradle8.11.1/AGP8.10.1/SDK36; tests API36 sous JDK21. Android Auto activé par défaut. Voir le guide192 pour les commandes détaillées et les limites de distribution Android Auto, toujours applicables.

188 tests Android simulés réussis, dont52 dans la suite cache; lint sans erreur bloquante. Aucun remplissage réel de4Go de tuiles ni test Xiaomi/voiture dans cette passe. Voir WORK/VALIDATION-11.14.193.md.
