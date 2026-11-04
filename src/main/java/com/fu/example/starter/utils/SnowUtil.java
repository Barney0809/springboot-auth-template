package com.fu.example.starter.utils;

import cn.hutool.core.util.IdUtil;

public class SnowUtil {
    public static String getSnowflakeIdStr() {
        return IdUtil.getSnowflake().nextIdStr();
    }
    public static long getSnowflakeId() {
        return IdUtil.getSnowflake().nextId();
    }
}
