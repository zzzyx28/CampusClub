#!/usr/bin/env python3
"""Run the documented 100-user/10-seat workload and emit measured JSON."""
import concurrent.futures,json,platform,statistics,subprocess,time,urllib.request,urllib.error,uuid
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1];BASE='http://127.0.0.1:8080/api'
def call(path,method='GET',body=None,token=None,key=None):
 data=json.dumps(body).encode() if body else None;headers={'Content-Type':'application/json'}
 if token:headers['Authorization']='Bearer '+token
 if key:headers['Idempotency-Key']=key
 req=urllib.request.Request(BASE+path,data=data,headers=headers,method=method);start=time.perf_counter()
 try:
  with urllib.request.urlopen(req,timeout=20) as res:return res.status,(time.perf_counter()-start)*1000,json.load(res)
 except urllib.error.HTTPError as e:return e.code,(time.perf_counter()-start)*1000,json.loads(e.read() or b'{}')
def sql(db,query):
 return subprocess.run(['docker','compose','exec','-T','mysql','bash','-c','MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot --batch --skip-column-names "$1"','_',db],cwd=ROOT,input=query,text=True,capture_output=True,check=True).stdout.strip()
def main():
 subprocess.run([ROOT/'scripts/prepare_load_demo.sh'],cwd=ROOT,check=True,stdout=subprocess.DEVNULL)
 status,_,login=call('/auth/login','POST',{'username':'admin','password':'admin123','role':'admin'});assert status==200;token=login['data']
 import base64
 actor=json.loads(base64.urlsafe_b64decode(token.split('.')[1]+'=='))['userId']
 subprocess.run(['docker','compose','exec','-T','redis','redis-cli','DEL',f'campus:gateway:http:user:{actor}'],cwd=ROOT,check=True,stdout=subprocess.DEVNULL)
 activity=int(sql('campus_ticket',"SELECT id FROM activity WHERE activity_code='LOAD-TEST'"));run=uuid.uuid4().hex
 def register(i):return call('/registration','POST',{'userId':10000+i,'activityId':activity,'tierName':'Standard'},token,f'benchmark-{run}-{i}')
 wall=time.perf_counter()
 with concurrent.futures.ThreadPoolExecutor(max_workers=100) as pool:results=list(pool.map(register,range(100)))
 elapsed=time.perf_counter()-wall;latencies=sorted(x[1] for x in results);codes={}
 for result in results:codes[str(result[0])]=codes.get(str(result[0]),0)+1
 succeeded=codes.get('200',0);inventory=int(sql('campus_ticket',"SELECT capacity FROM ticket_tier WHERE ticket_code='LOAD-TICKET'"));active=int(sql('campus_ticket',f"SELECT COUNT(*) FROM registration WHERE activity_id={activity} AND status=1"))
 assert succeeded==10 and inventory==0 and active==10,(succeeded,inventory,active,codes)
 counts={name:len(subprocess.check_output(['docker','compose','ps','-q',name],cwd=ROOT,text=True).splitlines()) for name in ['user','ticket']}
 assert counts=={'user':2,'ticket':2},counts
 info={'measuredAt':time.strftime('%Y-%m-%dT%H:%M:%S%z'),'host':platform.platform(),'dockerMemoryBytes':int(subprocess.check_output(['docker','info','--format','{{.MemTotal}}'],text=True)),'instances':{'gateway':1,'user':counts['user'],'ticket':counts['ticket'],'ai':1},'workload':{'concurrentUsers':100,'iterations':100,'initialSeats':10},'results':{'success':succeeded,'httpStatusCounts':codes,'wallSeconds':round(elapsed,3),'throughputRequestsPerSecond':round(100/elapsed,2),'p50Ms':round(statistics.median(latencies),2),'p95Ms':round(latencies[94],2),'maxMs':round(max(latencies),2),'finalSeats':inventory,'activeRegistrations':active}}
 (ROOT/'docs/PERFORMANCE_REPORT.json').write_text(json.dumps(info,ensure_ascii=False,indent=2)+'\n');print(json.dumps(info['results'],ensure_ascii=False))
if __name__=='__main__':main()
