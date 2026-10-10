package com.backend.messaging;
import com.backend.contract.CloudEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
@Service
public class EventInbox {
 private final JdbcTemplate jdbc;private final ObjectMapper json;
 public EventInbox(JdbcTemplate jdbc,ObjectMapper json){this.jdbc=jdbc;this.json=json;}
 @Transactional public void accept(CloudEvent event)throws Exception {
  if(event.eventId()==null || !java.util.Set.of("campus-user-service","campus-ticket-service","campus-ai-service").contains(event.source()))throw new IllegalArgumentException("Invalid event");
  if(jdbc.update("INSERT IGNORE INTO consumed_event(event_id) VALUES(?)",event.eventId())==0)return;
  if("REGISTRATION".equals(event.type())){
   if(!"campus-ticket-service".equals(event.source()))throw new IllegalArgumentException("Invalid registration source");
   jdbc.update("INSERT INTO notification_task(event_id,payload,trace_id) VALUES(?,?,?)",event.eventId(),event.payload(),event.traceId());
  } else if("AUDIT".equals(event.type())){
   Map<?,?> payload=json.readValue(event.payload(),Map.class);
   jdbc.update("INSERT INTO system_log(operator_id,class_name,method_name,message,trace_id,source_service) VALUES(?,?,?,?,?,?)",payload.get("operatorId"),payload.get("handler"),payload.get("method"),String.valueOf(payload.get("path")),event.traceId(),event.source());
  }else throw new IllegalArgumentException("Unknown event type");
 }
}
