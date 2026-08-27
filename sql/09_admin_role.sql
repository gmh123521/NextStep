-- 后台管理：为用户表增加角色列
USE `nextstep`;

ALTER TABLE `ns_user`
  ADD COLUMN `role` VARCHAR(16) NOT NULL DEFAULT 'USER'
  COMMENT '角色：USER=普通用户 ADMIN=管理员' AFTER `status`;

-- 将 id=2（开发环境的 admin 账号）提升为管理员
UPDATE `ns_user` SET `role` = 'ADMIN' WHERE `username` = 'admin';
