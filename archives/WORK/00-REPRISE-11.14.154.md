# Reprise ROADGUARD 11.14.154

Version source 11.14.154 / code 354. Le calcul d’écart automatique utilise maintenant une projection locale sur les segments de la polyligne; les positions sans vitesse GPS ne participent pas au déclenchement.

Tests : `python3 tools/test_route_alternatives.py`, `python3 tools/test_map_assistance_controls.py`, tests de passation/radar/packaging puis `node tools/roadguard_map_syntax_check.js`. Compilation explicitement exclue de ce chantier.

À valider ensuite sur Xiaomi comme passager : faux positifs en route parallèle, demi-tour et perte du cap GPS. La caméra reste le dernier chantier.
