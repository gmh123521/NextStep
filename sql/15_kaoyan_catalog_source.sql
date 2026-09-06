USE `nextstep`;

-- 为已存在的原始记录表补充标准化结果列，便于审计和重新发布。
SET @normalized_column_exists = (
  SELECT COUNT(*) FROM `information_schema`.`COLUMNS`
   WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = 'ns_data_raw_record'
     AND `COLUMN_NAME` = 'normalized_payload'
);
SET @ddl = IF(@normalized_column_exists = 0,
  'ALTER TABLE `ns_data_raw_record` ADD COLUMN `normalized_payload` LONGTEXT NULL COMMENT ''解析后的标准化 JSON'' AFTER `raw_payload`',
  'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 研招网专业目录真实 POST 接口；只替换之前提交的旧默认地址，保留管理员自定义地址。
UPDATE `ns_data_source`
   SET `source_url` = 'https://yz.chsi.com.cn/zsml/rs/zys.do',
       `source_type` = 'JSON',
       `parser_version` = 'v2'
 WHERE `source_code` = 'KAOYAN_CATALOG'
   AND (`source_url` IS NULL OR `source_url` = ''
        OR `source_url` = 'https://yz.chsi.com.cn/zsml/queryAction.do');

INSERT INTO `ns_data_source`
  (`source_code`, `source_name`, `organization`, `source_type`, `source_url`, `enabled`, `parser_version`, `update_frequency`, `license_note`)
VALUES
  ('KAOYAN_CATALOG', '考研专业目录', '中国研究生招生信息网', 'JSON', 'https://yz.chsi.com.cn/zsml/rs/zys.do', 1, 'v2', '按年度发布', '仅采集公开且允许使用的数据')
ON DUPLICATE KEY UPDATE
  `source_url` = CASE
      WHEN `source_url` IS NULL OR `source_url` = ''
           OR `source_url` = 'https://yz.chsi.com.cn/zsml/queryAction.do'
      THEN VALUES(`source_url`) ELSE `source_url` END,
  `source_type` = CASE
      WHEN `source_type` IS NULL OR `source_type` = '' OR `source_type` = 'XLSX'
      THEN VALUES(`source_type`) ELSE `source_type` END,
  `parser_version` = CASE
      WHEN `parser_version` IS NULL OR `parser_version` = '' OR `parser_version` = 'v1'
      THEN VALUES(`parser_version`) ELSE `parser_version` END;
