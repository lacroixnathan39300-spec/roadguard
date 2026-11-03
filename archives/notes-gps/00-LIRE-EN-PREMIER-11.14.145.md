# ROADGUARD 11.14.145 — téléchargement Lufop Radardroid par pays

- Version source Android 11.14.145 / code 345. Archive source, pas d’APK compilée.
- Ajoute l’export Lufop Radardroid directement dans le sélecteur des bases par pays : pas de clé API, pas de téléchargement manuel de ZIP.
- Convertit le CSV `X,Y,NAME,TYPE,SPEED,DIRTYPE,DIRECTION,GROUP` vers le stockage radar local. Catégories importées : fixes, mobiles, feux rouges, vitesse moyenne, zones radar et passages à niveau.
- Redirections HTTPS limitées au domaine Lufop, réponse limitée à 32 Mio, 100 000 lignes maximum, coordonnées et format vérifiés; fichier temporaire et remplacement réversible conservent l’ancienne base en cas d’échec.
- Attribution Lufop sauvegardée avec la base et licence ODbL 1.0. Les volumes et catégories dépendent du pays; la couverture communautaire n’est pas garantie.
- La source reste séparée de MapAtlas, OSM, API Lufop et base France. Les bases suisses sont téléchargées et gardées, mais exclues de la carte et des alertes.
- Estimations non auditées : Radars 92→93 %, global 81→82 %.
- Vérification statique faite; compiler dans CodeAssist puis tester France et un second pays sur Xiaomi.
