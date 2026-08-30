-- 扩展 GPA 精度，支持 100 分制满分 100.00
USE `nextstep`;

ALTER TABLE `ns_user_profile`
  MODIFY COLUMN `gpa` DECIMAL(5,2) DEFAULT NULL COMMENT '绩点 0-100';
