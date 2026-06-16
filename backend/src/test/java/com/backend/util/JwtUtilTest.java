package com.backend.util;

import com.backend.config.JwtProperties;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-secret-key-must-be-at-least-32-chars");
        properties.setExpireMs(3_600_000L);
        jwtUtil = new JwtUtil(properties);
    }

    @Test
    void generateAndParseJwt() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", 1L);
        claims.put("username", "alice");
        claims.put("userType", 1);

        String token = jwtUtil.generateJwt(claims);
        assertNotNull(token);

        Claims parsed = jwtUtil.parseJWT(token);
        assertEquals(1L, ((Number) parsed.get("userId")).longValue());
        assertEquals("alice", parsed.get("username"));
        assertEquals(1, ((Number) parsed.get("userType")).intValue());
    }
}
