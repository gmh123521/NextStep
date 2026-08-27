# 未完成功能补全实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 完成后台管理、数据采集、综合报告模块的交付收口，并修复前端构建错误。

**Architecture:** 保留现有多模块 Spring Boot 结构和 Vue 组件边界；服务层负责业务校验与异常兜底，控制器只负责协议转换，采集源通过 `SourceCrawler` 插件注册，报告通过聚合服务和 OpenPDF 渲染。

**Tech Stack:** Java 17, Spring Boot 3.2, MyBatis-Plus, Spring Security/JWT, OpenPDF, Vue 3, TypeScript, Vite.

---

### Task 1: 修复前端 TypeScript 构建错误

**Files:**
- Modify: `nextstep-web/src/api/http.ts`
- Modify: `nextstep-web/src/api/planner.ts`
- Modify: `nextstep-web/src/components/ChatPanel.vue`
- Modify: `nextstep-web/src/components/ResumeUploader.vue`
- Modify: `nextstep-web/src/components/TranscriptUploader.vue`
- Modify: `nextstep-web/src/views/Plan.vue`
- Modify: `nextstep-web/src/views/Profile.vue`
- Create: `nextstep-web/src/vite-env.d.ts`

- [x] **Step 1: Run the failing build**

Run `npm run build` in `nextstep-web` and confirm the six reported type errors.

- [x] **Step 2: Apply type-only fixes**

Keep runtime behavior unchanged: use Axios interceptor-compatible return typing, declare `ImportMetaEnv`, accept Element Plus event unions, guard missing upload raw files, normalize select values to strings, and define a shared language-level metadata type with optional hint.

- [x] **Step 3: Run the build again**

Run `npm run build`; expected output is successful `vue-tsc` and Vite bundle generation.

### Task 2: Harden admin service validation

**Files:**
- Modify: `nextstep-admin/src/main/java/com/nextstep/admin/service/AdminUserService.java`
- Modify: `nextstep-admin/src/main/java/com/nextstep/admin/service/AdminDataService.java`

- [x] **Step 1: Add validation guards**

Reject non-positive page values, invalid status values, blank/invalid roles, null write bodies, and self-disable/self-demotion based on `SecurityUtils.currentUserId()`.

- [x] **Step 2: Preserve existing CRUD behavior**

Use `BizException` with Chinese messages and keep existing mapper operations unchanged after validation.

### Task 3: Harden crawler execution and history

**Files:**
- Modify: `nextstep-crawler/src/main/java/com/nextstep/crawler/service/CrawlerService.java`
- Modify: `nextstep-crawler/src/main/java/com/nextstep/crawler/controller/CrawlerController.java`
- Modify: `nextstep-crawler/src/main/java/com/nextstep/crawler/entity/CrawlerJob.java`

- [x] **Step 1: Validate source and pagination inputs**

Normalize source names and reject unknown sources with `BizException`; clamp or reject invalid page parameters.

- [x] **Step 2: Bound task error text and result counters**

Persist status, started/finished times, and counters consistently; truncate exception messages to the database column length.

- [x] **Step 3: Verify scheduler isolation**

Ensure one source failure does not stop scheduled execution of other sources and manual requests return the persisted job.

### Task 4: Make report rendering defensive

**Files:**
- Modify: `nextstep-report/src/main/java/com/nextstep/report/service/ReportService.java`
- Modify: `nextstep-report/src/main/java/com/nextstep/report/service/ReportPdfService.java`

- [x] **Step 1: Guard missing profile/analysis and nullable PDF fields**

Return clear business errors for missing profile or analysis and render placeholders for null username, timestamps, dimensions, advice, and plan tasks.

- [x] **Step 2: Preserve no-plan export**

Add a visible “尚未生成规划” note when the recommended path has no plan.

### Task 5: Update documentation and verify repository state

**Files:**
- Modify: `README.md`

- [x] **Step 1: Update module status and migration list**

Mark crawler/report/admin as implemented and document their endpoints and default crawler-off behavior.

- [x] **Step 2: Run verification**

Run `npm run build`, `mvn -DskipTests compile` (or capture the dependency-network blocker), inspect `git diff --check`, and review the exact staging list.

- [ ] **Step 3: Commit in Chinese** — 因当前环境无法创建 `.git/index.lock` 且审批服务返回 503，待恢复 Git 写权限后执行。

Commit only task-related files with a Chinese message such as `完善后台管理、数据采集与综合报告功能`.
