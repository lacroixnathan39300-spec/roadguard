# Reprise ROADGUARD 11.14.165

Basée sur 11.14.164. Correctif demandé après l’erreur CodeAssist dans `importLufopSwissCsvZip` : variable `baseType` non déclarée. Remplacée par `type`, calculée immédiatement avant la création du flux CSV.

Le correctif carte libre reste inclus : la pause manuelle MapLibre résiste aux updates GPS; Leaflet suspend dès `dragstart`; ⌖ recentre.

Version Gradle 365 / 11.14.165. Régressions statiques et syntaxe JS à exécuter avant livraison. Ne pas compiler Android dans cette étape; l’utilisateur compile.
