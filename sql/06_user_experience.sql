-- M4 用户经历子表（实习/项目/科研/奖项）
USE `nextstep`;

DROP TABLE IF EXISTS `ns_user_experience`;
CREATE TABLE `ns_user_experience` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT       NOT NULL,
  `type`        VARCHAR(16)  NOT NULL              COMMENT 'INTERNSHIP/PROJECT/AWARD/RESEARCH/PAPER/COMPETITION',
  `title`       VARCHAR(255) NOT NULL              COMMENT '公司/项目/奖项 名称',
  `role`        VARCHAR(128)          DEFAULT NULL COMMENT '职位/角色',
  `start_date`  VARCHAR(16)           DEFAULT NULL COMMENT 'YYYY-MM 或 YYYY',
  `end_date`    VARCHAR(16)           DEFAULT NULL,
  `description` TEXT                    DEFAULT NULL COMMENT '描述/成果摘要',
  `summary`     VARCHAR(500)             DEFAULT NULL COMMENT 'AI 摘要，供学长对话上下文用',
  `source`      VARCHAR(16)  NOT NULL DEFAULT 'MANUAL' COMMENT 'RESUME/MANUAL/CHAT',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_user_type` (`user_id`, `type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户经历（实习/项目/奖项等）';
