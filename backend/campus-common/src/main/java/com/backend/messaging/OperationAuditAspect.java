package com.backend.messaging;
import com.backend.util.AuthContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;
import java.util.*;
@Aspect @Component
public class OperationAuditAspect {
 private final OutboxStore outbox; public OperationAuditAspect(OutboxStore outbox){this.outbox=outbox;}
 @Around("execution(* com.backend.controller.*.*(..))")
 public Object audit(ProceedingJoinPoint call)throws Throwable {
  Object result=call.proceed();var request=AuthContext.currentRequest();
  if(request!=null && !Set.of("GET","HEAD","OPTIONS").contains(request.getMethod()) && !request.getRequestURI().startsWith("/internal/")){
   Map<String,Object> event=new LinkedHashMap<>();event.put("operatorId",AuthContext.getUserId());event.put("method",request.getMethod());event.put("path",request.getRequestURI());event.put("handler",call.getSignature().toShortString());
   try { outbox.append("AUDIT",event); } catch (org.springframework.dao.DataAccessException failure) {
    org.slf4j.LoggerFactory.getLogger(OperationAuditAspect.class).error("Audit enqueue failed", failure);
   }
  }
  return result;
 }
}
