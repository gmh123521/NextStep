-- M2 考公路径：岗位、招录、进面线
USE `nextstep`;

-- 岗位（按年份维护）
DROP TABLE IF EXISTS `ns_gov_post`;
CREATE TABLE `ns_gov_post` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT,
  `year`            SMALLINT     NOT NULL              COMMENT '考试年份',
  `exam_type`       VARCHAR(16)  NOT NULL              COMMENT 'NATIONAL 国考 / PROVINCIAL 省考',
  `province`        VARCHAR(32)           DEFAULT NULL COMMENT '省份（省考必填）',
  `dept_name`       VARCHAR(128)          DEFAULT NULL COMMENT '招录单位',
  `post_code`       VARCHAR(32)           DEFAULT NULL COMMENT '职位代码',
  `post_name`       VARCHAR(128) NOT NULL              COMMENT '职位名称',
  `region`          VARCHAR(64)           DEFAULT NULL COMMENT '工作地点',
  `degree_required` VARCHAR(16)           DEFAULT NULL COMMENT 'BACHELOR/MASTER/DOCTOR/ANY',
  `major_required`  VARCHAR(255)          DEFAULT NULL COMMENT '专业要求',
  `political`       VARCHAR(32)           DEFAULT NULL COMMENT '政治面貌要求',
  `extra_required`  VARCHAR(512)          DEFAULT NULL COMMENT '其他条件（基层经验/性别/证书等）',
  `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_year_dept_code` (`year`, `exam_type`, `dept_name`, `post_code`),
  KEY `idx_year_type` (`year`, `exam_type`),
  KEY `idx_province` (`province`),
  KEY `idx_post_name` (`post_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考公岗位';

-- 岗位招录情况
DROP TABLE IF EXISTS `ns_gov_enroll`;
CREATE TABLE `ns_gov_enroll` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `post_id`      BIGINT       NOT NULL,
  `enroll_count` INT          NOT NULL DEFAULT 0      COMMENT '招录人数',
  `apply_count`  INT                   DEFAULT NULL   COMMENT '报考人数（过审）',
  `apply_pass`   INT                   DEFAULT NULL   COMMENT '通过审查',
  `attend_count` INT                   DEFAULT NULL   COMMENT '实际参加考试人数',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_post` (`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考公招录数据';

-- 进面分数线
DROP TABLE IF EXISTS `ns_gov_cutoff`;
CREATE TABLE `ns_gov_cutoff` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `post_id`      BIGINT       NOT NULL,
  `interview_min` DECIMAL(5,2)         DEFAULT NULL   COMMENT '进面最低分',
  `interview_max` DECIMAL(5,2)         DEFAULT NULL   COMMENT '进面最高分',
  `final_min`    DECIMAL(5,2)          DEFAULT NULL   COMMENT '录用最低综合分',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_post` (`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考公进面/录用线';
