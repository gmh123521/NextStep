-- 真实数据采集通用元数据：来源、批次、原始记录
USE `nextstep`;

CREATE TABLE IF NOT EXISTS `ns_data_source` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT,
  `source_code`     VARCHAR(64)  NOT NULL COMMENT '数据源编码',
  `source_name`     VARCHAR(128) NOT NULL COMMENT '数据源名称',
  `organization`    VARCHAR(128)          DEFAULT NULL COMMENT '发布机构',
  `source_type`     VARCHAR(16)  NOT NULL DEFAULT 'HTTP' COMMENT 'HTTP/JSON/CSV/XLSX/PDF',
  `source_url`      VARCHAR(512)          DEFAULT NULL COMMENT '来源地址',
  `enabled`         TINYINT      NOT NULL DEFAULT 1,
  `update_frequency` VARCHAR(32)         DEFAULT NULL COMMENT '更新频率说明',
  `license_note`    VARCHAR(512)          DEFAULT NULL COMMENT '使用许可备注',
  `parser_version`  VARCHAR(32)  NOT NULL DEFAULT 'v1',
  `last_success_at` DATETIME              DEFAULT NULL,
  `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_data_source_code` (`source_code`),
  KEY `idx_data_source_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='真实数据来源';

CREATE TABLE IF NOT EXISTS `ns_data_import_batch` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT,
  `source_code`     VARCHAR(64)  NOT NULL,
  `data_year`       SMALLINT     NOT NULL,
  `content_hash`    VARCHAR(128) NOT NULL,
  `parser_version`  VARCHAR(32)  NOT NULL,
  `source_url`      VARCHAR(512)          DEFAULT NULL,
  `snapshot_path`   VARCHAR(512)          DEFAULT NULL,
  `status`          VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/SUCCEEDED/FAILED/REJECTED/PUBLISHED',
  `total_count`     INT          NOT NULL DEFAULT 0,
  `success_count`   INT          NOT NULL DEFAULT 0,
  `skipped_count`   INT          NOT NULL DEFAULT 0,
  `failed_count`    INT          NOT NULL DEFAULT 0,
  `error_message`   VARCHAR(500)          DEFAULT NULL,
  `started_at`      DATETIME              DEFAULT NULL,
  `finished_at`     DATETIME              DEFAULT NULL,
  `published_at`    DATETIME              DEFAULT NULL,
  `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_import_source_year_hash` (`source_code`, `data_year`, `content_hash`),
  KEY `idx_import_source_status` (`source_code`, `status`),
  KEY `idx_import_year` (`data_year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据导入批次';

CREATE TABLE IF NOT EXISTS `ns_data_raw_record` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT,
  `batch_id`        BIGINT       NOT NULL,
  `record_no`       INT          NOT NULL,
  `raw_payload`     LONGTEXT     NOT NULL,
  `payload_hash`    VARCHAR(128)          DEFAULT NULL,
  `parse_status`    VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SUCCESS/FAILED/SKIPPED',
  `error_message`   VARCHAR(500)          DEFAULT NULL,
  `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_raw_batch_record` (`batch_id`, `record_no`),
  KEY `idx_raw_batch_status` (`batch_id`, `parse_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据原始记录';

INSERT INTO `ns_data_source`
  (`source_code`, `source_name`, `organization`, `source_type`, `source_url`, `update_frequency`, `license_note`)
VALUES
  ('KAOYAN_SCHOOL', '考研招生单位', '公开招生信息发布机构', 'JSON', NULL, '按年度或源站更新', '仅采集公开且允许使用的数据'),
  ('KAOYAN_CATALOG', '考研专业目录', '高校研究生招生单位', 'XLSX', NULL, '按年度发布', '需保留原始来源和使用许可')
ON DUPLICATE KEY UPDATE
  `source_name` = VALUES(`source_name`),
  `organization` = VALUES(`organization`),
  `source_type` = VALUES(`source_type`),
  `update_frequency` = VALUES(`update_frequency`),
  `license_note` = VALUES(`license_note`);
