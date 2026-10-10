package com.backend.interceptor;

import com.backend.service.RateLimitService;
import com.backend.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthInterceptorTest {

    @Mock
    private RateLimitService rateLimitService;

    private AuthInterceptor authInterceptor;
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        com.backend.config.JwtProperties properties = new com.backend.config.JwtProperties();
        properties.setIssuerEnabled(true);
        properties.setSecret("test-secret-key-must-be-at-least-32-chars");
        jwtUtil = new JwtUtil(properties);
        authInterceptor = new AuthInterceptor(jwtUtil, new ObjectMapper(), rateLimitService);
        doNothing().when(rateLimitService).checkAiRateLimit(anyLong());
    }

    @Test
    void allowsPublicAuthPath() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertTrue(authInterceptor.preHandle(request, response, new Object()));
    }

    @Test
    void rejectsMissingToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/user/list");
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertFalse(authInterceptor.preHandle(request, response, new Object()));
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
    }

    @Test
    void rejectsNonAdminForAdminPath() throws Exception {
        String token = issueToken(1L, 1);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/user/list");
        request.addHeader("token", token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertFalse(authInterceptor.preHandle(request, response, new Object()));
        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
    }

    @Test
    void allowsAdminForAdminPath() throws Exception {
        String token = issueToken(2L, 3);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/user/list");
        request.addHeader("token", token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertTrue(authInterceptor.preHandle(request, response, new Object()));
    }

    private String issueToken(Long userId, int userType) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", "tester");
        claims.put("userType", userType);
        return jwtUtil.generateJwt(claims);
    }
}
