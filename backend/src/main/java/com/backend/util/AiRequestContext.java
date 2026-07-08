package com.backend.util;

import com.backend.exception.UnauthorizedException;

/**
 * 绑定单次 AI 请求中的登录用户，供工具回调使用。
 * Spring AI 流式调用可能脱离 Servlet 线程，因此在 {@link com.backend.controller.AiController} 入口显式设置。
 */
public final class AiRequestContext {

    private static final ThreadLocal<Holder> HOLDER = new ThreadLocal<>();

    public record Holder(Long userId, String username, Integer userType) {
    }

    private AiRequestContext() {
    }

    public static void set(Holder holder) {
        HOLDER.set(holder);
    }

    public static void clear() {
        HOLDER.remove();
    }

    public static Long currentUserId() {
        Holder holder = HOLDER.get();
        if (holder != null && holder.userId() != null) {
            return holder.userId();
        }
        return AuthContext.getUserId();
    }

    public static Long requireUserId() {
        Long userId = currentUserId();
        if (userId == null) {
            throw new UnauthorizedException("未登录，请先登录后再进行此操作");
        }
        return userId;
    }

    public static boolean isAdmin() {
        Holder holder = HOLDER.get();
        if (holder != null && holder.userType() != null) {
            return holder.userType() == AuthConstants.USER_TYPE_ADMIN;
        }
        return AuthContext.isAdmin();
    }
}
