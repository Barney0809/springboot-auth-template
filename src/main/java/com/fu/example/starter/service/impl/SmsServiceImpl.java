package com.fu.example.starter.service.impl;

import cn.hutool.core.util.StrUtil;
import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeRequest;
import com.aliyun.teautil.models.RuntimeOptions;
import com.fu.example.starter.constant.UserConstant;
import com.fu.example.starter.exception.BusinessException;
import com.fu.example.starter.exception.ExceptionCode;
import com.fu.example.starter.model.properties.AliyunSmsProperties;
import com.fu.example.starter.service.SmsService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;


@Service
@Slf4j
public class SmsServiceImpl implements SmsService {

    @Resource
    private AliyunSmsProperties smsProperties;

    @Resource
    private Client aliyunSmsClient;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    // 预编译正则表达式，提升性能
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern CODE_PATTERN = Pattern.compile("\\d{6}");

    /**
     * 发送短信验证码
     *
     * @param phoneNum 手机号
     */
    @Override
    public void sendSmsVerifyCode(String phoneNum) {
        // 1. 校验手机号
        if (phoneNum == null || phoneNum.length() != 11 || !phoneNum.matches("^1[3-9]\\d{9}$")) {
            throw new BusinessException(ExceptionCode.PARAMS_ERROR, "手机号格式错误！");
        }
        // 2. 校验验证码（假设6位数字）
//        if(code == null || code.length() != 6 || !code.matches("\\d{6}")){
//            throw new BusinessException(ExceptionCode.PARAMS_ERROR, "验证码必须为6位数字！");
//        }
        // 2. 生成验证码
        int code = (int) (Math.random() * 900000) + 100000; // 生成100000到999999之间的随机数
        // 3. 构建短信请求参数（模板参数必须含code和min）
        SendSmsVerifyCodeRequest request = new SendSmsVerifyCodeRequest()
                .setSignName(smsProperties.getSignName()) // 签名
                .setTemplateCode(smsProperties.getTemplateCode()) // 模板CODE
                .setPhoneNumber(phoneNum) // 手机号
                // 模板参数：JSON格式，必须包含"code"（验证码）和"min"（有效期）
                .setTemplateParam("{\"code\":\"" + code + "\",\"min\":\"" + smsProperties.getCodeExpireMinutes() + "\"}");
        try {
            redisTemplate.opsForValue().set(UserConstant.USER_LOGIN_CODE_KEY + phoneNum, code, smsProperties.getCodeExpireMinutes(), TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("redis写入异常！", e);
            throw new BusinessException(ExceptionCode.ERROR, "系统错误");
        }
        // 4. 配置运行时选项（超时时间可自定义）
        RuntimeOptions runtime = new RuntimeOptions()
                .setConnectTimeout(smsProperties.getConnectTimeout())
                .setReadTimeout(smsProperties.getReadTimeout());

        // 5. 调用接口发送短信（捕获异常可自定义处理）
        try {
            aliyunSmsClient.sendSmsVerifyCodeWithOptions(request, runtime);
            log.info("短信发送成功！手机号：{}，验证码：{}", phoneNum, code);
        } catch (Exception e) {
            log.error("短信发送失败！原因：{}", e.getMessage());
            throw new BusinessException(ExceptionCode.ERROR, "短信发送失败，请稍后重试");
        }

    }

    @Override
    public boolean verifySmsCode(String phoneNum, String code) {
        // 1. 校验手机号
        validatePhone(phoneNum);

        // 2. 校验验证码
        validateCode(code);

        // 3. Redis 获取验证码
        String codeInRedis;
        String redisKey = buildSmsCodeRedisKey(phoneNum);
        Object codeObj;
        try {
            codeObj = redisTemplate.opsForValue().get(redisKey);
        } catch (RuntimeException e) {
            log.error("Redis读取登录验证码异常，手机号：{}", phoneNum, e);
            throw new BusinessException(ExceptionCode.ERROR, "系统错误");
        }
        if (codeObj == null) {
            log.warn("验证码不存在或已过期，手机号：{}", phoneNum);
            throw new BusinessException(ExceptionCode.VALIDATION_ERROR, "验证码不存在或已过期！");
        }
        codeInRedis = codeObj.toString();
        // 4. 校验验证码（使用常量时间比较）
        if (codeInRedis.length() != code.length() ||
                !MessageDigest.isEqual(code.getBytes(StandardCharsets.UTF_8),
                        codeInRedis.getBytes(StandardCharsets.UTF_8))) {
            throw new BusinessException(ExceptionCode.OPERATION_ERROR, "验证码错误！");
        }

        // 5. 删除验证码
        try {
            redisTemplate.delete(redisKey);
        } catch (Exception e) {
            log.warn("Redis删除登录验证码异常，手机号：{}", phoneNum, e);
            // 注意：此处不抛异常，防止验证成功但删除失败导致的业务中断
        }

        return true;
    }

    // 私有方法：校验手机号
    private void validatePhone(String phoneNum) {
        if (phoneNum == null || phoneNum.length() != 11 || !PHONE_PATTERN.matcher(phoneNum).matches()) {
            throw new BusinessException(ExceptionCode.PARAMS_ERROR, "手机号格式错误！");
        }
    }

    // 私有方法：校验验证码
    private void validateCode(String code) {
        if (code == null || code.length() != 6 || !CODE_PATTERN.matcher(code).matches()) {
            throw new BusinessException(ExceptionCode.PARAMS_ERROR, "验证码必须为6位数字！");
        }
    }

    // 私有方法：构建 Redis Key
    private String buildSmsCodeRedisKey(String phoneNum) {
        return UserConstant.USER_LOGIN_CODE_KEY + phoneNum;
    }


}
