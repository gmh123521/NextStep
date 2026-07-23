-- M2 mock 样本数据（少量真实数据用于联调）
USE `nextstep`;

-- 院校
INSERT IGNORE INTO `ns_school` (name, code, province, city, level, type, is_self_marking) VALUES
  ('清华大学',     '10003', '北京', '北京', 'C9',  '综合', 1),
  ('北京大学',     '10001', '北京', '北京', 'C9',  '综合', 1),
  ('上海交通大学', '10248', '上海', '上海', 'C9',  '综合', 1),
  ('复旦大学',     '10246', '上海', '上海', 'C9',  '综合', 1),
  ('浙江大学',     '10335', '浙江', '杭州', 'C9',  '综合', 1),
  ('武汉大学',     '10486', '湖北', '武汉', '985', '综合', 0),
  ('北京航空航天大学','10006','北京', '北京','985','理工', 1),
  ('南京大学',     '10284', '江苏', '南京', 'C9',  '综合', 1),
  ('东南大学',     '10286', '江苏', '南京', '985', '理工', 0);

-- 院校专业（计算机相关）
INSERT IGNORE INTO `ns_school_major` (school_id, major_code, major_name, category, degree_type) VALUES
  (1, '081200', '计算机科学与技术',     '工学', 'ACADEMIC'),
  (1, '085400', '电子信息（计算机方向）','工学', 'PROFESSIONAL'),
  (2, '081200', '计算机科学与技术',     '工学', 'ACADEMIC'),
  (3, '081200', '计算机科学与技术',     '工学', 'ACADEMIC'),
  (5, '081200', '计算机科学与技术',     '工学', 'ACADEMIC'),
  (5, '085400', '电子信息（计算机方向）','工学', 'PROFESSIONAL'),
  (6, '081200', '计算机科学与技术',     '工学', 'ACADEMIC'),
  (8, '081200', '计算机科学与技术',     '工学', 'ACADEMIC');

-- 招录（2024 / 2025）
INSERT IGNORE INTO `ns_school_enroll` (school_id, major_id, year, enroll_plan, enroll_actual, apply_count, cutoff_score, cutoff_english, cutoff_political, lowest_score, highest_score) VALUES
  (1, 1, 2024, 30,  32, 980,  390, 60, 60, 391, 425),
  (1, 1, 2025, 28,  30, 1100, 395, 60, 60, 396, 430),
  (1, 2, 2024, 80,  85, 1500, 360, 55, 55, 361, 410),
  (3, 4, 2024, 50,  53, 720,  370, 55, 55, 372, 415),
  (5, 5, 2024, 60,  62, 880,  365, 55, 55, 366, 412),
  (5, 5, 2025, 60,  64, 950,  370, 55, 55, 372, 418),
  (5, 6, 2024, 100, 105, 1300, 350, 50, 50, 352, 400),
  (6, 7, 2024, 70,  75, 600,  340, 50, 50, 342, 395),
  (8, 8, 2024, 55,  58, 760,  365, 55, 55, 367, 410);

-- 考公岗位（2025 国考 + 部分省考）
INSERT IGNORE INTO `ns_gov_post` (year, exam_type, province, dept_name, post_code, post_name, region, degree_required, major_required, political, extra_required) VALUES
  (2025, 'NATIONAL', NULL, '国家税务总局北京市税务局', 'NTAX-001', '科员（税务执法）',     '北京', 'BACHELOR', '财政学/税收学/会计学', '不限',     '应届生'),
  (2025, 'NATIONAL', NULL, '海关总署上海海关',         'NCUS-002', '一级行政执法员',       '上海', 'BACHELOR', '不限',                 '不限',     '两年基层工作经验'),
  (2025, 'NATIONAL', NULL, '中国证券监督管理委员会',   'NCSR-003', '科员（信息技术）',     '北京', 'MASTER',   '计算机/软件工程',      '党员',     '不限'),
  (2025, 'PROVINCIAL', '浙江', '杭州市公安局',         'PHZ-001', '执法勤务（计算机）',   '杭州', 'BACHELOR', '计算机类',             '不限',     '男性'),
  (2025, 'PROVINCIAL', '广东', '广州市市场监管局',     'PGZ-002', '综合管理',             '广州', 'BACHELOR', '不限',                 '党员',     '不限');

-- 招录数据
INSERT IGNORE INTO `ns_gov_enroll` (post_id, enroll_count, apply_count, apply_pass, attend_count) VALUES
  (1, 3, 350,  340,  310),
  (2, 5, 1200, 1180, 1050),
  (3, 1, 280,  270,  260),
  (4, 4, 220,  215,  200),
  (5, 2, 180,  175,  160);

-- 进面线
INSERT IGNORE INTO `ns_gov_cutoff` (post_id, interview_min, interview_max, final_min) VALUES
  (1, 132.50, 145.20, 71.50),
  (2, 138.20, 152.00, 73.80),
  (3, 145.30, 158.50, 76.20),
  (4, 128.40, 140.10, 70.30),
  (5, 122.60, 138.50, 68.50);

-- 行业
INSERT IGNORE INTO `ns_industry` (code, name, parent_id) VALUES
  ('IT',     '互联网/IT',  0),
  ('FIN',    '金融',       0),
  ('EDU',    '教育',       0),
  ('MFG',    '制造业',     0),
  ('CONS',   '咨询',       0);

-- 岗位
INSERT IGNORE INTO `ns_job_position` (name, industry_id, category, description) VALUES
  ('Java 后端开发',  1, '技术', '使用 Java/Spring 体系开发企业级应用'),
  ('前端开发',       1, '技术', 'Vue/React 等前端框架开发'),
  ('算法工程师',     1, '技术', '机器学习/深度学习算法开发'),
  ('数据分析师',     1, '数据', '业务数据分析与建模'),
  ('产品经理',       1, '产品', '需求分析与产品规划'),
  ('风控分析师',     2, '风险', '金融风险评估建模');

-- 薪资统计（应届）
INSERT IGNORE INTO `ns_salary_stat` (position_id, city, experience, degree, min_salary, max_salary, median_salary, sample_size, data_source, stat_year) VALUES
  (1, '北京', 'FRESH', 'BACHELOR', 12000, 25000, 18000, 1500, 'mock', 2025),
  (1, '北京', 'FRESH', 'MASTER',   15000, 32000, 22000, 800,  'mock', 2025),
  (1, '上海', 'FRESH', 'BACHELOR', 12000, 24000, 17500, 1300, 'mock', 2025),
  (1, '杭州', 'FRESH', 'BACHELOR', 11000, 22000, 16500, 1100, 'mock', 2025),
  (3, '北京', 'FRESH', 'MASTER',   25000, 50000, 35000, 600,  'mock', 2025),
  (4, '上海', 'FRESH', 'BACHELOR', 9000,  18000, 13500, 900,  'mock', 2025),
  (5, '北京', 'FRESH', 'BACHELOR', 10000, 22000, 15500, 700,  'mock', 2025),
  (6, '上海', 'FRESH', 'MASTER',   15000, 28000, 20000, 400,  'mock', 2025);
