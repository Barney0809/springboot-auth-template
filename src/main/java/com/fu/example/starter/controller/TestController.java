package com.fu.example.starter.controller;

import com.fu.example.starter.common.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    public static final String BUSINESS_NAME = "测试";

    @GetMapping("/test")
    @Operation(summary = "测试")
    public CommonResponse<String> health(){
        return CommonResponse.success("success");
    }
}
