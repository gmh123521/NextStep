# NextStep — 大学生三路径决策辅助平台

> 考研、考公，还是就业？
>
> 把你的个人画像和三条路的真实门槛放在一起量化对比，用规则引擎打分、大模型咨询、AI 解析简历，帮你想清楚下一步。
> 一个面向大学生的完整求职决策工程实践。

<p align="center">
  <img src="https://img.shields.io/badge/Java-17+-orange?logo=openjdk" alt="Java 17+">
  <img src="https://img.shields.io/badge/SpringBoot-3.2-brightgreen?logo=springboot" alt="Spring Boot 3.2">
  <img src="https://img.shields.io/badge/Vue-3-4FC08D?logo=vuedotjs" alt="Vue 3">
  <img src="https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql" alt="MySQL 8.0">
  <img src="https://img.shields.io/badge/Redis-7-DC382D?logo=redis" alt="Redis 7">
  <img src="https://img.shields.io/badge/DashScope-通义千问-blue" alt="DashScope">
  <img src="https://img.shields.io/badge/Docker-ready-2496ED?logo=docker" alt="Docker">
  <img src="https://img.shields.io/badge/license-MIT-green" alt="License MIT">
</p>

---

## ✨ Why NextStep

每年临近毕业，大量学生在"考研 / 考公 / 就业"三条路之间反复摇摆——缺的不是信息，而是**把自己的条件和三条路的真实门槛放在一起做量化对比**的工具。

NextStep 做的就是这件事：

- **简单问题（画像录入）**：院校层次、专业、GPA、英语等级、实习 / 项目 / 竞赛经历一站式录入
- **核心问题（路径打分）**：考研 / 考公 / 就业各自独立的规则引擎策略，输出匹配度评分与雷达图对比
- **深度问题（AI 咨询）**：大模型基于你的真实画像给出个性化建议，解析简历，生成备考计划

## 🎯 核心特性

| 能力 | 实现 |
|------|------|
| 👤 **个人画像** | 院校层次 / 专业 / GPA / 英语等级 / 实习·项目·竞赛经历，结构化存储 |
| 📊 **三路径打分** | 考研 / 考公 / 就业独立规则引擎策略，雷达图直观对比优劣势 |
| 🤖 **AI 流式咨询** | 通义千问 DashScope，SSE 流式对话 + 工具调用，AI 实时读取画像作答 |
| 📄 **简历 PDF 解析** | 上传简历，LLM 抽取结构化字段，并发生成每段经历的 AI 摘要 |
| 🎓 **成绩单解析** | 上传成绩单图片 / PDF，视觉模型自动识别课程与成绩 |
| 🗺️ **备考规划** | 按目标路径生成阶段性任务清单，支持导出 PDF 报告 |
| 🏫 **数据中心** | 院校考研分数线、考公岗位竞争比、行业薪资等真实数据查询 |
| ⚡ **并发摘要生成** | 简历入库时多条经历并行调 LLM 生成摘要，串行 ~60s → 并发 ~10s |
| 🛠️ **后台管理** | 管理员用户、院校、岗位、薪资、采集任务与系统概览 |
| 📥 **数据采集** | 研招网与国考岗位源手动/定时采集，外网定时任务默认关闭 |
| 📄 **综合报告** | 聚合个人画像、路径分析与规划并导出中文 PDF |

---

## 架构总览

### 对话与 AI 流程

