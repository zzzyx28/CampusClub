# CampusClub：企业级多 Agent 校园活动运营平台

基于 **Spring Cloud Alibaba、Spring AI、RAG 与持久化工作流** 的微服务与多 Agent 协作系统。它不把“多个角色提示词”当作多 Agent，而是用确定性控制面编排多个最小权限 Agent：任务拆解、并行执行、质量审校、人工审批、故障隔离和完整审计都落实在代码与数据模型中。


## 项目概述

CampusClub 面向校内社团活动与票务场景：讲座、比赛、招新、志愿等。核心业务为 **活动（Activity）**、**票档（TicketTier）** 与 **报名（Registration）**；静态政策通过 RAG 知识库检索，实时数据通过 AI 工具访问 MySQL。

## 微服务架构

```text
Browser → Spring Cloud Gateway → User / Ticket / AI
                                ↘ Nacos、Redis、RocketMQ
Ticket：活动 + 票档库存 + 报名（单一本地事务）
AI：会话 + 知识库 + 多 Agent（单实例）
```

后端是 Maven 多模块工程，使用 Spring Boot 3.5.3、Spring Cloud 2025.0.0 和 Spring Cloud Alibaba 2025.0.0.0。服务边界、报名时序、安全与故障处理见 [Spring Cloud 架构说明](docs/SPRING_CLOUD_ARCHITECTURE.md)。

## 快速开始（本地运行）

```bash
./scripts/cloud-up.sh
```

启动后访问前端 `http://localhost:8088`。默认模型与邮件均为明确标记的本地替身，无需外部密钥；真实供应商配置、监控、双实例和故障演示见 [部署说明](docs/DEPLOYMENT.md)。

核心验证：

```bash
mvn -f backend/pom.xml test
npm --prefix frontend run lint
npm --prefix frontend run build
docker compose -f docker-compose.yml -f deploy/compose-test.yml up -d
python3 scripts/smoke_cloud.py
```

演示账号见 [data/README.md](data/README.md)。三个新数据库独立初始化，原 `campusclub` 数据库保持不变。

## 核心能力

### 多 Agent 核心能力
- **确定性控制面**：意图路由和运行状态机由 Java 掌控，LLM 只负责边界清晰的数据面任务，避免依赖脆弱的规划 JSON。
- **并行任务 DAG**：活动侦察、报名核验、政策检索和数据分析可通过 Java 21 虚拟线程并行执行，再由质量审校 Agent 统一消解冲突。
- **最小权限工具隔离**：7 个 Agent 角色拥有不同工具白名单；政策 Agent 无业务写权限，报名顾问只读，只有审批后的事务 Agent 能调用报名/取消工具。
- **Human-in-the-loop**：所有报名数据写操作先停在 `WAITING_APPROVAL`，用户批准后才创建事务任务；拒绝不会修改业务数据。
- **可恢复持久化工作流**：运行、任务、依赖、重试、耗时和事件序列持久化到 MongoDB；重启后安全重放只读任务，对结果不确定的事务拒绝自动重放并要求核对。
- **弹性与降级**：按 Agent 设置超时、有限重试、熔断；单个专职 Agent 失败时，审校 Agent 仍可基于剩余证据输出降级结果。
- **并发安全**：数据库生成列 + 唯一索引保证同一用户/活动最多一条有效报名；取消采用条件更新，防止并发重复释放票档容量。
- **端到端可观测**：SSE 实时展示 Agent 协作轨迹，MongoDB 留存审计事件，Micrometer 记录运行结果与各角色耗时。
- **双层记忆机制**：MongoDB 保存完整对话窗口，同时维护 `agent_memory` 长期画像，沉淀用户姓名、偏好活动类型、常用场地、票档选择、最近意图与任务槽位。
- **多模型兼容配置**：基于 Spring AI 抽象层支持 DashScope 原生模式与 OpenAI-compatible 模式切换，可接入阿里云 compatible-mode、One API、LiteLLM 等网关，Agent 业务代码无需感知底层供应商。
- **可解释上下文注入**：每次 SSE 对话前动态组装“登录用户画像 + 长期摘要 + 偏好 + 槽位”，作为独立 system context 注入 Spring AI，减少长对话重复询问与上下文遗忘。
- **RAG + Tool Calling 混合编排**：政策/FAQ 走 Redis Vector Store 检索，实时活动、报名、统计走 11 个后端函数工具，避免模型凭空编造业务数据。

