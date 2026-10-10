package com.backend.messaging;
import com.backend.contract.CloudEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.client.producer.*;
import org.apache.rocketmq.common.message.Message;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.*;
@Component
@ConditionalOnProperty(name="app.messaging.enabled",havingValue="true")
public class OutboxPublisher {
 private final JdbcTemplate jdbc;private final TransactionTemplate tx;private final ObjectMapper json;private final MeterRegistry metrics;
 private final String app,nameserver,topic;private final String owner=UUID.randomUUID().toString();private DefaultMQProducer producer;
 public OutboxPublisher(JdbcTemplate jdbc,PlatformTransactionManager manager,ObjectMapper json,MeterRegistry metrics,
 @Value("${spring.application.name}") String app,@Value("${app.messaging.nameserver}") String nameserver,@Value("${app.messaging.topic:campus-events}") String topic){
  this.jdbc=jdbc;this.tx=new TransactionTemplate(manager);this.json=json;this.metrics=metrics;this.app=app;this.nameserver=nameserver;this.topic=topic;
 }
 @PostConstruct public void start() throws Exception {
  producer=new DefaultMQProducer(app+"-outbox");producer.setInstanceName(owner);producer.setNamesrvAddr(nameserver);
  producer.setRetryTimesWhenSendFailed(0);producer.setSendMsgTimeout(3000);producer.start();
  metrics.gauge("campus.outbox.pending",this,p->p.jdbc.queryForObject("SELECT COUNT(*) FROM outbox_event WHERE status<>'SENT'",Long.class));
 }
 @PreDestroy public void close(){if(producer!=null)producer.shutdown();}
 @Scheduled(fixedDelayString="${app.messaging.poll-ms:1000}") public void publish(){
  List<Map<String,Object>> rows=tx.execute(status->{
   var claimed=jdbc.queryForList("SELECT * FROM outbox_event WHERE (status='PENDING' AND next_attempt<=NOW(6)) OR (status='SENDING' AND lease_until<NOW(6)) ORDER BY created_at LIMIT 10 FOR UPDATE SKIP LOCKED");
   for(var row:claimed)jdbc.update("UPDATE outbox_event SET status='SENDING',lease_owner=?,lease_until=DATE_ADD(NOW(6),INTERVAL 60 SECOND),attempts=attempts+1 WHERE event_id=?",owner,row.get("event_id"));
   return claimed;
  });
  for(var row:Objects.requireNonNull(rows)){
   String id=(String)row.get("event_id");
   try {
    var event=new CloudEvent(id,(String)row.get("event_type"),app,(String)row.get("trace_id"),(String)row.get("payload"));
    Message message=new Message(topic,event.type(),id,json.writeValueAsBytes(event));
    SendResult result=producer.send(message);
    if(result.getSendStatus()!=SendStatus.SEND_OK)throw new IllegalStateException("Broker did not confirm durable send: "+result.getSendStatus());
    jdbc.update("UPDATE outbox_event SET status='SENT',lease_owner=NULL,lease_until=NULL,last_error=NULL WHERE event_id=? AND lease_owner=?",id,owner);
    metrics.counter("campus.outbox.sent").increment();
   }catch(Exception e){
    jdbc.update("UPDATE outbox_event SET status='PENDING',next_attempt=DATE_ADD(NOW(6),INTERVAL 5 SECOND),lease_owner=NULL,lease_until=NULL,last_error=? WHERE event_id=? AND lease_owner=?",e.getClass().getSimpleName(),id,owner);
    metrics.counter("campus.outbox.failed").increment();
   }
  }
 }
}
