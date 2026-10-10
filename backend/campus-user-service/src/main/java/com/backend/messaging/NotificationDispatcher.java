package com.backend.messaging;
import com.backend.contract.RegistrationNotification;
import com.backend.util.EmailUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.*;
@Component
public class NotificationDispatcher {
 private final JdbcTemplate jdbc;private final TransactionTemplate tx;private final ObjectMapper json;private final EmailUtil email;private final boolean stub;private final MeterRegistry metrics;
 public NotificationDispatcher(JdbcTemplate jdbc,PlatformTransactionManager manager,ObjectMapper json,EmailUtil email,MeterRegistry metrics,@Value("${app.mail.stub:true}")boolean stub){this.jdbc=jdbc;this.tx=new TransactionTemplate(manager);this.json=json;this.email=email;this.stub=stub;this.metrics=metrics;}
 @Scheduled(fixedDelayString="${app.notification.poll-ms:1000}") public void dispatch(){
  // A crashed/in-flight external send cannot be safely auto-repeated.
  jdbc.update("UPDATE notification_task SET status='UNCERTAIN',last_error='Worker stopped or provider response unknown' WHERE status='SENDING' AND updated_at<DATE_SUB(NOW(6),INTERVAL 60 SECOND)");
  var rows=tx.execute(status->{var batch=jdbc.queryForList("SELECT event_id,payload FROM notification_task WHERE status='PENDING' ORDER BY created_at LIMIT 1 FOR UPDATE SKIP LOCKED");
   for(var r:batch)jdbc.update("UPDATE notification_task SET status='SENDING',attempts=attempts+1,updated_at=NOW(6) WHERE event_id=?",r.get("event_id"));return batch;});
  for(var row:Objects.requireNonNull(rows)) {
   String id=(String)row.get("event_id");boolean sending=false;
   try {
    var message=json.readValue((String)row.get("payload"),RegistrationNotification.class);
    var recipients=jdbc.queryForList("SELECT email FROM user WHERE id=?",String.class,message.userId());
    if(recipients.isEmpty() || recipients.getFirst()==null || recipients.getFirst().isBlank())throw new IllegalArgumentException("Recipient missing");
    sending=true;
    if(!stub)email.sendRegistrationNotification(recipients.getFirst(),message.registrationId().toString(),message.ticketCode(),message.tierName(),message.eventStart(),message.status());
    jdbc.update("UPDATE notification_task SET status='SENT',last_error=NULL,updated_at=NOW(6) WHERE event_id=? AND status='SENDING'",id);
    metrics.counter("campus.notification.sent","mode",stub?"stub":"provider").increment();
   }catch(Exception e){jdbc.update("UPDATE notification_task SET status=?,last_error=?,updated_at=NOW(6) WHERE event_id=? AND status='SENDING'",sending?"UNCERTAIN":"FAILED",e.getClass().getSimpleName(),id);metrics.counter("campus.notification.failed").increment();}
  }
 }
}
