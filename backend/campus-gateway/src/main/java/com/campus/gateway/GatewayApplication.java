package com.campus.gateway;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import com.backend.config.JwtProperties;
import com.backend.util.JwtUtil;
@SpringBootApplication
@Import({JwtProperties.class,JwtUtil.class})
public class GatewayApplication {public static void main(String[] args){SpringApplication.run(GatewayApplication.class,args);}}
