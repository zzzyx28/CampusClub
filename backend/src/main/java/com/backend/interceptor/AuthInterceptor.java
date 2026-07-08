package com.backend.interceptor;

import com.backend.common.Result;
import com.backend.common.enumeration.ResultCode;
import com.backend.exception.ForbiddenException;
import com.backend.service.RateLimitService;
import com.backend.util.AuthConstants;
import com.backend.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private static final Pattern USER_ID_PATH = Pattern.compile("^/api/user/(\\d+)$");
    private static final Pattern REGISTRATION_USER_PATH = Pattern.compile("^/api/registration/user/(\\d+)");
    private static final Pattern CONVERSATION_USER_PATH = Pattern.compile("^/api/conversation/user/(\\d+)");
    private static final Pattern CONVERSATION_SERVICE_PATH = Pattern.compile("^/api/conversation/service/(\\d+)$");

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final RateLimitService rateLimitService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if (HttpMethod.OPTIONS.matches(method)) {
            response.setStatus(HttpServletResponse.SC_OK);
            return true;
        }

        if (path.startsWith("/deprecated/")) {
            writeError(response, HttpServletResponse.SC_GONE, ResultCode.ERROR, "接口已废弃");
            return false;
        }

        if (path.startsWith("/api/auth/")) {
            return true;
        }

        if (path.startsWith("/actuator/")) {
            return true;
        }

        String token = resolveToken(request);
        if (!StringUtils.hasText(token)) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, ResultCode.UNAUTHORIZED, "NOT_LOGIN");
            return false;
        }

        Claims claims;
        try {
            claims = jwtUtil.parseJWT(token);
        } catch (Exception e) {
            log.debug("JWT parse failed: {}", e.getMessage());
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, ResultCode.UNAUTHORIZED, "NOT_LOGIN");
            return false;
        }

        Long userId = toLong(claims.get("userId"));
        Integer userType = toInt(claims.get("userType"));
        if (userId == null || userType == null) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, ResultCode.UNAUTHORIZED, "无效令牌");
            return false;
        }

        request.setAttribute(AuthConstants.ATTR_USER_ID, userId);
        request.setAttribute(AuthConstants.ATTR_USER_TYPE, userType);
        request.setAttribute(AuthConstants.ATTR_USERNAME, claims.get("username"));

        if (!checkRoleAccess(path, method, userType)) {
            writeError(response, HttpServletResponse.SC_FORBIDDEN, ResultCode.FORBIDDEN, "禁止访问");
            return false;
        }

        if (!checkResourceOwnership(path, method, userId, userType)) {
            writeError(response, HttpServletResponse.SC_FORBIDDEN, ResultCode.FORBIDDEN, "无权访问该资源");
            return false;
        }

        if (path.startsWith("/api/ai/")) {
            String paramUserId = request.getParameter("userId");
            if (StringUtils.hasText(paramUserId) && !userId.equals(Long.valueOf(paramUserId))) {
                writeError(response, HttpServletResponse.SC_FORBIDDEN, ResultCode.FORBIDDEN, "用户身份不匹配");
                return false;
            }
            try {
                rateLimitService.checkAiRateLimit(userId);
            } catch (ForbiddenException e) {
                writeError(response, 429, ResultCode.TOO_MANY_REQUESTS, e.getMessage());
                return false;
            }
        }

        return true;
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("token");
        if (StringUtils.hasText(header)) {
            return header;
        }
        return request.getParameter("token");
    }

    private boolean checkRoleAccess(String path, String method, int userType) {
        if (path.startsWith("/api/admin/") || path.startsWith("/api/dashboard/")) {
            return userType == AuthConstants.USER_TYPE_ADMIN;
        }
        if (path.startsWith("/api/knowledge/") || path.startsWith("/api/prompt/")) {
            return userType == AuthConstants.USER_TYPE_ADMIN;
        }
        if (path.equals("/api/registration/admin/list") || path.equals("/api/conversation/admin/list")) {
            return userType == AuthConstants.USER_TYPE_ADMIN;
        }
        if (path.startsWith("/api/activity") && isWriteMethod(method)) {
            return userType == AuthConstants.USER_TYPE_ADMIN;
        }
        if (path.equals("/api/conversation/transferred")) {
            return userType >= AuthConstants.USER_TYPE_SERVICE;
        }
        if (CONVERSATION_SERVICE_PATH.matcher(path).matches()) {
            return userType >= AuthConstants.USER_TYPE_SERVICE;
        }
        if (path.startsWith("/api/registration/activity/")) {
            return userType >= AuthConstants.USER_TYPE_SERVICE;
        }
        if (path.matches("^/api/registration/\\d+/complete$")) {
            return userType >= AuthConstants.USER_TYPE_SERVICE;
        }
        if (path.matches("^/api/registration/\\d+/cancel$") && userType == AuthConstants.USER_TYPE_USER) {
            return true;
        }
        if (path.matches("^/api/registration/\\d+/cancel$")) {
            return userType >= AuthConstants.USER_TYPE_SERVICE;
        }
        if (path.matches("^/api/conversation/[^/]+/append$") && userType >= AuthConstants.USER_TYPE_SERVICE) {
            return true;
        }
        if (path.matches("^/api/conversation/[^/]+/close$") && userType >= AuthConstants.USER_TYPE_SERVICE) {
            return true;
        }
        if (HttpMethod.DELETE.matches(method) && path.matches("^/api/conversation/[^/]+$")) {
            return userType == AuthConstants.USER_TYPE_ADMIN;
        }
        return true;
    }

    private boolean checkResourceOwnership(String path, String method, Long userId, int userType) {
        if (userType == AuthConstants.USER_TYPE_ADMIN) {
            return true;
        }

        Matcher userMatcher = USER_ID_PATH.matcher(path);
        if (userMatcher.matches()) {
            Long targetId = Long.valueOf(userMatcher.group(1));
            if (HttpMethod.GET.matches(method) && userType == AuthConstants.USER_TYPE_SERVICE) {
                return true;
            }
            return userId.equals(targetId);
        }

        Matcher regUserMatcher = REGISTRATION_USER_PATH.matcher(path);
        if (regUserMatcher.find()) {
            Long targetId = Long.valueOf(regUserMatcher.group(1));
            return userType >= AuthConstants.USER_TYPE_SERVICE || userId.equals(targetId);
        }

        Matcher convUserMatcher = CONVERSATION_USER_PATH.matcher(path);
        if (convUserMatcher.find()) {
            Long targetId = Long.valueOf(convUserMatcher.group(1));
            return userId.equals(targetId);
        }

        Matcher convServiceMatcher = CONVERSATION_SERVICE_PATH.matcher(path);
        if (convServiceMatcher.matches()) {
            Long targetId = Long.valueOf(convServiceMatcher.group(1));
            return userId.equals(targetId);
        }

        if (HttpMethod.POST.matches(method) && path.equals("/api/registration")) {
            return true;
        }

        if (HttpMethod.POST.matches(method) && (path.equals("/api/conversation") || path.equals("/api/conversation/start"))) {
            return true;
        }

        if (path.equals("/api/user/modifyPassword")) {
            return true;
        }

        return true;
    }

    private boolean isWriteMethod(String method) {
        return HttpMethod.POST.matches(method)
                || HttpMethod.PUT.matches(method)
                || HttpMethod.DELETE.matches(method);
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(value.toString());
    }

    private Integer toInt(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.valueOf(value.toString());
    }

    private void writeError(HttpServletResponse response, int httpStatus, ResultCode code, String message) throws Exception {
        response.setStatus(httpStatus);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        Result result = Result.error(code, message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
