package com.backend.client;
import com.backend.security.InternalTokenService;
import com.backend.util.*;
import feign.*;
import org.springframework.context.annotation.*;
@Configuration
public class ClientConfiguration {
 @Bean public Retryer retryer() { return Retryer.NEVER_RETRY; }
 @Bean public Request.Options requestOptions() { return new Request.Options(1,java.util.concurrent.TimeUnit.SECONDS,3,java.util.concurrent.TimeUnit.SECONDS,true); }
 @Bean public RequestInterceptor identity(InternalTokenService signer) {
  return template -> {
   var async=AiRequestContext.get();
   var request=AuthContext.currentRequest();
   Long userId=async==null?AuthContext.getUserId():async.userId();
   Integer role=async==null?AuthContext.getUserType():async.userType();
   String name=async==null?(request==null?null:(String)request.getAttribute(AuthConstants.ATTR_USERNAME)):async.username();
   template.header("X-Campus-Service-Token",signer.issue(template.feignTarget().name(),userId,role,name,
      async==null?null:async.operationId(),async!=null && async.writeApproved()));
  };
 }
}
