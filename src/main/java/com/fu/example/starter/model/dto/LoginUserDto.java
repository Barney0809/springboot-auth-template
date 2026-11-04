package com.fu.example.starter.model.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class LoginUserDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 用户id
    private Long id;
    // 用户名
    private String username;
    // 昵称
    private String nickname;
    // 头像
    private String avatar;
    // 个人简介
    private String profile;
    // 角色
    private String role;
    // 创建时间
    private LocalDateTime createTime;
    // 更新时间
    private LocalDateTime updateTime;
    // 是否删除
    private Boolean isDelete;
    // token
    private String token;

}
