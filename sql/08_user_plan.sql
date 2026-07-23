-- M5 备考规划：主表 + 子任务表
USE `nextstep`;

-- 用户规划主表（一个用户同一时刻只有一份"当前规划"，重新生成会覆盖）
DROP TABLE IF EXISTS `ns_user_plan`;
CREATE TABLE `ns_user_plan` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`        BIGINT       NOT NULL,
  `path`           VARCHAR(8)   NOT NULL              COMMENT 'PG / CS / EM',
  `target_summary` VARCHAR(500)          DEFAULT NULL COMMENT '目标描述（如"冲击 985 计算机"）',
  `strategy`       TEXT                  DEFAULT NULL COMMENT '总体策略文本（LLM 生成的开篇分析）',
  `total_months`   INT          NOT NULL DEFAULT 6    COMMENT '规划总月数',
  `risk_alerts`    TEXT                  DEFAULT NULL COMMENT 'JSON 数组：风险预警列表',
  `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`        TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_path` (`user_id`, `path`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户备考/求职规划主表';

-- 规划下的任务项（按月份阶段 + 科目分组，支持勾选完成）
DROP TABLE IF EXISTS `ns_user_plan_task`;
CREATE TABLE `ns_user_plan_task` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `plan_id`     BIGINT       NOT NULL,
  `user_id`     BIGINT       NOT NULL              COMMENT '冗余 user_id 便于权限校验',
  `phase`       VARCHAR(32)  NOT NULL              COMMENT '阶段标签，如 "第1月" / "Month 1-2"',
  `phase_order` INT          NOT NULL DEFAULT 0    COMMENT '阶段排序（用于按月排序）',
  `subject`     VARCHAR(32)           DEFAULT NULL COMMENT '科目/类别，如 "数学" / "简历准备" / "面试"',
  `title`       VARCHAR(255) NOT NULL              COMMENT '任务标题',
  `description` VARCHAR(1000)         DEFAULT NULL COMMENT '具体内容（资料推荐、做法）',
  `order_idx`   INT          NOT NULL DEFAULT 0    COMMENT '同阶段内排序',
  `completed`   TINYINT      NOT NULL DEFAULT 0,
  `completed_at` DATETIME             DEFAULT NULL,
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted`     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_plan` (`plan_id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='规划任务项';
