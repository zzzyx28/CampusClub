package com.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
    /** 至少 32 字符 */
    private String secret = "change-me-in-local-yml-must-be-at-least-32-chars!!";
    private boolean issuerEnabled = false;
    private long expireMs = 43_200_000L;
}
