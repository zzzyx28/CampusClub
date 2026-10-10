#!/usr/bin/env python3
"""Verify an Outbox event survives a local RocketMQ outage and is delivered after restart."""
import importlib.util,subprocess,time,uuid
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('smoke',ROOT/'scripts/smoke_cloud.py');smoke=importlib.util.module_from_spec(spec);spec.loader.exec_module(smoke)
code,data=smoke.request('/auth/login','POST',{'username':'alice123','password':'pass123','role':'user'});assert code==200;token=data['data']
activity=int(smoke.sql('campus_ticket',"SELECT id FROM activity WHERE activity_code LIKE 'SMOKE-%' ORDER BY id DESC LIMIT 1"));marker=uuid.uuid4().hex[:10]
subprocess.run(['docker','compose','stop','rocketmq'],cwd=ROOT,check=True,stdout=subprocess.DEVNULL)
try:
 response=smoke.request('/registration','POST',{'userId':1,'activityId':activity,'tierName':'Standard'},token,{'Idempotency-Key':'broker-'+marker});assert response[0]==200,response
 registration=response[1]['data']['id'];time.sleep(5)
 event=smoke.sql('campus_ticket',f"SELECT event_id FROM outbox_event WHERE event_type='REGISTRATION' AND JSON_UNQUOTE(JSON_EXTRACT(payload,'$.registrationId'))='{registration}'")
 state=smoke.sql('campus_ticket',f"SELECT status FROM outbox_event WHERE event_id='{event}'");assert state=='PENDING',state
finally:
 subprocess.run(['docker','compose','up','-d','rocketmq'],cwd=ROOT,check=True,stdout=subprocess.DEVNULL)
smoke.wait(lambda:smoke.sql('campus_user',f"SELECT status FROM notification_task WHERE event_id='{event}'")=='SENT',40,'outbox did not recover')
cancel=smoke.request(f'/registration/{registration}/cancel','PUT',None,token,{'Idempotency-Key':'broker-cancel-'+marker});assert cancel[0]==200,cancel
print('Broker recovery passed: committed event remained pending and was delivered after RocketMQ restarted.')