```
┌─────────────────────────────────────────────────────────────────┐
│                      用户交互层 (SSE / REST)                      │
│   ChatController · ResumeController · TranscriptController      │
└──────────────────────────┬──────────────────────────────────────┘
                           │
          ┌────────────────▼────────────────┐
          │         ChatService              │
          │  · 读取用户画像 + 经历摘要         │
          │  · 构建系统 Prompt               │
          │  · 工具调用（查院校/岗位/薪资）    │
          │  · SSE 流式推送 Token            │
          └────────────────┬────────────────┘
                           │
          ┌────────────────▼────────────────┐
          │        DashScopeClient           │
          │  OpenAI 兼容模式 · 通义千问       │
          │  text-model: qwen3.7-plus        │
          │  vision-model: qwen-vl-plus      │
          └─────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                      简历解析流程                                  │
└──────────────────────────┬──────────────────────────────────────┘
                           │
   ┌───────────────────────▼──────────────────────────────────┐
   │  ResumeExtractService.parse()                             │
   │  PDF → PDFBox 提取文本 → LLM 抽取结构化 JSON              │
   │  返回前端预览，用户确认后调 apply()                         │
   └───────────────────────┬──────────────────────────────────┘
                           │ apply()
   ┌───────────────────────▼──────────────────────────────────┐
   │  阶段 1：过滤重复经历（指纹匹配 + 包含匹配）                 │
   │  阶段 2：并发调 LLM 生成摘要（parallelStream）             │
   │  阶段 3：串行入库（保持事务 + 顺序）                        │
   └──────────────────────────────────────────────────────────┘
```

### 路径分析流程

```
┌─────────────────────────────────────────────────────────────────┐
│                      AnalysisController                          │
└──────────────────────────┬──────────────────────────────────────┘
                           │
          ┌────────────────▼────────────────┐
          │         AnalysisService          │
          │  读取 UserProfile + UserExperience│
          └──┬──────────────┬───────────────┘
             │              │              │
   ┌─────────▼──┐  ┌────────▼───┐  ┌──────▼──────┐
   │ 考研策略    │  │ 考公策略    │  │ 就业策略     │
   │ Postgrad   │  │ CivilServ  │  │ Employment  │
   │ Strategy   │  │ Strategy   │  │ Strategy    │
   │            │  │            │  │             │
   │ · 院校层次  │  │ · 英语等级  │  │ · 实习经历   │
   │ · GPA      │  │ · 政治倾向  │  │ · 项目经历   │
   │ · 英语等级  │  │ · 岗位竞争  │  │ · 竞赛奖项   │
   │ · 科研经历  │  │ · 笔试分数  │  │ · 薪资匹配   │
   └─────┬──────┘  └─────┬──────┘  └──────┬──────┘
         │               │                │
         └───────────────▼────────────────┘
                  AnalysisResult
                  · 三路径 PathScore
                  · 雷达图数据
                  · 推荐路径 + 理由
```

### 数据与基础设施层

```
┌─────────────────────────────────────────────────────────────────┐
│                         数据存储层                                │
├──────────────────┬──────────────────┬───────────────────────────┤
│                  │                  │                           │
│     MySQL 8.0    │    Redis 7        │      本地文件系统           │
│                  │                  │                           │
│ · ns_user        │ · JWT Token 缓存  │ · PDF 简历（临时）          │
│ · ns_user_profile│ · 会话状态        │ · 成绩单图片（临时）         │
│ · ns_user_exp    │ · 接口限流        │                           │
│ · ns_user_plan   │                  │                           │
│ · ns_school      │                  │                           │
│ · ns_gov_post    │                  │                           │
│ · ns_job_position│                  │                           │
│ · ns_salary_stat │                  │                           │
│ · Flyway 迁移    │                  │                           │
└──────────────────┴──────────────────┴───────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                         基础设施层                                │
├──────────────┬───────────────┬──────────────┬───────────────────┤
│              │               │              │                   │
│  JWT 安全    │  MyBatis-Plus  │  Druid 连接池 │   Knife4j 文档    │
│              │               │              │                   │
│ · JwtFilter  │ · 逻辑删除     │ · 连接监控    │ · OpenAPI 3       │
│ · SecurityCfg│ · 自动填充     │ · 慢查询检测  │ · Swagger UI      │
│ · 无状态     │ · 分页插件     │              │                   │
└──────────────┴───────────────┴──────────────┴───────────────────┘
```

---

## 核心功能详解

### 1. 个人画像与经历管理

