#!/usr/bin/env bash
# Installe le « runner » GitHub ROADGUARD sur un PC Linux ou Windows (WSL Ubuntu).
# Les compilations se font alors sur ce PC, gratuitement et sans limite de minutes.
# Usage : bash install-runner.sh <jeton d'inscription>
set -euo pipefail
TOKEN="${1:?Jeton d'inscription manquant (donné par Claude ou par GitHub → Settings → Actions → Runners)}"
REPO_URL="https://github.com/lacroixnathan39300-spec/roadguard"
VERSION="2.329.0"

echo "== Outils nécessaires (Java 21, Python, Git, GitHub CLI)…"
sudo apt-get update -y
sudo apt-get install -y curl unzip zip git python3 openssl openjdk-21-jdk-headless ca-certificates
if ! command -v gh >/dev/null; then
  curl -fsSL https://cli.github.com/packages/githubcli-archive-keyring.gpg | sudo dd of=/usr/share/keyrings/githubcli-archive-keyring.gpg
  echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/githubcli-archive-keyring.gpg] https://cli.github.com/packages stable main" | sudo tee /etc/apt/sources.list.d/github-cli.list >/dev/null
  sudo apt-get update -y && sudo apt-get install -y gh
fi

echo "== Runner GitHub…"
mkdir -p "$HOME/roadguard-runner" && cd "$HOME/roadguard-runner"
if [ ! -f ./config.sh ]; then
  curl -fsSL -o runner.tgz "https://github.com/actions/runner/releases/download/v${VERSION}/actions-runner-linux-x64-${VERSION}.tar.gz"
  tar xzf runner.tgz && rm runner.tgz
fi
./config.sh --unattended --replace --url "$REPO_URL" --token "$TOKEN" --name "roadguard-pc" --labels "roadguard"

echo "== Démarrage automatique…"
if pidof systemd >/dev/null 2>&1; then
  sudo ./svc.sh install "$USER" && sudo ./svc.sh start
  echo "OK : le runner tourne en arrière-plan et redémarre tout seul."
else
  echo "systemd absent : lance « cd ~/roadguard-runner && ./run.sh » (laisse la fenêtre ouverte)."
fi
