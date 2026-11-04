package com.fu.example.starter.exception;

import com.fu.example.starter.common.CommonResponse;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
@Slf4j
@Hidden
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public CommonResponse<?> businessExceptionHandler(BusinessException e) {
        log.error("BusinessException", e);
        return CommonResponse.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public CommonResponse<?> runtimeExceptionHandler(RuntimeException e) {
        log.error("RuntimeException", e);
        return CommonResponse.error(ExceptionCode.ERROR.getCode(), "系统错误");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public CommonResponse<?> validExceptionHandler(MethodArgumentNotValidException e) {
        log.error("MethodArgumentNotValidException", e);
        BindingResult exceptions = e.getBindingResult();
        // 判断异常中是否有错误信息，如果存在就使用异常中的消息，否则使用默认消息
        if (exceptions.hasErrors()) {
            List<ObjectError> errors = exceptions.getAllErrors();
            if (!errors.isEmpty()) {
                // 这里列出了全部错误参数，按正常逻辑，只需要第一条错误即可
                FieldError fieldError = (FieldError) errors.getFirst();
                return CommonResponse.error(ExceptionCode.PARAMS_ERROR.getCode(),fieldError.getDefaultMessage());
            }
        }
        return CommonResponse.error(ExceptionCode.PARAMS_ERROR.getCode(),"请求参数错误！");
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public CommonResponse<?> validExceptionHandler(ConstraintViolationException e) {
        log.error("ConstraintViolationException", e);
        String message = e.getConstraintViolations().iterator().next().getMessage();
        return CommonResponse.error(ExceptionCode.PARAMS_ERROR.getCode(),message);
    }
}


