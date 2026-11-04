package com.fu.example.starter.service;

public interface SmsService {

    void sendSmsVerifyCode(String phoneNum);
    boolean verifySmsCode(String phoneNum, String code);
}
