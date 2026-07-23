-- M2 就业路径：行业、岗位、薪资
USE `nextstep`;

-- 行业
DROP TABLE IF EXISTS `ns_industry`;
CREATE TABLE `ns_industry` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `code`       VARCHAR(16)  NOT NULL              COMMENT '行业代码',
  `name`       VARCHAR(64)  NOT NULL              COMMENT '行业名称',
  `parent_id`  BIGINT                DEFAULT 0    COMMENT '父行业 0=顶级',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='行业';

-- 岗位（如 Java 后端开发 / 数据分析师 / 产品经理）
DROP TABLE IF EXISTS `ns_job_position`;
CREATE TABLE `ns_job_position` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `name`         VARCHAR(64)  NOT NULL              COMMENT '岗位名称',
  `industry_id`  BIGINT                DEFAULT NULL,
  `category`     VARCHAR(32)           DEFAULT NULL COMMENT '一级分类：技术/产品/运营/...',
  `description`  VARCHAR(512)          DEFAULT NULL,
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name_industry` (`name`, `industry_id`),
  KEY `idx_industry` (`industry_id`),
  KEY `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='岗位';

-- 薪资统计（按城市 × 岗位 × 经验聚合）
DROP TABLE IF EXISTS `ns_salary_stat`;
CREATE TABLE `ns_salary_stat` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT,
  `position_id`    BIGINT       NOT NULL,
  `city`           VARCHAR(32)  NOT NULL,
  `experience`     VARCHAR(16)  NOT NULL              COMMENT 'FRESH 应届 / 1-3Y / 3-5Y / 5Y+',
  `degree`         VARCHAR(16)  NOT NULL DEFAULT 'ANY' COMMENT 'BACHELOR/MASTER/DOCTOR/ANY',
  `min_salary`     INT                   DEFAULT NULL COMMENT '月薪下限',
  `max_salary`     INT                   DEFAULT NULL,
  `median_salary`  INT                   DEFAULT NULL,
  `sample_size`    INT                   DEFAULT NULL COMMENT '样本量',
  `data_source`    VARCHAR(32)           DEFAULT NULL COMMENT '数据来源',
  `stat_year`      SMALLINT     NOT NULL,
  `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pos_city_exp_deg_year` (`position_id`, `city`, `experience`, `degree`, `stat_year`),
  KEY `idx_city` (`city`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='薪资统计';
