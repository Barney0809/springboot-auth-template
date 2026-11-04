package com.fu.example.starter.exception;

import lombok.Getter;

@Getter
public enum ExceptionCode {

    SUCCESS(0, "成功"),
    ERROR(-1, "服务端错误"),
    PERMISSION_DENIED(401, "您的权限不足"),
    USER_NOT_EXIST(1001, "用户不存在"),
    NOT_FOUND(404, "请求数据不存在"),
    USERNAME_OR_PASSWORD_ERROR(1002, "用户名或密码错误"),
    USER_NOT_LOGIN(1003, "用户未登录"),
    PARAMS_ERROR(2000, "参数错误"),
    OPERATION_ERROR(2001, "操作失败"),
    VALIDATION_ERROR(2002, "参数验证失败");
    /**
     * 状态码
     */
    private final int code;

    /**
     * 错误信息
     */
    private final String message;

    ExceptionCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

}