完整设计、状态机、故障矩阵与 SLO 见 [多 Agent 架构说明](docs/MULTI_AGENT_ARCHITECTURE.md)。

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
| 报名 | `getMyRegistrations` | 查询本人有效报名 |
| 报名 | `createRegistration` | 创建报名 |
| 报名 | `cancelRegistration` | 取消报名 |
| 统计 | `getDashboardOverview` | 仪表盘概览与趋势 |
| 统计 | `getActivityStats` | 活动报名统计 |
| 用户 | `getCurrentUserProfile` | 当前登录用户个人信息与权益 |

## Agent 团队（7 个角色）

| Agent | 职责 | 权限边界 |
|------|------|------|
| Activity Researcher | 实时活动、地点、票档检索 | 仅活动读取工具 |
| Registration Specialist | 核验本人报名与操作参数 | 仅报名读取工具 |
| Policy Analyst | RAG 政策解释 | 无业务工具 |
| Data Analyst | 运营统计与口径解释 | 统计工具，RBAC 继续生效 |
| Campus Guide | 一般咨询与能力导航 | 无业务工具 |
| Transaction Executor | 创建/取消报名 | 仅审批后临时启用写工具 |
| Quality Reviewer | 证据汇总、冲突处理、最终答复 | 无业务工具 |

## Demo 对话示例

1. 「最近有什么讲座，报名后最晚何时取消？」→ 活动、报名、政策 Agent 并行 → 审校 Agent 汇总
2. 「帮我报名算法分享会」→ 只读核验 → 用户审批 → 事务 Agent 执行 → 审校回执
3. 「算法分享会有多少人报名？」→ 数据 Agent 查询实时统计 → 审校输出口径
4. 「你好，你能做什么？」→ 校园向导单 Agent 快速路径
5. 复杂纠纷 → 转人工社团助手

## 技术栈

- **后端**：Spring Cloud Alibaba、Gateway、Nacos、Sentinel、OpenFeign、RocketMQ、Spring Boot 3.5、Java 21 Virtual Threads、MyBatis-Plus、Spring AI、MongoDB、Redis Vector Store、Micrometer/OTLP
- **前端**：Vue 3、Element Plus、Pinia、ECharts

详细 API 见 [API.md](./API.md)。

Spring AI 架构价值与技术选型说明见 [docs/SPRING_AI_VALUE.md](docs/SPRING_AI_VALUE.md)，模型供应商切换配置见 [docs/AI_PROVIDER_CONFIG.md](docs/AI_PROVIDER_CONFIG.md)。

## 部署（可选）

Docker Compose、Nacos 配置、监控、扩容与故障演示见 [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md)。

## Test plan（手工验证）

- [ ] 三角色登录后分别进入 `/user`、`/service`、`/admin`
- [ ] 未登录访问 `/api/admin/user/list` 返回 401
- [ ] 普通用户无法查看他人报名列表
- [ ] AI 对话 SSE 流式响应正常（需配置 DashScope 或 OpenAI-compatible Provider）
- [ ] 组合问题同时出现多个专职 Agent，最终由 Quality Reviewer 汇总
- [ ] 报名/取消请求停在审批门禁；拒绝后数据库不发生变化
- [ ] 模拟单个 Agent 超时后运行以 `PARTIALLY_COMPLETED` 降级完成
- [ ] 管理端新增知识库后 AI 能检索到新内容
- [ ] 重复报名同一活动被拒绝
- [x] `mvn test`、`npm run lint` 与 `npm run build` 通过
