package com.fu.example.starter.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 统一响应封装的契约测试。
 *
 * <p>前端按 {@code code == 0} 判断成功，所以这几个静态工厂的语义不能随意改。
 */
class CommonResponseTest {

    @Test
    void success_carriesZeroCodeAndPayload() {
        var response = CommonResponse.success("payload");

        assertEquals(0, response.getCode());
        assertEquals("payload", response.getData());
    }

    @Test
    void successWithoutPayload_hasNullData() {
        var response = CommonResponse.success();

        assertEquals(0, response.getCode());
        assertNull(response.getData());
    }

    @Test
    void error_keepsGivenCodeAndMessage() {
        var response = CommonResponse.error(4001, "参数错误");

        assertEquals(4001, response.getCode());
        assertEquals("参数错误", response.getMessage());
        assertNull(response.getData());
    }

    @Test
    void errorFromBusinessException_reusesExceptionCode() {
        var exception = new com.fu.example.starter.exception.BusinessException(
                com.fu.example.starter.exception.ExceptionCode.PARAMS_ERROR, "手机号格式错误");

        var response = CommonResponse.error(exception);

        assertNull(response.getData());
        assertEquals(exception.getCode(), response.getCode());
        assertEquals("手机号格式错误", response.getMessage());
    }
}
