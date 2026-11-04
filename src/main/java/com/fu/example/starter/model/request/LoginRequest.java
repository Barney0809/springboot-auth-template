package com.fu.example.starter.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.io.Serial;
import java.io.Serializable;

@Data
public class LoginRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户名 */
    @NotBlank(message = "用户名不能为空！")
    @Length(min = 4,message = "用户名长度不能小于4位！")
    @Length(max = 16,message = "用户名长度不能超过16位！")
    private String username;
    /** 密码 */
    @NotBlank(message = "密码不能为空！")
    @Length(min = 6,message = "密码长度不能小于6位！")
    @Length(max = 16,message = "密码长度不能超过16位！")
    private String password;

}
