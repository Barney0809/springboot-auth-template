package com.fu.example.starter.common;

import com.fu.example.starter.exception.BusinessException;
import lombok.Data;

import java.io.Serializable;

@Data
public class CommonResponse<T> implements Serializable {
    /**
     * 返回码
     */
    private int code;

    /**
     * 返回信息
     */
    private String message;

    /**
     * 返回泛型数据，自定义类型
     */
    private T data;

    public CommonResponse() {
    }

    public CommonResponse(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public CommonResponse(T data) {
        this.data = data;
    }

    public CommonResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> CommonResponse<T> success() {
        return new CommonResponse<>(0, "成功");
    }

    public static <T> CommonResponse<T> success(T data) {
        return new CommonResponse<>(0, "成功", data);
    }

    public static <T> CommonResponse<T> error() {
        return new CommonResponse<>(-1, "服务端错误");
    }

    public static <T> CommonResponse<T> error(int code, String message) {
        return new CommonResponse<>(code, message);
    }

    public static <T> CommonResponse<T> error(BusinessException e) {
        return new CommonResponse<>(e.getCode(), e.getMessage());
    }
}
