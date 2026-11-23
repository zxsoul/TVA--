package com.tva.startrail.common;

/**
 * 当前登录用户上下文（由 AuthInterceptor 写入，请求结束清理）
 */
public class UserContext {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    public static void set(Long userId) {
        USER_ID.set(userId);
    }

    public static Long get() {
        return USER_ID.get();
    }

    /**
     * 获取当前用户 ID，未登录时抛 401
     */
    public static Long require() {
        Long userId = USER_ID.get();
        if (userId == null) {
            throw new BizException(401, "未登录或 Token 已失效");
        }
        return userId;
    }

    public static void clear() {
        USER_ID.remove();
    }
}
