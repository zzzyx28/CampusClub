#!/usr/bin/env python3
"""Toggle the live registration feature flag in Nacos (on|off)."""
import json,sys,urllib.request,urllib.parse
from pathlib import Path
if len(sys.argv)!=2 or sys.argv[1] not in ('on','off'):raise SystemExit('usage: toggle-registration.py on|off')
root=Path(__file__).resolve().parents[1];env=dict(line.split('=',1) for line in (root/'.env').read_text().splitlines() if line and not line.startswith('#'))
base='http://127.0.0.1:8848/nacos'
def post(path,data,headers=None):
 req=urllib.request.Request(base+path,data=urllib.parse.urlencode(data).encode(),headers=headers or {})
 with urllib.request.urlopen(req,timeout=5) as result:return json.load(result)
login=post('/v3/auth/user/login',{'username':'nacos','password':env['NACOS_PASSWORD']});token=(login.get('data') or login)['accessToken']
result=post('/v3/admin/cs/config',{'dataId':'campus-ticket-service.properties','groupName':'CAMPUS','type':'properties','content':'app.remote.max-concurrent=20\napp.registration.enabled='+str(sys.argv[1]=='on').lower()+'\n'},{'accessToken':token})
if result.get('code')!=0:raise RuntimeError(result)
print('Registration flag updated in Nacos:',sys.argv[1])
