package com.fu.example.starter.controller;

import com.fu.example.starter.common.CommonResponse;
import com.fu.example.starter.model.dto.LoginUserDto;
import com.fu.example.starter.model.request.LoginRequest;
import com.fu.example.starter.model.request.RegisterRequest;
import com.fu.example.starter.model.request.UserSmsRequest;
import com.fu.example.starter.service.SmsService;
import com.fu.example.starter.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 用户 前端控制器
 * </p>
 *
 * @author FU
 * @since 2025-11-11
 */
@RestController
@RequestMapping("/user")
@Validated
public class UserController {

    public static final String BUSINESS_NAME = "用户";

    @Resource
    private UserService userService;

    @Resource
    private SmsService smsService;

    /**
     * 用户名登录
     * @param loginRequest 登录参数
     * @return 登录用户信息
     */
    @PostMapping("/login")
    @Operation(summary = "用户名登录")
    public CommonResponse<LoginUserDto> login (@RequestBody @Valid LoginRequest loginRequest){
       return CommonResponse.success(userService.login(loginRequest));
    }

    /**
     * 发送短信验证码
     * @param phoneNum 手机号
     * @return 无
     */
    @PostMapping("/send")
    @Operation(summary = "发送短信验证码")
    public CommonResponse<Void> sendSmsVerifyCode (@RequestParam @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式错误！")
                                                       String phoneNum){
        smsService.sendSmsVerifyCode(phoneNum);
        return CommonResponse.success();
    }

    /**
     * 手机号登录
     * @param userSmsRequest 登录参数
     * @return 登录用户信息
     */
    @PostMapping("/sms/login")
    @Operation(summary = "手机号登录")
    public CommonResponse<LoginUserDto> smsLoginOrRegister (@RequestBody @Valid UserSmsRequest userSmsRequest){
        return CommonResponse.success(userService.smsLoginOrRegister(userSmsRequest));
    }

    /**
     * 获取当前用户信息
     * @return 当前用户信息
     */
    @GetMapping("/current")
    @Operation(summary = "获取当前用户信息")
    public CommonResponse<LoginUserDto> getCurrentUser(){
        return CommonResponse.success(userService.getCurrentUser());
    }

    /**
     * 登出
     * @return 无
     */
    @PostMapping("/logout")
    @Operation(summary = "登出")
    public CommonResponse<Void> logout(){
        userService.logout();
        return CommonResponse.success();
    }

    /**
     * 用户名注册
     * @param registerRequest 注册参数
     * @return 注册用户id
     */
    @PostMapping("/register")
    @Operation(summary = "注册")
    public CommonResponse<Long> register (@RequestBody @Valid RegisterRequest registerRequest){
        return CommonResponse.success(userService.register(registerRequest));
    }
}
