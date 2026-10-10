# CampusClub 后端 API 文档

基础路径：`/api`（除 AI 流式接口外，均需 JWT `token` 请求头，公开接口除外）

## 活动 Activity

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/activity/list` | 活动列表（含票档） |
| GET | `/activity/{id}` | 活动详情 |
| GET | `/activity/ticket/{ticketCode}` | 按票种编码查活动 |
| GET | `/activity/search` | 搜索；参数：`activityType`, `venue`, `startTime`, `endTime` |
| GET | `/activity/club/{clubName}` | 按社团名列表 |
| POST | `/activity` | 新增活动（含 `tierList`） |
| PUT | `/activity/{id}` | 更新活动 |
| DELETE | `/activity/{id}` | 删除活动 |

### Activity 字段

`activityCode`, `title`, `clubName`, `activityType`, `description`, `venue`, `registrationStart`, `eventStart`, `eventEnd`, `posterUrl`, `status`, `tierList[]`

### TicketTier 字段

`ticketCode`, `tierName`, `price`, `originalPrice`, `capacity`, `isDefault`

## 报名 Registration

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/registration/{id}` | 报名记录 |
| GET | `/registration/{id}/detail` | 报名详情（含活动信息） |
| GET | `/registration/user/{userId}` | 用户报名列表 |
| GET | `/registration/user/{userId}/detail` | 用户报名详情列表 |
| GET | `/registration/activity/{activityId}` | 活动下报名列表 |
| POST | `/registration` | 创建报名；body: `{ userId, activityId, tierName? }` |
| PUT | `/registration/{id}/cancel` | 取消报名 |
| PUT | `/registration/{id}/complete` | 标记已参加 |
| GET | `/registration/admin/list` | 全部报名（管理） |

### RegistrationDetail 字段

`registrationId`, `ticketCode`, `tierName`, `ticketCount`, `unitPrice`, `totalPrice`, `status`, `activityTitle`, `clubName`, `activityType`, `venue`, `registrationStart`, `eventStart`, `eventEnd`, 及用户信息

状态：`1` 已报名，`2` 已取消，`3` 已参加

## 仪表盘

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/dashboard/overview` | 返回 `todayActivities`, `todayRegistrations`, `activeUsers`, `serviceTickets`, `dates`, `dailyActivities`, `dailyRegistrations` |

## 多 Agent 编排（推荐）

所有接口均需 JWT。运行记录按当前登录用户隔离，客户端不能通过参数冒充其他用户。

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/agent-runs` | 创建持久化运行；请求头支持 `Idempotency-Key` |
| GET | `/agent-runs/{runId}` | 获取运行、任务 DAG、状态与最终结果 |
| GET | `/agent-runs/{runId}/events` | SSE 事件流；支持 `after` 和 `Last-Event-ID` 断点续传 |
| POST | `/agent-runs/{runId}/approval` | 批准或拒绝写操作；body: `{ "approved": true }` |
| POST | `/agent-runs/{runId}/cancel` | 取消尚未结束的运行 |
| GET | `/agent-runs` | 当前用户最近 20 条运行 |

创建示例：

```http
POST /api/agent-runs
token: <jwt>
Idempotency-Key: 1c84fdf0-2c9c-4ad5-82a1-b45909d38b63
Content-Type: application/json

{
  "message": "最近有什么讲座，报名后最晚什么时候可以取消？",
  "conversationId": "3d524ccb-1ee9-4e75-bef5-e5957c74981c"
}
```

关键运行状态：`QUEUED` → `RUNNING` → `WAITING_APPROVAL`（仅写操作）→ `COMPLETED` / `PARTIALLY_COMPLETED` / `FAILED` / `CANCELLED`。

关键事件：`RUN_CREATED`、`PLAN_STARTED`、`TASK_STARTED`、`TASK_COMPLETED`、`TASK_FAILED`、`APPROVAL_REQUIRED`、`APPROVAL_GRANTED`、`RUN_COMPLETED`。

## 单 Agent 对话（兼容接口）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/ai/generateStreamAsString` | SSE 流式对话；参数：`message`, `conversationId`, `userId` |

新前端默认使用 `/agent-runs`。该接口保留用于兼容旧客户端和对照实验。

## 认证 / 用户 / 会话 / 知识库

与重构前一致，路径未变：

- `/auth/login`, `/auth/register`, `/auth/forgot-password` 等
- `/user/**`
- `/conversation/**`
- `/knowledge/**`
- `/prompt/**`
- `/admin/**`
- `/log/**`

## 数据库

- MySQL 库名：`campusclub`
- MongoDB：`mongodb://localhost:27017/campusclub`

## Spring Cloud 调用约定

所有原 `/api/**` 路径由 Gateway 保持兼容。报名创建及取消接受 `Idempotency-Key`（1–160 字符）；未传时旧客户端仍可调用，但无法在网络超时后主动复用原键。查询 `/api/registration/operations/{key}` 可核对当前用户的提交结果；同键不同参数返回 HTTP 409。

`/internal/**` 是服务私有契约，Gateway 固定返回 404。内部请求必须携带 30 秒有效的服务身份令牌；用户、票务和 AI 服务还会检查调用方、受众、资源归属与角色。AI 写请求额外要求审批声明和 `agent:<runId>` 操作键一致。

管理员通知接口：

- `GET /api/admin/notifications`：查看异步通知状态。
- `POST /api/admin/notifications/{eventId}/retry?confirmed=true`：核对邮件供应商后补发 `FAILED` 或 `UNCERTAIN` 任务。

Agent SSE 仍使用 `GET /api/agent-runs/{runId}/events`，支持 `Last-Event-ID` 或 `after` 恢复。Gateway 为该路由关闭响应超时，Nginx 关闭响应缓冲。
