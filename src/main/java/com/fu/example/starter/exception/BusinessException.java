package com.fu.example.starter.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException{

    private final int code;

    public BusinessException(int code) {
        this.code = code;
    }

    public BusinessException(ExceptionCode code) {
        super(code.getMessage());
        this.code = code.getCode();
    }

    public BusinessException(ExceptionCode code, String message) {
        super(message);
        this.code = code.getCode();
    }
}
