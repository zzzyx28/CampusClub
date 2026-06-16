package com.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {
    private List<String> allowedOriginPatterns = List.of("http://localhost:*", "http://127.0.0.1:*");
}
