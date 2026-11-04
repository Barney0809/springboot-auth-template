package com.fu.example.starter.model.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "aliyun.sms")
public class AliyunSmsProperties {
    /**
     * 接入点地址（必填，从阿里云文档获取，如：dypnsapi.aliyuncs.com）
     */
    private String endpoint;

    /**
     * 短信签名（必填，系统赠送的签名，从阿里云控制台获取）
     */
    private String signName = "速通互联验证码";

    /**
     * 模板CODE（必填，系统赠送的验证码模板，从控制台获取）
     */
    private String templateCode;

    /**
     * 验证码有效期（默认5分钟，可选）
     */
    private Integer codeExpireMinutes = 5;

    /**
     * 连接超时时间（默认3000毫秒，可选）
     */
    private Integer connectTimeout = 3000;

    /**
     * 读取超时时间（默认3000毫秒，可选）
     */
    private Integer readTimeout = 3000;

}
