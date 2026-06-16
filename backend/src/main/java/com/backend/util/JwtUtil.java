package com.backend.util;

import com.backend.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Component
public class JwtUtil {

    private final SecretKey signKey;
    private final long expireMs;

    public JwtUtil(JwtProperties properties) {
        this.signKey = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
        this.expireMs = properties.getExpireMs();
    }

    public String generateJwt(Map<String, Object> claims) {
        return Jwts.builder()
                .setClaims(claims)
                .signWith(signKey)
                .setExpiration(new Date(System.currentTimeMillis() + expireMs))
                .compact();
    }

    public Claims parseJWT(String jwt) {
        return Jwts.parserBuilder()
                .setSigningKey(signKey)
                .build()
                .parseClaimsJws(jwt)
                .getBody();
    }
}
