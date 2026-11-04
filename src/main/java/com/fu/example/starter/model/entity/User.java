package com.fu.example.starter.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 用户
 * </p>
 *
 * @author FU
 * @since 2025-11-11
 */
@Data
@TableName("user")
@Schema(name = "User", description = "用户")
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId("id")
    @Schema(description = "ID")
    private Long id;

    /**
     * 账号
     */
    @TableField("username")
    @Schema(description = "账号")
    private String username;

    /**
     * 密码
     */
    @TableField("password")
    @Schema(description = "密码")
    private String password;

    /**
     * 用户昵称
     */
    @TableField("nickname")
    @Schema(description = "用户昵称")
    private String nickname;

    /**
     * 用户头像
     */
    @TableField("avatar")
    @Schema(description = "用户头像")
    private String avatar;

    /**
     * 用户简介
     */
    @TableField("profile")
    @Schema(description = "用户简介")
    private String profile;

    /**
     * 用户角色：USER/ADMIN…
     */
    @TableField("role")
    @Schema(description = "用户角色：USER/ADMIN…")
    private String role;

    /**
     * 创建时间
     */
    @TableField("create_time")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /**
     * 是否删除：0-未删、1-已删
     */
    @TableField("is_delete")
    @Schema(description = "是否删除：0-未删、1-已删")
    private Boolean isDelete;
}
