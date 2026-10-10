#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
docker compose up -d --scale user=2 --scale ticket=2
