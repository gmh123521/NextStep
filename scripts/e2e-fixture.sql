-- ============================================================
-- E2E 测试数据夹具（仅开发/测试环境使用，不在 sql/ 目录 —— 不会被
-- docker-entrypoint-initdb.d 自动执行）
--
-- 用法：
--   docker exec -i nextstep-mysql mysql -uroot -proot < scripts/e2e-fixture.sql
--
-- 前置：先通过 /api/auth/register 注册 e2euser / e2eadmin（密码 e2e12345），
--       本脚本只负责补画像、经历和管理员角色。
-- 幂等：可反复执行，按 username 定位 user_id，先删后插。
-- ============================================================
USE `nextstep`;

-- ---------- 1. e2eadmin 提升为管理员 ----------
UPDATE `ns_user` SET `role` = 'ADMIN' WHERE `username` = 'e2eadmin';

-- ---------- 2. e2euser 完整画像（用于路径打分 / 规划 / AI 咨询）----------
DELETE FROM `ns_user_profile`
 WHERE `user_id` = (SELECT `id` FROM `ns_user` WHERE `username` = 'e2euser');

INSERT INTO `ns_user_profile`
  (`user_id`, `current_school`, `school_level`, `current_major`, `major_category`,
   `degree_type`, `grade_year`, `gpa`, `gpa_scale`, `class_rank_pct`,
   `english_level`, `english_score`, `target_paths`, `preferred_regions`,
   `preferred_industries`, `salary_expectation`, `risk_appetite`, `monthly_budget`,
   `interests`, `strengths`, `weaknesses`, `current_status`)
SELECT `id`, '武汉大学', '985', '计算机科学与技术', '工学',
       'BACHELOR', 3, 3.62, 4, 18.50,
       'CET6', 528, 'PG,EM', '武汉,杭州,深圳',
       '互联网,人工智能', 18000, 4, 1500,
       '后端开发,分布式系统,算法竞赛', '工程实践扎实，有大厂实习和竞赛奖项', '数学基础一般，科研论文空缺', 'IN_SCHOOL'
  FROM `ns_user` WHERE `username` = 'e2euser';

-- ---------- 3. e2euser 三条经历（summary 已填，AI 咨询可直接用作上下文）----------
DELETE FROM `ns_user_experience`
 WHERE `user_id` = (SELECT `id` FROM `ns_user` WHERE `username` = 'e2euser');

INSERT INTO `ns_user_experience`
  (`user_id`, `type`, `title`, `role`, `start_date`, `end_date`, `description`, `summary`, `source`)
SELECT u.`id`, t.`type`, t.`title`, t.`role`, t.`start_date`, t.`end_date`, t.`description`, t.`summary`, 'MANUAL'
  FROM `ns_user` u
  JOIN (
    SELECT 'INTERNSHIP' AS `type`, '字节跳动' AS `title`, '后端开发实习生' AS `role`,
           '2025-07' AS `start_date`, '2025-09' AS `end_date`,
           '参与推荐中台服务开发，负责特征回流链路的性能优化，QPS 从 1.2k 提升到 4k。' AS `description`,
           '在字节跳动担任后端开发实习生 2 个月，负责推荐中台特征回流链路性能优化，QPS 提升约 3 倍，具备高并发服务调优经验。' AS `summary`
    UNION ALL
    SELECT 'PROJECT', '校园二手交易平台', '全栈负责人',
           '2025-03', '2025-06',
           'Spring Boot + Vue3 全栈项目，实现商品发布、IM 聊天与订单流程，校内累计 2000+ 注册用户。',
           '独立主导校园二手交易平台全栈开发，技术栈 Spring Boot + Vue3，落地商品、IM 与订单闭环，校内 2000+ 用户，具备完整项目交付能力。'
    UNION ALL
    SELECT 'COMPETITION', '蓝桥杯省级一等奖', '参赛选手',
           '2025-04', '2025-04',
           '第十六届蓝桥杯软件赛 Java 组省级一等奖。',
           '获蓝桥杯 Java 组省级一等奖，算法与编码能力较强，可作为考研复试和就业简历的加分项。'
  ) t
 WHERE u.`username` = 'e2euser';

-- ---------- 4. 校验 ----------
SELECT u.id, u.username, u.role, p.current_school, p.gpa, p.english_level, p.target_paths,
       (SELECT COUNT(*) FROM ns_user_experience e WHERE e.user_id = u.id AND e.deleted = 0) AS exp_count
  FROM ns_user u
  LEFT JOIN ns_user_profile p ON p.user_id = u.id AND p.deleted = 0
 WHERE u.username IN ('e2euser', 'e2eadmin');
