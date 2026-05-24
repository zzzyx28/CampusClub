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

## AI 对话

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/ai/generateStreamAsString` | SSE 流式对话；参数：`message`, `conversationId`, `userId` |

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
