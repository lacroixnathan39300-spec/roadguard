# ROADGUARD 11.14.216 — Rafraîchissement des alertes

## Changements
- Ajout `BackgroundAlertService.requestRadarReload()` : rechargement sur le thread principal du service déjà actif, sans lancer de nouveau service.
- Après installation locale d’une base Lufop, invalidation du cache et demande de rechargement immédiat.
- Après modification de visibilité d’un pays, demande de rechargement du service actif.
- Bases originales, cartes et caméra non modifiées.

## Limites / à faire
- Le rafraîchissement de la carte au premier plan et d’Android Auto n’est pas encore validé.
- Téléchargement automatique depuis RadarDroid toujours absent.
- Compilation Android complète et tests sur Xiaomi non effectués.
- Vérifier les performances du rechargement de 80 000 points sur le téléphone et la continuité des alertes.

## Intervention
- Compilation demandée uniquement à la fin du chantier.
