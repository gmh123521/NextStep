-- 后台管理：为用户表增加角色列
USE `nextstep`;

-- MySQL 8.0 不支持 ALTER TABLE ... ADD COLUMN IF NOT EXISTS（那是 MariaDB 语法），
-- 用 information_schema 判断 + 预处理语句实现幂等，保证脚本可重复执行。
SET @col_exists = (
  SELECT COUNT(*) FROM `information_schema`.`COLUMNS`
   WHERE `TABLE_SCHEMA` = DATABASE()
     AND `TABLE_NAME`   = 'ns_user'
     AND `COLUMN_NAME`  = 'role'
);
SET @ddl = IF(@col_exists = 0,
  'ALTER TABLE `ns_user` ADD COLUMN `role` VARCHAR(16) NOT NULL DEFAULT ''USER'' COMMENT ''角色：USER=普通用户 ADMIN=管理员'' AFTER `status`',
  'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 将 id=2（开发环境的 admin 账号）提升为管理员
UPDATE `ns_user` SET `role` = 'ADMIN' WHERE `username` = 'admin';
