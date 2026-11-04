package com.fu.example.starter.constant;

import java.util.UUID;

public interface UserConstant {
    // 用户登录token
    String USER_LOGIN_KEY = "user:login:";
    String TOKEN_KEY = "token:";
    // 用户登录token过期时间 单位：小时
    int USER_LOGIN_TOKEN_EXPIRE = 24;

    String USER_LOGIN_CODE_KEY = "user:code:";

    String DEFAULT_RANDOM_PASSWORD = UUID.randomUUID().toString();
    // 随机头像数组
    String[] RANDOM_AVATARS = {
            "https://barney-fu.oss-cn-beijing.aliyuncs.com/FuBlog/defaultAvatar/1.jpg",
            "https://barney-fu.oss-cn-beijing.aliyuncs.com/FuBlog/defaultAvatar/2.jpg",
            "https://barney-fu.oss-cn-beijing.aliyuncs.com/FuBlog/defaultAvatar/3.jpg",
            "https://barney-fu.oss-cn-beijing.aliyuncs.com/FuBlog/defaultAvatar/4.jpg",
            "https://barney-fu.oss-cn-beijing.aliyuncs.com/FuBlog/defaultAvatar/5.jpg"
    };

    // 随机头像个数
    int RANDOM_AVATAR_COUNT = UserConstant.RANDOM_AVATARS.length;

    String DEFAULT_NICKNAME_PREFIX = "FU用户";

    String REGISTER_LOCK_KEY = "register:lock:";
}
