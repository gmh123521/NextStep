-- 采集中心：爬虫任务运行记录
USE `nextstep`;

DROP TABLE IF EXISTS `ns_crawler_job`;
CREATE TABLE `ns_crawler_job` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `source`      VARCHAR(32)  NOT NULL              COMMENT '数据源：KAOYAN 研招网 / GOV_POST 考公岗位',
  `trigger_by`  VARCHAR(16)  NOT NULL DEFAULT 'SCHEDULE' COMMENT 'SCHEDULE 定时 / MANUAL 手动',
  `status`      VARCHAR(16)  NOT NULL DEFAULT 'RUNNING'  COMMENT 'RUNNING / SUCCESS / FAILED',
  `fetched`     INT          NOT NULL DEFAULT 0    COMMENT '抓取条数',
  `inserted`    INT          NOT NULL DEFAULT 0    COMMENT '新入库条数（去重后）',
  `skipped`     INT          NOT NULL DEFAULT 0    COMMENT '重复跳过条数',
  `message`     VARCHAR(512)          DEFAULT NULL COMMENT '结果 / 错误摘要',
  `started_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `finished_at` DATETIME              DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_source_time` (`source`, `started_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='爬虫任务运行记录';
