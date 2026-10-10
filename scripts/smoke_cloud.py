#!/usr/bin/env python3
"""Destructive only to records created by this run; requires compose-test.yml."""
import json,time,uuid,urllib.request,urllib.error,subprocess,sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1];BASE='http://127.0.0.1:8080/api'
def request(path,method='GET',body=None,token=None,headers=None,timeout=10):
 data=json.dumps(body).encode() if body is not None else None
 h={'Content-Type':'application/json',**(headers or {})}
 if token:h['Authorization']='Bearer '+token
 req=urllib.request.Request(BASE+path,data=data,headers=h,method=method)
 try:
  with urllib.request.urlopen(req,timeout=timeout) as response:return response.status,json.loads(response.read() or b'{}')
 except urllib.error.HTTPError as e:
  try:payload=json.loads(e.read())
  except Exception:payload={}
  return e.code,payload
def sql(database,query):
 p=subprocess.run(['docker','compose','exec','-T','mysql','bash','-c','MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot --batch --skip-column-names "$1"','_',database],cwd=ROOT,input=query,text=True,capture_output=True,check=True)
 return p.stdout.strip()
def wait(predicate,seconds,message):
 end=time.time()+seconds
 while time.time()<end:
  if predicate():return
  time.sleep(.5)
 raise AssertionError(message)
def main():
 code,data=request('/auth/login','POST',{'username':'alice123','password':'pass123','role':'user'})
 assert code==200 and data['code']==200,(code,data);token=data['data']
 code,data=request('/activity/list',token=token);assert code==200 and len(data['data'])>=4,(code,data)
 # The gateway rejects user-controlled internal identity and never routes /internal.
 code,_=request('/../internal/users/1',token=token,headers={'X-Campus-Service-Token':'forged'});assert code in (400,404)
 marker=uuid.uuid4().hex[:10];admin=request('/auth/login','POST',{'username':'admin','password':'admin123','role':'admin'})[1]['data']
 activity={'activityCode':'SMOKE-'+marker,'title':'Cloud smoke '+marker,'clubName':'Test','activityType':'Lecture','venue':'Room','registrationStart':'2030-01-01T09:00:00','eventStart':'2030-01-02T09:00:00','status':1,'tierList':[{'ticketCode':'SMOKE-T-'+marker,'tierName':'Standard','price':0,'capacity':2,'isDefault':1}]}
 code,data=request('/activity','POST',activity,admin,{'Idempotency-Key':'unused'});assert code==200,(code,data);activity_id=data['data']['id']
 key='smoke-'+marker;payload={'userId':1,'activityId':activity_id,'tierName':'Standard'}
 first=request('/registration','POST',payload,token,{'Idempotency-Key':key});second=request('/registration','POST',payload,token,{'Idempotency-Key':key})
 assert first[0]==200 and first[1]['data']==second[1]['data'],(first,second);registration=first[1]['data']['id']
 conflict=request('/registration','POST',{**payload,'activityId':activity_id+999},token,{'Idempotency-Key':key});assert conflict[0]==409,conflict
 event=sql('campus_ticket',f"SELECT event_id FROM outbox_event WHERE event_type='REGISTRATION' AND JSON_UNQUOTE(JSON_EXTRACT(payload,'$.registrationId'))='{registration}' ORDER BY created_at DESC LIMIT 1")
 wait(lambda:sql('campus_user',f"SELECT status FROM notification_task WHERE event_id='{event}'")=='SENT',40,'notification was not relayed')
 # Re-dispatch the same event; the consumer inbox must keep exactly one notification task.
 sql('campus_ticket',f"UPDATE outbox_event SET status='PENDING',next_attempt=NOW(6) WHERE event_id='{event}'")
 time.sleep(3);assert sql('campus_user',f"SELECT COUNT(*) FROM notification_task WHERE event_id='{event}'")=='1'
 cancel=request(f'/registration/{registration}/cancel','PUT',None,token,{'Idempotency-Key':'cancel-'+marker});assert cancel[0]==200,cancel
 # Agent control plane and SSE endpoint function with an explicit local substitute label.
 run=request('/agent-runs','POST',{'message':'你好，你能做什么？','conversationId':'smoke-'+marker},token,{'Idempotency-Key':'agent-'+marker})
 assert run[0]==202,run;run_id=run[1]['data']['id']
 wait(lambda:request('/agent-runs/'+run_id,token=token)[1]['data']['status'] in ('COMPLETED','PARTIALLY_COMPLETED'),20,'agent run did not complete')
 req=urllib.request.Request(BASE+f'/agent-runs/{run_id}/events',headers={'Authorization':'Bearer '+token,'Accept':'text/event-stream'})
 with urllib.request.urlopen(req,timeout=10) as stream:
  sample=stream.read(2048).decode();assert 'data:' in sample and 'sequence' in sample,sample
 print('Smoke passed: gateway auth, routing, idempotency, outbox/MQ/inbox, cancellation and Agent SSE.')
if __name__=='__main__':main()
