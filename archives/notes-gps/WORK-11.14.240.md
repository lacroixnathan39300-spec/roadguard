# ROADGUARD 11.14.240 — Centrage horizontal et bannière contextuelle
- map.html : supprime les déplacements horizontaux calculés par les décalages de caméra « look ahead », rotation et panneau bas. La position GPS réelle et le moteur cartographique restent inchangés.
- MainActivity.checkDangers() : masque la bannière pour les radars à plus de 500 m et les autres dangers à plus de 250 m. Elle se déploie automatiquement à l'approche. Les jalons vocaux restent actifs à leur distance normale.
- Correction ciblée : pas de vérification générale anticipée, pas de compilation Android revendiquée.
- Attention : le centrage exact sous rotation et les seuils visuels doivent être confirmés sur Xiaomi.
- Les 59 fichiers de données radar sont inchangés.
