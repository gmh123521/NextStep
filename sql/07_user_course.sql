-- M4-4 用户成绩单课程明细
USE `nextstep`;

DROP TABLE IF EXISTS `ns_user_course`;
CREATE TABLE `ns_user_course` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT       NOT NULL,
  `course_name` VARCHAR(255) NOT NULL              COMMENT '课程名',
  `credit`      DECIMAL(4,2)          DEFAULT NULL COMMENT '学分',
  `score`       DECIMAL(5,2)          DEFAULT NULL COMMENT '百分制成绩',
  `gpa`         DECIMAL(4,2)          DEFAULT NULL COMMENT '4 分制 GPA（按校规换算）',
  `semester`    VARCHAR(32)           DEFAULT NULL COMMENT '学期，如 2024-2025-1',
  `category`    VARCHAR(32)           DEFAULT NULL COMMENT '课程类别（必修/选修/公共/...）',
  `source`      VARCHAR(16)  NOT NULL DEFAULT 'TRANSCRIPT' COMMENT 'TRANSCRIPT/MANUAL',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_user_semester` (`user_id`, `semester`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户成绩单课程';
