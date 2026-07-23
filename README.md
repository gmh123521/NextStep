# NextStep —— 未来指导决策系统

考研 / 考公 / 就业三路径决策辅助。Spring Boot 3.2 + JDK 17 单体多模块。

## 模块

| 模块 | 职责 | M1 状态 |
|---|---|---|
| nextstep-common | 响应、异常、枚举、常量 | done |
| nextstep-framework | Web/Redis/MyBatis-Plus/Security/Knife4j 公共配置 | done |
| nextstep-auth | 注册、登录、JWT | done |
| nextstep-user | 用户中心、个人画像 | M2 |
| nextstep-data-school | 院校与考研数据 | M2 |
| nextstep-data-gov | 考公数据 | M2 |
| nextstep-data-job | 就业数据 | M2 |
| nextstep-crawler | 数据采集 | M2 |
| nextstep-analysis | 路径打分、推荐 | M3 |
| nextstep-planner | 备考/求职规划 | M3 |
| nextstep-ai | LLM 接入 | M4 |
| nextstep-report | PDF 报告 | M4 |
| nextstep-admin | 后台管理 | M4 |
| nextstep-api | 启动聚合入口 | done |

## 里程碑

- **M1 骨架（当前）**：多模块 + 登录注册 + JWT + Knife4j
- **M2 数据**：三条路径的数据表 + 基础查询 + 一两个爬虫
- **M3 分析**：个人画像 + 规则引擎打分 + 三路径对比
- **M4 增强**：LLM 咨询 + PDF 报告 + 后台 + ES 搜索

## 本地启动

```bash
# 1. 起 MySQL + Redis
docker compose up -d

# 2. 编译
mvn clean package -DskipTests

# 3. 启动
java -jar nextstep-api/target/nextstep-api.jar

# 4. 接口文档
# http://localhost:8080/api/doc.html
```

## 默认配置

可通过环境变量覆盖：`MYSQL_HOST`、`MYSQL_PORT`、`MYSQL_USER`、`MYSQL_PWD`、`MYSQL_DB`、`REDIS_HOST`、`REDIS_PORT`、`REDIS_PWD`。

JWT 密钥请务必修改 `nextstep.jwt.secret`。
