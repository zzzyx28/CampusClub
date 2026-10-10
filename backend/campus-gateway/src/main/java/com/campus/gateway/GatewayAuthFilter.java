package com.campus.gateway;
import com.backend.util.JwtUtil;
import com.backend.common.Result;
import com.backend.common.enumeration.ResultCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.gateway.filter.*;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.util.*;
@Component
public class GatewayAuthFilter implements GlobalFilter,Ordered {
 private final JwtUtil jwt;private final ReactiveStringRedisTemplate redis;private final ObjectMapper json;private final Environment env;
 private static final RedisScript<Long> WINDOW=RedisScript.of("local n=redis.call('INCR',KEYS[1]); if n==1 then redis.call('EXPIRE',KEYS[1],60) end; return n",Long.class);
 public GatewayAuthFilter(JwtUtil jwt,ReactiveStringRedisTemplate redis,ObjectMapper json,Environment env){this.jwt=jwt;this.redis=redis;this.json=json;this.env=env;}
 public int getOrder(){return -100;}
 public Mono<Void> filter(ServerWebExchange exchange,GatewayFilterChain chain){
  String path=exchange.getRequest().getPath().value();
  if(path.startsWith("/internal/"))return error(exchange,404,"接口不存在");
  if(exchange.getRequest().getMethod()==HttpMethod.OPTIONS)return chain.filter(exchange);
  boolean login=path.startsWith("/api/auth/");String identity;
  if(login){identity="ip:"+(exchange.getRequest().getRemoteAddress()==null?"unknown":exchange.getRequest().getRemoteAddress().getAddress().getHostAddress());}
  else{
   String token=exchange.getRequest().getHeaders().getFirst("Authorization");
   if(token!=null && token.startsWith("Bearer "))token=token.substring(7);
   else token=exchange.getRequest().getHeaders().getFirst("token");
   if(token==null)token=exchange.getRequest().getQueryParams().getFirst("token");
   try{var claims=jwt.parseJWT(token);if(claims.get("userId")==null || claims.get("userType")==null)throw new IllegalArgumentException();identity="user:"+claims.get("userId");}
   catch(Exception e){return error(exchange,401,"NOT_LOGIN");}
  }
  boolean ai=path.startsWith("/api/ai/") || (path.equals("/api/agent-runs") && exchange.getRequest().getMethod()==HttpMethod.POST);
  int limit=env.getProperty(login?"app.gateway.auth-per-minute":ai?"app.gateway.ai-per-minute":"app.gateway.requests-per-minute",Integer.class,login?20:ai?20:120);
  String key="campus:gateway:"+(ai?"ai:":"http:")+identity;
  var clean=exchange.mutate().request(request->request.headers(headers->{headers.remove("X-Campus-Service-Token");headers.remove("X-User-Id");headers.remove("X-User-Type");})).build();
  return redis.execute(WINDOW,List.of(key)).next()
    .flatMap(count->count>limit?error(exchange,429,"请求过于频繁"):chain.filter(clean))
    .onErrorResume(org.springframework.data.redis.RedisConnectionFailureException.class,e->error(exchange,503,"入口限流服务暂不可用"));
 }
 private Mono<Void> error(ServerWebExchange exchange,int status,String message){
  exchange.getResponse().setStatusCode(HttpStatusCode.valueOf(status));exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
  if(status==429)exchange.getResponse().getHeaders().set("Retry-After","60");
  try{return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(json.writeValueAsBytes(Result.error(status==401?ResultCode.UNAUTHORIZED:status==429?ResultCode.TOO_MANY_REQUESTS:ResultCode.ERROR,message)))));}
  catch(Exception e){return exchange.getResponse().setComplete();}
 }
}
