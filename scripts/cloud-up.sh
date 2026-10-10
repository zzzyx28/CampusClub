#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 scripts/init-cloud-env.py
mvn -B -f backend/pom.xml package
docker compose up -d mysql mongodb redis nacos nameserver rocketmq
python3 scripts/init-nacos.py
docker compose up -d --build "$@"
printf 'Gateway: http://localhost:8080  Frontend: http://localhost:8088\n'
