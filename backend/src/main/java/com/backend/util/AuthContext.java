package com.backend.util;

import com.backend.exception.ForbiddenException;
import com.backend.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class AuthContext {

    private AuthContext() {
    }

    public static HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    public static Long getUserId() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }
        Object value = request.getAttribute(AuthConstants.ATTR_USER_ID);
        return value instanceof Long id ? id : null;
    }

    public static Integer getUserType() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }
        Object value = request.getAttribute(AuthConstants.ATTR_USER_TYPE);
        return value instanceof Integer type ? type : null;
    }

    public static boolean isAdmin() {
        Integer type = getUserType();
        return type != null && type == AuthConstants.USER_TYPE_ADMIN;
    }

    public static boolean isServiceOrAdmin() {
        Integer type = getUserType();
        return type != null && type >= AuthConstants.USER_TYPE_SERVICE;
    }

    public static void ensureSelfOrServiceOrAdmin(Long targetUserId) {
        Long currentUserId = getUserId();
        if (currentUserId == null) {
            throw new UnauthorizedException("未登录");
        }
        if (isServiceOrAdmin() || currentUserId.equals(targetUserId)) {
            return;
        }
        throw new ForbiddenException("无权访问该用户资源");
    }

    public static void ensureSelfOrAdmin(Long targetUserId) {
        Long currentUserId = getUserId();
        if (currentUserId == null) {
            throw new UnauthorizedException("未登录");
        }
        if (isAdmin() || currentUserId.equals(targetUserId)) {
            return;
        }
        throw new ForbiddenException("无权访问该用户资源");
    }
}
