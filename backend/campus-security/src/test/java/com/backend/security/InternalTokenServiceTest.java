package com.backend.security;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class InternalTokenServiceTest {
 private static final String U="user-secret-long-enough-for-hmac-256-key";
 private static final String T="ticket-secret-long-enough-for-hmac-256-key";
 private static final String A="ai-secret-long-enough-for-hmac-256-key";
 @Test void validatesCallerAudienceUserAndApproval(){
  var ai=new InternalTokenService("campus-ai-service",U,T,A);var ticket=new InternalTokenService("campus-ticket-service",U,T,A);
  String token=ai.issue("campus-ticket-service",42L,1,"alice","run-1",true);var claims=ticket.verify(token,"campus-ticket-service");
  assertEquals("42",claims.getSubject());assertEquals("campus-ai-service",claims.getIssuer());assertEquals(true,claims.get("writeApproved"));
  assertThrows(IllegalArgumentException.class,()->ticket.verify(token,"campus-user-service"));
  assertThrows(IllegalArgumentException.class,()->ticket.verify(token.substring(0,token.length()-5)+"wrong","campus-ticket-service"));
 }
 @Test void otherServicesCannotIssueAiApproval(){var user=new InternalTokenService("campus-user-service",U,T,A);var ticket=new InternalTokenService("campus-ticket-service",U,T,A);assertEquals(false,ticket.verify(user.issue("campus-ticket-service",1L,3,"admin","run",true),"campus-ticket-service").get("writeApproved"));}
 @Test void refusesUnsignedContextAndMissingUser(){var tokens=new InternalTokenService("campus-ai-service",U,T,A);assertThrows(IllegalArgumentException.class,()->tokens.verify(null,"campus-ticket-service"));assertThrows(IllegalArgumentException.class,()->tokens.issue("campus-ticket-service",null,3,"admin",null,false));}
}
