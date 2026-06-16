# CampusClub：校园社团活动票务助手

基于 **MCP 与 RAG** 的校园社团活动智能助手。用户可通过自然语言查询活动、报名/取消、了解政策；社团助手（人工）在复杂场景兜底；管理员维护活动、知识库与提示词。


## 项目概述

CampusClub 面向校内社团活动与票务场景：讲座、比赛、招新、志愿等。核心业务为 **活动（Activity）**、**票档（TicketTier）** 与 **报名（Registration）**；静态政策通过 RAG 知识库检索，实时数据通过 AI 工具访问 MySQL。

## 快速开始（本地运行）

### 1. 环境要求

| 组件 | 说明 |
|------|------|
| **JDK** | 21（与 `backend/pom.xml` 中 `java.version` 一致） |
| **Maven** | 3.8+ |
| **Node.js** | 建议 18+（前端 Vite 7） |
| **MySQL** | 8.x |
| **MongoDB** | 会话与知识库文档 |
| **Redis** | 验证码与向量检索 |
| **百炼 / DashScope** | Spring AI 模型 API Key |

### 2. 初始化数据库

1. 在 MySQL 中创建库 **`campusclub`**
2. 启动后端时 Flyway 会自动执行 `backend/src/main/resources/db/migration/` 建表
3. 本地 `local` profile 下会自动灌入演示数据（可通过 `app.seed.dev-data=false` 关闭）

手动建库参考：`backend/src/main/resources/schema.sql`（遗留脚本，推荐 Flyway）

### 3. 后端配置

```bash
cp backend/src/main/resources/application-local.yml.example backend/src/main/resources/application-local.yml
# 编辑 application-local.yml，填入数据库、Redis、DashScope、邮件等密钥
```

启动：`cd backend && mvn spring-boot:run`

### 4. 前端

```bash
cd frontend
cp .env.development.example .env.development
npm install
npm run dev
```

## 核心能力

### 用户端
- 与 AI 社团助手对话：查活动、报名、取消、政策问答
- 转人工社团助手
- 个人中心查看报名与历史会话

### 社团助手端
- 会话列表与回复
- 查看用户报名并协助取消

### 管理端
- 活动/票档 CRUD、报名管理
- 知识库、提示词、账号、日志
- 仪表盘：今日活动/报名趋势

## AI 工具（11 个）

| 分类 | 工具 | 说明 |
|------|------|------|
| 活动 | `listUpcomingActivities` | 即将开放报名/开始的活动 |
| 活动 | `searchActivities` | 按类型、地点、日期搜索 |
| 活动 | `getActivityByTicketCode` | 按票种编码查活动 |
| 活动 | `getActivitiesByClub` | 按主办社团筛选 |
| 报名 | `getRegistrationDetail` | 报名详情 |
| 报名 | `getRegistrationsByName` | 按姓名查报名 |
| 报名 | `createRegistration` | 创建报名 |
| 报名 | `cancelRegistration` | 取消报名 |
| 统计 | `getDashboardOverview` | 仪表盘概览与趋势 |
| 统计 | `getActivityStats` | 活动报名统计 |
| 用户 | `getUserProfile` | 个人信息与权益 |

## Demo 对话示例

1. 「最近有什么活动可以报名？」→ `listUpcomingActivities`
2. 「5月28日图书馆有什么活动？」→ `searchActivities`
3. 「帮我查一下 Alice Wang 的报名」→ `getRegistrationsByName`
4. 「活动开始前能取消吗？」→ RAG 检索取消政策
5. 「我的社团活跃度等级是什么？」→ `getUserProfile`
6. 「算法分享会有多少人报名了？」→ `getActivityStats`
7. 复杂纠纷 → 转人工社团助手

## 技术栈

- **后端**：Spring Boot 3.5、Java 21、MyBatis-Plus、Spring AI、MongoDB、Redis 向量库
- **前端**：Vue 3、Element Plus、Pinia、ECharts

详细 API 见 [API.md](./API.md)。

## 部署（可选）

Docker Compose 与 CI 说明见 [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md)。

## Test plan（手工验证）

- [ ] 三角色登录后分别进入 `/user`、`/service`、`/admin`
- [ ] 未登录访问 `/api/admin/user/list` 返回 401
- [ ] 普通用户无法查看他人报名列表
- [ ] AI 对话 SSE 流式响应正常（需配置 DashScope）
- [ ] 管理端新增知识库后 AI 能检索到新内容
- [ ] 重复报名同一活动被拒绝
- [ ] `mvn test` 与 `npm run build` 通过
