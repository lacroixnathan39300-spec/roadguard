# ROADGUARD 11.14.230 — Cohérence des alertes

- Le type « Radar feu rouge » ne peut plus hériter d'une limitation de vitesse de sa source ou d'un doublon pour les alertes de vitesse dans MainActivity.
- Le service de surveillance en arrière-plan ignore aussi la limite associée à un feu rouge pour ses bips de dépassement.
- Le halo et la bannière radar passent du bleu au rouge graphite, conformément à la maquette.
- Les notifications et la bannière utilisent encore des flux GPS différents : la divergence de distance 150/250 m et le doublon visuel ne sont **pas encore corrigés**.
- Bases radar CSV/ASC/JSON strictement conservées.
- Validation : ZIP et comparaison des ressources ; compilation Android et essais GPS réels non réalisés.
