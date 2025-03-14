package com.spot.taxi.common.util;

/**
 * 获取当前用户信息帮助类
 */
public class AuthContextHolder {

    private static final ThreadLocal<Long> userId = new ThreadLocal<>();

    public static void setUserId(Long _userId) {
        userId.set(_userId);
    }

    public static Long getUserId() {
        return userId.get();
    }

    public static void removeUserId() {
        userId.remove();
    }

}
