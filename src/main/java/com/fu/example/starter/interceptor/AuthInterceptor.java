package com.fu.example.starter.interceptor;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.fu.example.starter.common.UserContextHolder;
import com.fu.example.starter.constant.UserConstant;
import com.fu.example.starter.exception.BusinessException;
import com.fu.example.starter.exception.ExceptionCode;
import com.fu.example.starter.model.dto.LoginUserDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Enumeration;
import java.util.Map;

/**
 * 拦截器：Spring框架特有的，常用于登录校验，权限校验，请求日志打印
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    public final RedisTemplate<String, Object> redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //String method = request.getMethod();
        if("OPTIONS".equalsIgnoreCase(request.getMethod())){
            log.error("OPTIONS请求，不做处理");
            return true;
        }
        // 打印所有请求头，用于调试
//        Enumeration<String> headerNames = request.getHeaderNames();
//        while (headerNames.hasMoreElements()) {
//            String headerName = headerNames.nextElement();
//            String headerValue = request.getHeader(headerName);
//            log.info("{}: {}", headerName, headerValue);
//        }
        // 1. 从请求头（或Cookie）中获取token，例如 header: "authorization"
        String token = request.getHeader("authorization");
        if (StrUtil.isBlank(token)) {
            // token为空，未登录，拦截请求
            log.error("token为空，未登录，拦截请求");
            response.setStatus(401);
            throw new BusinessException(ExceptionCode.USER_NOT_LOGIN,"当前用户未登录");
        }
        log.info("获取到当前token：{}", token);
        // 2. 基于token从Redis中获取用户

        String tokenKey = UserConstant.TOKEN_KEY + token;
        Object usernameObj = redisTemplate.opsForValue().get(tokenKey);
        if (usernameObj == null) {
            // token无效，未登录，拦截请求
            log.error("token无效，未登录，拦截请求");
            response.setStatus(401);
            throw new BusinessException(ExceptionCode.USER_NOT_LOGIN,"当前用户未登录");
        }
        String userHashKey = UserConstant.USER_LOGIN_KEY + usernameObj;
        Map<Object, Object> userMap = redisTemplate.opsForHash().entries(userHashKey);

        // 3. 判断用户是否存在
        if (userMap.isEmpty()) {
            // 用户不存在，拦截请求
            log.error("User对象不存在");
            response.setStatus(401);
            throw new BusinessException(ExceptionCode.ERROR,"系统错误");
        }

        // 4. 将查询到的Hash数据转为UserDTO对象
        LoginUserDto loginUserDto = BeanUtil.fillBeanWithMap(userMap, new LoginUserDto(), false);
        log.info("当前用户：{}", loginUserDto);
        // 5. 将用户信息保存到ThreadLocal，方便后续Controller使用
        UserContextHolder.setLoginUser(loginUserDto);
        // 6. 刷新token有效期（可选）
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 请求结束后移除用户，避免内存泄漏
        UserContextHolder.clear();
    }
}
