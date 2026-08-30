-- M2 画像表
USE `nextstep`;

DROP TABLE IF EXISTS `ns_user_profile`;
CREATE TABLE `ns_user_profile` (
  `id`                    BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`               BIGINT       NOT NULL                 COMMENT '关联 ns_user.id',

  -- 学业基础
  `current_school`        VARCHAR(128)          DEFAULT NULL    COMMENT '当前院校',
  `school_level`          VARCHAR(16)           DEFAULT NULL    COMMENT '院校层次：C9/985/211/DOUBLE_FIRST/REGULAR/COLLEGE',
  `current_major`         VARCHAR(128)          DEFAULT NULL    COMMENT '当前专业',
  `major_category`        VARCHAR(32)           DEFAULT NULL    COMMENT '学科门类',
  `degree_type`           VARCHAR(16)           DEFAULT NULL    COMMENT '学历：BACHELOR/MASTER/DOCTOR',
  `grade_year`            TINYINT               DEFAULT NULL    COMMENT '年级：1-4 本科 / 5-7 硕博',
  `gpa`                   DECIMAL(5,2)          DEFAULT NULL    COMMENT '绩点 0-100',
  `gpa_scale`             TINYINT               DEFAULT 4       COMMENT 'GPA 满分制：4 / 5 / 100',
  `class_rank_pct`        DECIMAL(5,2)          DEFAULT NULL    COMMENT '班级排名百分位 0-100',

  -- 能力素质
  `english_level`         VARCHAR(16)           DEFAULT NULL    COMMENT 'CET4/CET6/IELTS/TOEFL/NONE',
  `english_score`         INT                   DEFAULT NULL    COMMENT '英语分数',
  -- 注意：has_research / has_internship / has_competition / has_paper
  --      已改为派生字段（从 ns_user_experience 聚合），不再存数据库

  -- 偏好与目标
  `target_paths`          VARCHAR(64)           DEFAULT NULL    COMMENT '目标路径，多选逗号分隔：PG,CS,EM',
  `preferred_regions`     VARCHAR(255)          DEFAULT NULL    COMMENT '偏好城市，逗号分隔',
  `preferred_industries`  VARCHAR(255)          DEFAULT NULL    COMMENT '偏好行业，逗号分隔',
  `salary_expectation`    INT                   DEFAULT NULL    COMMENT '期望月薪（人民币）',

  -- 风险/家庭
  `risk_appetite`         TINYINT               DEFAULT 3       COMMENT '风险偏好 1-5（保守→激进）',
  `monthly_budget`        INT                   DEFAULT NULL    COMMENT '每月可承受备考开销（元）',

  -- 个性化
  `interests`             VARCHAR(512)          DEFAULT NULL    COMMENT '兴趣描述',
  `strengths`             VARCHAR(512)          DEFAULT NULL    COMMENT '优势',
  `weaknesses`            VARCHAR(512)          DEFAULT NULL    COMMENT '劣势',

  -- 状态
  `current_status`        VARCHAR(16)           DEFAULT 'IN_SCHOOL' COMMENT 'IN_SCHOOL/GRADUATED/EMPLOYED/PREPARING',

  `created_at`            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`               TINYINT      NOT NULL DEFAULT 0,

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户画像';
