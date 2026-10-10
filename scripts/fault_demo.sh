#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
echo "Stopping ticket service; authenticated ticket routes should return 503 after discovery converges."
docker compose stop ticket
trap 'docker compose up -d ticket' EXIT
echo "Observe the gateway response, Nacos instance health, and traces, then press Enter to restore."
read -r
docker compose up -d ticket
trap - EXIT
