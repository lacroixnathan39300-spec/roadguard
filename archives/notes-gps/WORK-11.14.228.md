# ROADGUARD 11.14.228 — Pays, sources et radars embarqués
- Analyse du projet 11.14.227 : le gris « Indisponible » provenait d'une liste codée en dur limitée à 7 pays.
- La liste inclut 45 pays européens dont les codes figurent dans la publication SpeedAlert europe-202606 ; tri alphabétique français et drapeaux.
- Bouton de mise à jour pour ces pays via archives publiques SpeedAlert, sans saisie d'URL.
- État « Radars intégrés » vert lorsque le pays a des données Lufop embarquées ou une base CSV dédiée ; les nombres Lufop proviennent de l'analyse du CSV embarqué et ne représentent pas le total dédupliqué des radars actifs.
- État « Archive téléchargée • non activée » distinct : la décompression 7z et l'import SQLite ne sont PAS intégrés.
- Aucune base radar embarquée modifiée. Les liens et le rendu doivent être testés sur Android ; la disponibilité réseau des 45 liens n'est pas individuellement vérifiée.
- Attention : les restrictions légales nationales sur les avertisseurs radar doivent être respectées.
