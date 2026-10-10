package com.backend.messaging;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.MDC;
import java.util.UUID;
@Component
public class OutboxStore {
 private final JdbcTemplate jdbc; private final ObjectMapper json;
 public OutboxStore(JdbcTemplate jdbc,ObjectMapper json){this.jdbc=jdbc;this.json=json;}
 public String append(String type,Object payload) {
  try {String id=UUID.randomUUID().toString();jdbc.update("INSERT INTO outbox_event(event_id,event_type,payload,trace_id) VALUES(?,?,?,?)",id,type,json.writeValueAsString(payload),MDC.get("traceId"));return id;}
  catch(org.springframework.dao.DataAccessException e){throw e;}catch(Exception e){throw new IllegalStateException("Cannot persist outbox event",e);}
 }
}
