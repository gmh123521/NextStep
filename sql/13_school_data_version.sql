-- 考研专业目录按年度保留版本
USE `nextstep`;

SET @year_exists = (
  SELECT COUNT(*) FROM `information_schema`.`COLUMNS`
   WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = 'ns_school_major' AND `COLUMN_NAME` = 'year'
);
SET @ddl = IF(@year_exists = 0,
  'ALTER TABLE `ns_school_major` ADD COLUMN `year` SMALLINT NOT NULL DEFAULT 0 COMMENT ''目录年份'' AFTER `degree_type`',
  'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @old_key_exists = (
  SELECT COUNT(*) FROM `information_schema`.`STATISTICS`
   WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = 'ns_school_major' AND `INDEX_NAME` = 'uk_school_major'
);
SET @ddl = IF(@old_key_exists > 0,
  'ALTER TABLE `ns_school_major` DROP INDEX `uk_school_major`',
  'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @new_key_exists = (
  SELECT COUNT(*) FROM `information_schema`.`STATISTICS`
   WHERE `TABLE_SCHEMA` = DATABASE() AND `TABLE_NAME` = 'ns_school_major' AND `INDEX_NAME` = 'uk_school_major_year'
);
SET @ddl = IF(@new_key_exists = 0,
  'ALTER TABLE `ns_school_major` ADD UNIQUE KEY `uk_school_major_year` (`school_id`, `major_code`, `year`)',
  'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
