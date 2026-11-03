# Validation 11.14.181

- Compilation Android Gradle Plugin 8.2.2 / SDK 34 : `:app:assembleDebug` a produit une APK; signature de vérification SHA-256 `52ff1a034ff05a11847435e25c0f2077614fe278290818b42b4bad198502ffa3`, vérifiée par apksigner v2/v3.
- 35 groupes de régressions source réussis (`WORK/regressions-11.14.181.json`). Contrôle des cinq CSV : 20 263 identifiants uniques et sommes SHA-256 conformes au manifeste.
- 87 tests Robolectric ont été lancés, mais échouent tous avant le corps des tests : Maven `repo.maven.apache.org` est inaccessible au lanceur Robolectric (`UnknownHostException` pour android-all-instrumented). Ce résultat ne valide ni n'invalide le comportement de l'application.
- Pas de test matériel Xiaomi/Waze, de bulle Android en usage réel, de recalcul GPS en déplacement ni d'Android Auto dans un véhicule. Les données pays voisins datent du 21/05/2024; leur présence actuelle et leur exhaustivité ne sont pas garanties.
- Installation : l'APK de vérification ne met à jour que l'installation signée avec la même clé. Si Android signale une signature différente, conserver l'application et ses données puis compiler le ZIP avec la signature CodeAssist habituelle.
