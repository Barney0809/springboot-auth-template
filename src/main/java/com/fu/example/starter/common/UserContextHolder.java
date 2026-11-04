package com.fu.example.starter.common;

import com.fu.example.starter.model.dto.LoginUserDto;

public class UserContextHolder {
    private static final ThreadLocal<LoginUserDto> USER_CONTEXT = new ThreadLocal<>();

    // 设置用户信息到当前线程
    public static void setLoginUser(LoginUserDto loginUser) {
        USER_CONTEXT.set(loginUser);
    }

    // 获取当前线程的用户信息
    public static LoginUserDto getLoginUser() {
        return USER_CONTEXT.get();
    }

    // 获取当前用户ID
    public static Long getCurrentUserId() {
        LoginUserDto loginUser = getLoginUser();
        return loginUser != null ? loginUser.getId() : null;
    }

    // 获取当前用户名
    public static String getCurrentUsername() {
        LoginUserDto loginUser = getLoginUser();
        return loginUser != null ? loginUser.getUsername() : null;
    }

    // 清除用户信息（防止内存泄漏）
    public static void clear() {
        USER_CONTEXT.remove();
    }

    // 检查是否已登录
    public static boolean isLogin() {
        return getLoginUser() != null;
    }
}