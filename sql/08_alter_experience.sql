-- 迁移：description 从 VARCHAR(1024) 扩至 TEXT，新增 summary 字段
USE `nextstep`;

ALTER TABLE `ns_user_experience`
  MODIFY COLUMN `description` TEXT COMMENT '描述/成果摘要',
  ADD COLUMN `summary` VARCHAR(500) DEFAULT NULL COMMENT 'AI 摘要，供学长对话上下文用' AFTER `description`;