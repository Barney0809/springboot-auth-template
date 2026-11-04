-- =====================================================================
--  starter 模板建库脚本
--  MySQL 8.0+
--  用法：mysql -uroot -p < sql/schema.sql
-- =====================================================================

CREATE DATABASE IF NOT EXISTS `starter_db`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

USE `starter_db`;

DROP TABLE IF EXISTS `user`;

CREATE TABLE `user`
(
    `id`          bigint      NOT NULL COMMENT 'ID（雪花算法生成）',
    `username`    varchar(64) NOT NULL COMMENT '账号',
    `password`    varchar(128) NOT NULL COMMENT '密码摘要：MD5(盐 + 明文)，盐见 auth.password-salt',
    `nickname`    varchar(64)  DEFAULT NULL COMMENT '用户昵称',
    `avatar`      varchar(512) DEFAULT NULL COMMENT '用户头像 URL',
    `profile`     varchar(512) DEFAULT NULL COMMENT '用户简介',
    `role`        varchar(32) NOT NULL DEFAULT 'USER' COMMENT '角色：USER / ADMIN',
    `create_time` datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`   tinyint(1)  NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删、1-已删',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户';
