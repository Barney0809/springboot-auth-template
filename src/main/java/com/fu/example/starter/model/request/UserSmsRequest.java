package com.fu.example.starter.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.io.Serial;
import java.io.Serializable;

@Data
public class UserSmsRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 手机号(用户名)
    @NotBlank(message = "手机号不能为空！")
    @Length(min = 11, max = 11, message = "手机号长度必须为11位！")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式错误！")
    private String username;
    // 验证码
    @NotBlank(message = "验证码不能为空！")
    @Length(min = 6, max = 6, message = "验证码长度必须为6位！")
    @Pattern(regexp = "\\d{6}", message = "验证码格式错误！")
    private String code;
}