- **画像字段**：院校层次（C9/985/211/双一流/普通本科/专科）、专业、GPA（4/5/100 分制）、英语等级（CET4/6/雅思/托福等）、年级、当前状态
- **经历类型**：实习（INTERNSHIP）/ 项目（PROJECT）/ 奖项（AWARD）/ 科研（RESEARCH）/ 论文（PAPER）/ 学科竞赛（COMPETITION）
- **AI 摘要**：每条经历自动调 LLM 生成 100 字以内的结构化摘要，供 AI 咨询时作为上下文注入

### 2. 简历解析（PDF → 结构化数据）

- **文本提取**：PDFBox 提取文字版 PDF，限 5MB / 8000 字符
- **LLM 抽取**：系统 Prompt 约束输出 JSON，包含学校、专业、GPA、英语等级、经历列表
- **隐私保护**：Prompt 明确禁止抽取手机 / 邮箱 / 地址 / 身份证
- **去重入库**：指纹匹配（同类型同标题）+ 包含匹配（"蓝桥杯" vs "蓝桥杯省级一等奖"）防重复
- **并发摘要**：多条经历并行调 LLM，`parallelStream` + `ConcurrentHashMap` 收集结果后串行入库

### 3. AI 咨询（流式对话 + 工具调用）

- **上下文注入**：对话前自动读取用户画像 + 经历摘要，AI 基于真实数据作答
- **工具调用**：AI 可主动查询院校数据、考公岗位、薪资统计
- **流式推送**：SSE 逐 Token 推送，前端实时渲染
- **重试机制**：LLM 调用失败自动重试一次

### 4. 路径打分引擎

三条路径各自独立的 `PathScoreStrategy` 实现，基于 `ScoringConstants` 中的权重常量计算：

| 维度 | 考研 | 考公 | 就业 |
|------|------|------|------|
| 院校层次 | ✅ 高权重 | ✅ 中权重 | ✅ 低权重 |
| GPA | ✅ 高权重 | ✅ 低权重 | ✅ 中权重 |
| 英语等级 | ✅ 中权重 | ✅ 低权重 | ✅ 中权重 |
| 实习经历 | ❌ | ❌ | ✅ 高权重 |
| 竞赛 / 科研 | ✅ 中权重 | ❌ | ✅ 低权重 |

---

## 技术栈

| 分类 | 技术 | 说明 |
|------|------|------|
| **语言 / 框架** | Java 17+ · Spring Boot 3.2 | 多模块 Maven 单体 |
| **安全** | Spring Security · JWT (JJWT) | 无状态 Token 认证 |
| **持久层** | MyBatis-Plus · Druid · Flyway | ORM + 连接池 + 版本迁移 |
| **存储** | MySQL 8.0 · Redis 7 | 业务数据 + 缓存 |
| **AI** | 通义千问 DashScope | OpenAI 兼容模式，文本 + 视觉模型 |
| **PDF 解析** | Apache PDFBox | 文字版 PDF 文本提取 |
| **前端** | Vue 3 · Vite · Pinia · UnoCSS · ECharts | SPA + 雷达图可视化 |
| **文档** | Knife4j / SpringDoc (OpenAPI 3) | 在线接口调试 |
| **部署** | Docker · Docker Compose · Nginx | 一键拉起全栈环境 |

---

## 项目结构

