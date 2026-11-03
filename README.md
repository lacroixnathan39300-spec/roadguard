# ROADGUARD

Navigation, radars et autoradio — © Nathan Lacroix.

| Dossier | Contenu | Plateforme |
|---|---|---|
| `gps/` | **ROADGUARD GPS** (`fr.roadguard.app`) : navigation, cartes, radars, vitesse moyenne, Android Auto. Reprend aussi le rôle de l'ancien Compagnon (liaison avec l'autoradio). | Téléphone, Android 8+ |
| `drive/` | **ROADGUARD DRIVE 2** (`fr.roadguard.drive`) : launcher d'autoradio, écrit de zéro, sans code Dashwheel. | Autoradio, API 27+, ARM 32/64 bits |
| `site/` | Site communautaire ROADGUARD (Next.js). | Web |
| `docs/` | Architecture et feuille de route. | |
| `archives/` | Notes et preuves des versions précédentes (historique, lecture seule). | |

L'ancien DRIVE 1.x et l'ancien Compagnon étaient basés sur Dashwheel (© Alexandre Leblanc,
licence non libre). Ils ne font **pas** partie de ce dépôt et restent dans les archives ZIP privées.

## Compiler

Chaque push sur `main` compile les applis sur GitHub Actions (onglet **Actions**) et publie les
APK en pièces jointes du build. En local : JDK 21 et Android SDK 36, puis

```
cd drive && ./gradlew :app:testDebugUnitTest :app:assembleDebug
cd gps   && ./gradlew :app:assembleDebug
```

## Signature

Les clés ne sont jamais dans le dépôt. Elles sont stockées dans les **secrets** GitHub du dépôt :
`DRIVE_KEYSTORE_BASE64`, `DRIVE_KEYSTORE_PASSWORD`, `DRIVE_KEY_ALIAS`, `DRIVE_KEY_PASSWORD`
et les mêmes avec le préfixe `GPS_`. Sans secret, seules les versions de test sont produites.
