-- M2 考研路径：院校、专业、分数线、报录比
USE `nextstep`;

-- 院校
DROP TABLE IF EXISTS `ns_school`;
CREATE TABLE `ns_school` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `name`         VARCHAR(128) NOT NULL                COMMENT '院校名称',
  `code`         VARCHAR(16)           DEFAULT NULL   COMMENT '学校代码',
  `province`     VARCHAR(32)           DEFAULT NULL   COMMENT '所在省份',
  `city`         VARCHAR(32)           DEFAULT NULL   COMMENT '所在城市',
  `level`        VARCHAR(16)  NOT NULL DEFAULT 'REGULAR' COMMENT 'C9/985/211/DOUBLE_FIRST/REGULAR/COLLEGE',
  `type`         VARCHAR(16)           DEFAULT NULL   COMMENT '类型：综合/理工/师范/...',
  `is_self_marking` TINYINT   NOT NULL DEFAULT 0      COMMENT '是否 34 所自划线',
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`),
  KEY `idx_level` (`level`),
  KEY `idx_province` (`province`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='院校';

-- 招生专业（学校 × 专业）
DROP TABLE IF EXISTS `ns_school_major`;
CREATE TABLE `ns_school_major` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `school_id`    BIGINT       NOT NULL,
  `major_code`   VARCHAR(16)  NOT NULL                COMMENT '专业代码',
  `major_name`   VARCHAR(128) NOT NULL                COMMENT '专业名称',
  `category`     VARCHAR(32)           DEFAULT NULL   COMMENT '学科门类',
  `degree_type`  VARCHAR(16)           DEFAULT 'ACADEMIC' COMMENT 'ACADEMIC 学硕 / PROFESSIONAL 专硕',
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_school_major` (`school_id`, `major_code`),
  KEY `idx_major_name` (`major_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='院校专业';

-- 历年录取数据（招生人数 + 报名人数 + 复试线）
DROP TABLE IF EXISTS `ns_school_enroll`;
CREATE TABLE `ns_school_enroll` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `school_id`    BIGINT       NOT NULL,
  `major_id`     BIGINT       NOT NULL                COMMENT '关联 ns_school_major.id',
  `year`         SMALLINT     NOT NULL                COMMENT '年份',
  `enroll_plan`  INT                   DEFAULT NULL   COMMENT '招生计划',
  `enroll_actual` INT                  DEFAULT NULL   COMMENT '实际录取',
  `apply_count`  INT                   DEFAULT NULL   COMMENT '报考人数',
  `cutoff_score` INT                   DEFAULT NULL   COMMENT '复试线总分',
  `cutoff_english` INT                 DEFAULT NULL   COMMENT '英语单科线',
  `cutoff_political` INT               DEFAULT NULL   COMMENT '政治单科线',
  `lowest_score` INT                   DEFAULT NULL   COMMENT '最低录取分',
  `highest_score` INT                  DEFAULT NULL   COMMENT '最高录取分',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_major_year` (`major_id`, `year`),
  KEY `idx_year` (`year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考研历年招录';
