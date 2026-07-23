-- NextStep 初始化脚本（M1 仅用户表，后续模块补充）

CREATE DATABASE IF NOT EXISTS `nextstep`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE `nextstep`;

DROP TABLE IF EXISTS `ns_user`;
CREATE TABLE `ns_user` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `username`   VARCHAR(64)  NOT NULL COMMENT '用户名',
  `password`   VARCHAR(128) NOT NULL COMMENT 'BCrypt 密码',
  `nickname`   VARCHAR(64)           DEFAULT NULL,
  `email`      VARCHAR(128)          DEFAULT NULL,
  `phone`      VARCHAR(32)           DEFAULT NULL,
  `status`     TINYINT      NOT NULL DEFAULT 0 COMMENT '0=正常 1=禁用',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
