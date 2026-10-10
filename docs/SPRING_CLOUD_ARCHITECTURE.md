# Spring Cloud 微服务架构

## 服务边界

| 服务 | 公开 API | 私有 API | 数据所有权 |
|---|---|---|---|
| Gateway | 统一 `/api/**` | 无 | 无 |
| User | 认证、用户、日志、通知任务 | 批量用户资料 | `campus_user` |
| Ticket | 活动、票档、报名、仪表盘 | AI 查询与审批后报名 | `campus_ticket` |
| AI | 对话、知识库、Agent、SSE | 人工接待数量 | `campus_ai`、MongoDB、Redis 向量索引 |

活动、票档与报名属于一个一致性边界。扣减名额、写报名、记录幂等结果和写 Outbox 在同一个 MySQL 事务中完成，因此无需用分布式事务解决库存问题。跨服务表不建立外键，也不执行 SQL 联表。

## 一次报名的处理过程

1. Gateway 验证用户 JWT、清除伪造的内部身份头，并用 Redis 执行共享限流。
2. Ticket 服务再次验证 JWT 和资源归属。AI 发起写操作时，还必须提供服务签名中 `writeApproved=true` 且操作标识与 `Idempotency-Key=agent:<runId>` 一致。
3. Ticket 服务在事务之前调用 User 批量接口验证用户；Feign 超时为连接 1 秒、读取 3 秒且不自动重试。
4. Ticket 服务锁定幂等记录，以条件更新扣减库存，写入报名、结果快照和 Outbox。
5. Outbox worker 抢占带租约的记录并发送 RocketMQ。重复投递由 User 服务的 Inbox 主键去重。
6. User 服务建立通知任务。模型替身与邮件替身会明确标记；真实邮件响应不确定时任务进入 `UNCERTAIN`，只能由管理员核对后确认补发。

同键同参数返回已提交结果，同键不同参数返回 409。客户端收到写请求超时后以原键查询；Agent 不会换键重试，且不会把结果不确定表述为成功。

## 安全与故障边界

- 对外 JWT 由 User 服务签发，各应用独立验签。公开 API 不信任 `X-User-*`，Gateway 会删除所有内部身份头。
- 私有 API 不经 Gateway。服务身份令牌有效期 30 秒，包含固定签发方、受众、用户及操作上下文；接收方只尝试预配置的受信签发方密钥。
- Ticket 私有写入口只接受 AI 服务签发的审批上下文。模型工具参数无法生成该上下文。
- Sentinel 为每类远程客户端设置并发隔离、最小请求数、错误比例和熔断窗口。并发阈值可由 Nacos 更新。
- AI 服务首版为单实例，因为实时 SSE sink 与运行锁仍在进程内；运行、任务和事件保存在 MongoDB，重启只恢复安全的读取任务。

## 可观测性

所有服务暴露 Actuator 健康和 Prometheus 指标，但 Gateway 不代理 `/actuator/**`。Micrometer/OTLP 将同步 HTTP 链路发送到 Jaeger。Outbox、通知和 Agent 提供业务指标，消息携带 `traceId` 供审计关联。Grafana 默认面板包括吞吐、P95、JVM、Outbox 和通知失败。

## 取舍

本项目使用 OpenFeign 展示成熟的声明式服务调用；官方已将 OpenFeign 定位为功能完整、以后以修复为主，后续可以迁移到 Spring HTTP Service Clients。首版不拆库存、不使用 Seata，也不声称邮件 exactly-once 或 AI 多实例安全。
