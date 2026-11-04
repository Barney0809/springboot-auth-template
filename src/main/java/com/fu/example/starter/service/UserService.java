package com.fu.example.starter.service;

import com.fu.example.starter.model.dto.LoginUserDto;
import com.fu.example.starter.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.fu.example.starter.model.request.LoginRequest;
import com.fu.example.starter.model.request.RegisterRequest;
import com.fu.example.starter.model.request.UserSmsRequest;
import jakarta.servlet.http.HttpServletRequest;

/**
 * <p>
 * 用户 服务类
 * </p>
 *
 * @author FU
 * @since 2025-11-11
 */
public interface UserService extends IService<User> {

    LoginUserDto login(LoginRequest loginRequest);

    //短信注册
//    LoginUserDto smsRegister(UserSmsRequest userSmsRequest);

    // 短信登录和注册
    LoginUserDto smsLoginOrRegister(UserSmsRequest userSmsRequest);

    // 获取当前用户
    LoginUserDto getCurrentUser();

    // 注销
    void logout();

    // 注册
    Long register(RegisterRequest registerRequest);
}
