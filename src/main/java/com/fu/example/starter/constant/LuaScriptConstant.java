package com.fu.example.starter.constant;

import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

public class LuaScriptConstant {
    public static final RedisScript<Long> UNLOCK_REDIS_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('get', KEYS[1]) == ARGV[1] then
              return redis.call('del', KEYS[1])
            else
              return -1
            end
            """, Long.class);
}
