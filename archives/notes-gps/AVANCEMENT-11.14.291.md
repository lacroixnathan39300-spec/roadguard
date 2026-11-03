# ROADGUARD 11.14.291 — suivi

Les pourcentages ci-dessous sont **les estimations historiques conservées dans le dernier suivi disponible, 11.14.268**. Aucun pourcentage n'a été augmenté par la seule lecture du code ou par des tests simulés. Les deltas restent nuls jusqu'à une mesure et une validation sur appareil.

| Module | Poids | Développement estimé | Validation estimée | Δ développement | Δ validation |
|---|---:|---:|---:|---:|---:|
| Carte / GPS | 20,0000 | 100,0000 % | 52,5000 % | +0,0000 pt | +0,0000 pt |
| Alertes | 20,0000 | 85,0000 % | 20,0000 % | +0,0000 pt | +0,0000 pt |
| Sources / activation radar | 25,0000 | 88,0000 % | 56,0000 % | +0,0000 pt | +0,0000 pt |
| UI / ergonomie | 10,0000 | 80,0000 % | 10,0000 % | +0,0000 pt | +0,0000 pt |
| Android Auto, reporté | 10,0000 | 50,0000 % | 20,0000 % | +0,0000 pt | +0,0000 pt |
| Qualité / tests / compilation | 10,0000 | 80,0000 % | 80,0000 % | +0,0000 pt | +0,0000 pt |
| Sécurité / données | 5,0000 | 100,0000 % | 90,0000 % | +0,0000 pt | +0,0000 pt |
| **TOTAL pondéré** | **100,0000** | **85,0000 %** | **44,0000 %** | **+0,0000 pt** | **+0,0000 pt** |

Chantier actif : rétablir le démarrage du moteur vectoriel et le passage aux packs téléchargés. Deux blocages reproduits dans Chromium réel : appel de setTimeout avec un receveur invalide, puis attente de style.load après une mise à jour différentielle. Correctifs intégrés ; aucun nouveau crédit dans la grille historique. Δ /11.14.290 : +0,0000 point pour chaque module et global.

Validation 291 : Chromium réel avec scripts embarqués et worker MapLibre, tuiles PBF synthétiques ; tests de démarrage simulés et politique/pipeline Java ciblés. Compilation Android, rendu des vrais packs France/Suisse et essai Xiaomi non effectués. Le retour utilisateur de cette conversation confirme l'échec de la carte hors ligne en 290.
