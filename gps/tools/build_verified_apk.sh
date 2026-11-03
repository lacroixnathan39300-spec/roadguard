#!/usr/bin/env bash
# AndroidX AAR dependencies require the Android Gradle Plugin resource merger/desugarer.
set -euo pipefail
project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$project_root"
mode="${1:---auto}"
case "$mode" in
  --auto) car=true ;;
  --phone) car=false ;;
  *) echo "Usage: $0 [--auto|--phone]"; exit 2 ;;
esac
# Switching the conditional source set can leave old generated R classes.
# Only Gradle output is removed; delivered APKs and application data are separate.
if [[ -L "$project_root/app/build" ]]; then
  echo "Dossier app/build lié : nettoyage automatique refusé."
  exit 2
fi
rm -rf -- "$project_root/app/build"
if [[ -n "${RG_GRADLE:-}" ]]; then
  bash "$RG_GRADLE" --no-daemon "-ProadguardAndroidAuto=$car" :app:clean :app:assembleDebug
elif [[ -f "$project_root/gradlew" ]]; then
  bash "$project_root/gradlew" --no-daemon "-ProadguardAndroidAuto=$car" :app:clean :app:assembleDebug
elif command -v gradle >/dev/null 2>&1; then
  gradle --no-daemon "-ProadguardAndroidAuto=$car" :app:clean :app:assembleDebug
else
  echo "Compile avec Gradle 8.11.1 / Android Gradle Plugin 8.10.1 (JDK 17 minimum; JDK 21 pour les tests API36) ou CodeAssist : Android Auto nécessite la résolution des bibliothèques AndroidX."
  exit 2
fi
