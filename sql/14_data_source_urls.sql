-- 为已有数据库补齐官方数据源地址；保留管理员已配置的非空地址
USE `nextstep`;

UPDATE `ns_data_source`
   SET `source_url` = 'https://yz.chsi.com.cn/zsml/pages/getMl.jsp'
 WHERE `source_code` = 'KAOYAN_SCHOOL'
   AND (`source_url` IS NULL OR `source_url` = '');

UPDATE `ns_data_source`
   SET `source_url` = 'https://yz.chsi.com.cn/zsml/queryAction.do'
 WHERE `source_code` = 'KAOYAN_CATALOG'
   AND (`source_url` IS NULL OR `source_url` = '');

INSERT INTO `ns_data_source`
  (`source_code`, `source_name`, `organization`, `source_type`, `source_url`, `update_frequency`, `license_note`)
VALUES
  ('GOV_POST', '国考职位表', '国家公务员局', 'JSON', 'http://www.scs.gov.cn/kl2023/kl/zwb/index.json', '按年度发布', '仅采集公开且允许使用的数据')
ON DUPLICATE KEY UPDATE
  `source_url` = COALESCE(NULLIF(`source_url`, ''), VALUES(`source_url`));
