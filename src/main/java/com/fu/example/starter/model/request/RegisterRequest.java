package com.fu.example.starter.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.io.Serial;
import java.io.Serializable;

@Data
public class RegisterRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户名 */
    @NotBlank(message = "用户名不能为空")
    @Length(min = 4, max = 16,message = "用户名长度在4-16之间")
    private String username;
    /** 密码 */
    @NotBlank(message = "密码不能为空")
    @Length(min = 6, max = 16,message = "密码长度在6-16之间")
    private String password;
    /** 确认密码 */
    @NotBlank(message = "确认密码不能为空")
    @Length(min = 6, max = 16, message = "密码长度在6-16之间")
    private String checkPassword;

}
