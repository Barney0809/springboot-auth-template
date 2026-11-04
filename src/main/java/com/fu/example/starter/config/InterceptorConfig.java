package com.fu.example.starter.config;

import com.fu.example.starter.interceptor.AuthInterceptor;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class InterceptorConfig implements WebMvcConfigurer {

    @Resource
    private AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/api/v1/sys/login",
                        "/doc.html/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/swagger-resources/**",
                        "/webjars/**",
                        "/swagger-ui.html/**",
                        "/api",
                        "/api-docs",
                        "/api-docs/**",

                        "/user/login",
                        "/user/send",
                        "/user/verify",
                        "/user/sms/**",
                        "/user/register"
//                        "/user/**"
                )
        ;
    }
}
