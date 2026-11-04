package com.fu.example.starter.common;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.filter.SimplePropertyPreFilter;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Aspect
@Component
@Slf4j
public class LogAspect {

    // 统一管理敏感字段
    private static final Set<String> EXCLUDE_FIELDS_BEFORE = new HashSet<>(Arrays.asList("password", "file","checkPassword","code"));
    private static final Set<String> EXCLUDE_FIELDS_AROUND = new HashSet<>(Arrays.asList("mobile", "repData"));

    // 预建过滤器缓存
    private static final SimplePropertyPreFilter FILTER_BEFORE = new SimplePropertyPreFilter();
    private static final SimplePropertyPreFilter FILTER_AROUND = new SimplePropertyPreFilter();

    static {
        FILTER_BEFORE.getExcludes().addAll(EXCLUDE_FIELDS_BEFORE);
        FILTER_AROUND.getExcludes().addAll(EXCLUDE_FIELDS_AROUND);
    }

    public LogAspect() {
        log.info("LogAspect初始化成功！");
    }

    /**
     * 定义一个切点
     */
    @Pointcut("execution(public * com.fu..*Controller.*(..))")
    public void controllerPointcut() {
    }

    @Before("controllerPointcut()")
    public void doBefore(JoinPoint joinPoint) throws Throwable {
        // 使用 UUID 生成唯一日志 ID
        MDC.put("LOG_ID", UUID.randomUUID().toString().replace("-", "").substring(0, 16));

        // 开始打印请求日志
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            log.warn("无法获取请求上下文");
            return;
        }
        HttpServletRequest request = attributes.getRequest();
        Signature signature = joinPoint.getSignature();
        String name = signature.getName();

        // 打印业务操作
        String nameCn = getOperationName(name);

        // 使用反射，获取业务名称
        Class<?> clazz = signature.getDeclaringType();
        String businessName = getBusinessName(clazz);

        // 打印请求信息
        log.info("------------- 【{}】{}开始 -------------", businessName, nameCn);
        log.info("请求地址: {} {}", request.getRequestURL().toString(), request.getMethod());
        log.info("类名方法: {}.{}", signature.getDeclaringTypeName(), name);
        log.info("远程地址: {}", request.getRemoteAddr());

        // 打印请求参数
        Object[] args = joinPoint.getArgs();
        Object[] arguments = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof ServletRequest
                    || args[i] instanceof ServletResponse
                    || args[i] instanceof MultipartFile) {
                continue;
            }
            arguments[i] = args[i];
        }

        // 避免无效序列化
        log.info("请求参数: {}", JSON.toJSONString(arguments, FILTER_BEFORE));
    }

    @Around("controllerPointcut()")
    public Object doAround(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result;
        try {
            result = proceedingJoinPoint.proceed();
        } catch (Throwable t) {
            log.error("业务执行异常", t);
            throw t;
        }

        // 避免无效序列化
        log.info("返回结果: {}", JSON.toJSONString(result, FILTER_AROUND));
        log.info("------------- 结束 耗时：{} ms -------------", System.currentTimeMillis() - startTime);
        MDC.remove("LOG_ID"); // 清理MDC上下文
        return result;
    }

    private String getOperationName(String methodName) {
        if (methodName.contains("list") || methodName.contains("query")) {
            return "查询";
        } else if (methodName.contains("save")) {
            return "保存";
        } else if (methodName.contains("delete")) {
            return "删除";
        } else {
            return "操作";
        }
    }

    private String getBusinessName(Class<?> clazz) {
        try {
            Field field = clazz.getField("BUSINESS_NAME");
            if (Modifier.isPublic(field.getModifiers()) && Modifier.isStatic(field.getModifiers())) {
                return (String) field.get(null);
            }
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }
        return "未知业务";
    }
}
