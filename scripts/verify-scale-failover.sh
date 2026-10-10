#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
containers=($(docker compose ps -q ticket))
[[ ${#containers[@]} -ge 2 ]] || { echo "Run ./scripts/cloud-scale.sh first" >&2; exit 1; }
token=$(python3 - <<'PY'
import json,urllib.request,time
for attempt in range(30):
 try:
  r=urllib.request.Request('http://localhost:8080/api/auth/login',data=json.dumps({'username':'alice123','password':'pass123','role':'user'}).encode(),headers={'Content-Type':'application/json'})
  print(json.load(urllib.request.urlopen(r,timeout=3))['data']);break
 except Exception:
  if attempt==29:raise
  time.sleep(1)
PY
)
docker stop "${containers[0]}" >/dev/null
trap 'docker start "${containers[0]}" >/dev/null' EXIT
for ignored in 1 2 3 4 5; do
  code=$(curl -sS -o /dev/null -w '%{http_code}' -H "Authorization: Bearer $token" http://localhost:8080/api/activity/list)
  [[ "$code" = 200 ]] || { echo "Expected 200, got $code" >&2; exit 1; }
done
docker start "${containers[0]}" >/dev/null
trap - EXIT
echo "Failover passed: five requests succeeded while one Ticket instance was stopped."
