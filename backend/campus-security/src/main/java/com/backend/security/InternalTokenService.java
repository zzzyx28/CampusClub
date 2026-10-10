package com.backend.security;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import java.nio.charset.StandardCharsets;
import java.util.*;
@Component
public class InternalTokenService {
 private final String caller; private final Map<String,String> keys;
 public InternalTokenService(@Value("${spring.application.name:campus-user-service}") String caller,
  @Value("${app.internal.user-secret:}") String userKey,
  @Value("${app.internal.ticket-secret:}") String ticketKey,
  @Value("${app.internal.ai-secret:}") String aiKey) {
  this.caller=caller;this.keys=Map.of("campus-user-service",userKey,"campus-ticket-service",ticketKey,"campus-ai-service",aiKey);
 }
 public String issue(String audience, Long userId, Integer role, String username, String operationId, boolean approved) {
  if(userId==null || role==null) throw new IllegalArgumentException("Missing authenticated user context");
  String key=keys.get(caller); if(key==null || key.length()<32) throw new IllegalStateException("Internal signing key is not configured");
  return Jwts.builder().setIssuer(caller).setAudience(audience).setSubject(userId.toString())
   .claim("userType",role).claim("username",username).claim("operationId",operationId)
   .claim("writeApproved",approved && caller.equals("campus-ai-service"))
   .setIssuedAt(new Date()).setExpiration(new Date(System.currentTimeMillis()+30_000))
   .signWith(Keys.hmacShaKeyFor(key.getBytes(StandardCharsets.UTF_8))).compact();
 }
 public Claims verify(String token, String audience) {
  // Try only explicitly trusted issuers; never select a key from an unverified header.
  for(var e:keys.entrySet()) {
   if(e.getKey().equals(audience) || e.getValue().length()<32) continue;
   try {return Jwts.parserBuilder().requireIssuer(e.getKey()).requireAudience(audience)
    .setSigningKey(Keys.hmacShaKeyFor(e.getValue().getBytes(StandardCharsets.UTF_8)))
    .build().parseClaimsJws(token).getBody();} catch(JwtException|IllegalArgumentException ignored) {}
  }
  throw new IllegalArgumentException("Invalid internal service identity");
 }
}