```
nextstep/
├── nextstep-common/          # 响应体 R<T>、BizException、枚举、常量
├── nextstep-framework/       # Security/JWT、MyBatis-Plus、Redis、Knife4j 公共配置
├── nextstep-auth/            # 注册、登录、JWT 签发
├── nextstep-user/            # 用户中心、个人画像 CRUD
├── nextstep-data-school/     # 院校与考研数据查询接口
├── nextstep-data-gov/        # 考公岗位数据查询接口
├── nextstep-data-job/        # 就业薪资数据查询接口
├── nextstep-analysis/        # 三路径规则引擎打分与推荐
│   └── strategy/
│       ├── PostgraduateStrategy.java
│       ├── CivilServantStrategy.java
│       └── EmploymentStrategy.java
├── nextstep-planner/         # 备考 / 求职规划生成与 PDF 导出
├── nextstep-ai/              # LLM 接入层
│   ├── client/               #   DashScopeClient（OpenAI 兼容）
│   ├── service/
│   │   ├── ChatService.java              # 流式对话 + 工具调用
│   │   ├── ResumeExtractService.java     # 简历解析（三阶段并发）
│   │   ├── TranscriptExtractService.java # 成绩单解析（视觉模型）
│   │   └── ExperienceSummaryService.java # 经历摘要生成 + 批量回填
│   └── controller/
│       ├── ChatController.java
│       ├── ResumeController.java
│       ├── TranscriptController.java
│       └── UserExperienceController.java
├── nextstep-crawler/         # 数据采集（后台手动触发 + 定时任务）
├── nextstep-report/          # 综合决策报告 PDF 导出
├── nextstep-admin/           # 后台用户、数据与采集管理
├── nextstep-api/             # 启动聚合入口（NextStepApplication）
│   └── resources/application.yml
├── nextstep-web/             # Vue 3 前端
│   ├── src/views/            #   Dashboard / Profile / Plan / School / Gov / Job
│   ├── src/components/       #   ChatPanel / ResumeUploader / RadarChart
│   └── src/api/              #   各模块 HTTP 封装
├── sql/                      # 数据库初始化与增量脚本（01~11）
├── docker-compose.yml        # MySQL + Redis + 后端一键部署
├── Dockerfile                # 后端镜像（eclipse-temurin:23-jre-alpine）
└── nginx/nextstep.conf       # 前端反向代理配置
```

---

## 设计原则

- **隐私优先**：简历解析 Prompt 明确禁止抽取手机 / 邮箱 / 身份证，敏感字段不落库
- **用户确认后入库**：简历解析结果先返回前端预览，用户确认后才调 `apply()` 写库
- **并发优先**：多条经历摘要并行调 LLM，串行 ~60s → 并发 ~10s
- **去重兜底**：指纹匹配 + 包含匹配双重去重，防止重复导入经历
- **容错重试**：LLM 调用失败自动重试一次，JSON 解析失败给出明确错误提示
- **派生字段**：`has_internship` 等标志位从 experience 表实时聚合，不手动维护，避免脏数据

---

## 🚀 快速启动

### 1. 环境要求

- **JDK 17+**（必须）
- **Maven 3.8+**
- **Docker + Docker Compose**
- **Node.js 18+**（前端开发）
- **DashScope API Key**：[阿里云百炼平台](https://bailian.console.aliyun.com/)（AI 功能必填）

### 2. 配置环境变量

```bash
# 复制模板
cp .env.example .env

# 编辑 .env，填入你的 API Key
DASHSCOPE_API_KEY=sk-your-key-here
```

### 3. 启动中间件

```bash
docker compose up -d mysql redis
```

### 4. 编译打包

```bash
mvn clean package -DskipTests
```

### 5. 启动后端

```bash
java -jar nextstep-api/target/nextstep-api.jar
```

接口文档：`http://localhost:8080/api/doc.html`

### 6. 启动前端（开发模式）

```bash
cd nextstep-web
npm install
npm run dev
# 访问：http://localhost:5173
```

### 全栈 Docker 一键部署

```bash
# 先打包后端
mvn clean package -DskipTests

# 一键拉起 MySQL + Redis + 后端
docker compose up -d
```

---

## ⚙️ 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `MYSQL_HOST` | `127.0.0.1` | MySQL 地址 |
| `MYSQL_PORT` | `3306` | MySQL 端口 |
| `MYSQL_DB` | `nextstep` | 数据库名 |
| `MYSQL_USER` | `root` | 数据库用户 |
| `MYSQL_PWD` | `root` | 数据库密码（生产环境务必修改） |
| `REDIS_HOST` | `127.0.0.1` | Redis 地址 |
| `REDIS_PORT` | `6379` | Redis 端口 |
| `DASHSCOPE_API_KEY` | — | 通义千问 API Key（必填，AI 功能依赖） |

> ⚠️ 生产环境请修改 `nextstep.jwt.secret`（application.yml）为随机长字符串，并替换数据库默认密码。

---

## 📄 License

MIT
