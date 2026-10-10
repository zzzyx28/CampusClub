package com.backend.security;
import com.backend.util.AuthConstants;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import io.jsonwebtoken.Claims;
@Component
public class InternalAuth {
 private final InternalTokenService tokens; private final String application;
 public InternalAuth(InternalTokenService tokens,@Value("${spring.application.name}") String application) {this.tokens=tokens;this.application=application;}
 public void authenticate(HttpServletRequest request) {
  Claims c=tokens.verify(request.getHeader("X-Campus-Service-Token"),application);
  request.setAttribute(AuthConstants.ATTR_USER_ID,Long.valueOf(c.getSubject()));
  request.setAttribute(AuthConstants.ATTR_USER_TYPE,((Number)c.get("userType")).intValue());
  request.setAttribute(AuthConstants.ATTR_USERNAME,c.get("username"));
  request.setAttribute("internalClaims",c);
 }
 public static void requireCaller(String... allowed) {
  var req=com.backend.util.AuthContext.currentRequest();
  Claims c=req==null?null:(Claims)req.getAttribute("internalClaims");
  if(c==null || !java.util.Arrays.asList(allowed).contains(c.getIssuer())) throw new com.backend.exception.ForbiddenException("服务调用方无权访问");
 }
 public static void requireApproved(String key) {
  requireCaller("campus-ai-service");
  Claims c=(Claims)com.backend.util.AuthContext.currentRequest().getAttribute("internalClaims");
  if(!Boolean.TRUE.equals(c.get("writeApproved")) || c.get("operationId")==null || !key.equals("agent:"+c.get("operationId")))
   throw new com.backend.exception.ForbiddenException("写操作未通过服务端审批");
 }
}
