#!/usr/bin/env python3
"""Initialize only the local CAMPUS configuration group; never print credentials."""
import json,time,urllib.request,urllib.parse,urllib.error
from pathlib import Path
root=Path(__file__).resolve().parents[1]
env=dict(line.split('=',1) for line in (root/'.env').read_text().splitlines() if line and not line.startswith('#'))
base='http://127.0.0.1:8848/nacos'
def post(path,data):
 req=urllib.request.Request(base+path,data=urllib.parse.urlencode(data).encode())
 with urllib.request.urlopen(req,timeout=5) as response:return json.loads(response.read())
for attempt in range(60):
 try:
  # Nacos 3 requires first-time administrator initialization.
  try:post('/v3/auth/user/admin',{'password':env.get('NACOS_PASSWORD','nacos')})
  except urllib.error.HTTPError:pass
  token=post('/v3/auth/user/login',{'username':'nacos','password':env.get('NACOS_PASSWORD','nacos')})['accessToken']
  break
 except Exception:
  if attempt==59:raise
  time.sleep(2)
configs={
 'campus-gateway':'app.gateway.requests-per-minute=120\napp.gateway.ai-per-minute=20\napp.gateway.auth-per-minute=20\n',
 'campus-user-service':'app.remote.max-concurrent=20\n',
 'campus-ticket-service':'app.remote.max-concurrent=20\napp.registration.enabled=true\n',
 'campus-ai-service':'app.remote.max-concurrent=20\n',
}
for app,content in configs.items():
 path='/v3/admin/cs/config?'+urllib.parse.urlencode({'accessToken':token})
 result=post(path,{'dataId':app+'.properties','groupName':'CAMPUS','type':'properties','content':content})
 if not isinstance(result,dict) or result.get('code')!=0:raise RuntimeError('Nacos config publish failed: '+app)
print('Initialized CAMPUS configs (no credentials stored in Nacos).')
