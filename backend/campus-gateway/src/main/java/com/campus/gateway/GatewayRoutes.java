package com.campus.gateway;
import org.springframework.context.annotation.*;
import org.springframework.cloud.gateway.route.*;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
@Configuration
public class GatewayRoutes {
 @Bean RouteLocator routes(RouteLocatorBuilder b){return b.routes()
  .route("users",r->r.path("/api/auth/**","/api/user/**","/api/admin/user/**","/api/admin/log/**","/api/admin/notifications/**").uri("lb://campus-user-service"))
  .route("tickets",r->r.path("/api/activity/**","/api/registration/**","/api/dashboard/**").uri("lb://campus-ticket-service"))
  .route("ai",r->r.path("/api/ai/**","/api/agent-runs/**","/api/conversation/**","/api/knowledge/**","/api/prompt/**")
    .metadata("response-timeout",-1).uri("lb://campus-ai-service"))
  .build();}
}
