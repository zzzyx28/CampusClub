# Spring AI 在 CampusClub 中的价值

CampusClub 不是单轮问答机器人，而是一个带业务状态的校园活动 Agent：它需要理解用户意图、检索政策知识、调用活动/报名/统计工具、保存多轮记忆，并在高风险动作前执行确认流程。Spring AI 的意义在于把这些能力统一到 Spring Boot 的运行时里，而不是散落成脚本式的大模型调用。

## 为什么不是更轻量的技术栈

如果只做「输入一句话，调用一次模型，返回文本」，普通 HTTP SDK 或 Python 脚本更轻。但 CampusClub 的核心复杂度不在模型接口，而在业务编排：

- **多数据源协同**：MySQL 保存活动/报名，MongoDB 保存会话与长期记忆，Redis Vector Store 保存知识库向量。
- **多工具调用**：活动查询、报名、取消、统计、用户画像等 11 个函数工具需要统一暴露给模型，并复用已有 Spring Service 权限与事务边界。
- **多轮上下文治理**：短期聊天窗口、长期 `agent_memory`、登录用户画像和 RAG 片段要在每次请求前组合成稳定上下文。
- **高风险动作保护**：报名/取消不能让模型直接执行，需要通过提示词、工具边界和确认流程共同约束。
- **可观测与可调参**：RAG topK、相似度阈值、记忆窗口、流式响应耗时需要可配置、可记录、可回归测试。

轻量栈可以更快做 demo，但这些能力往往需要手写胶水代码。Spring AI 让它们成为 Spring Boot 应用的一等公民。

## 当前项目如何体现 Spring AI

| Spring AI 能力 | 项目落点 | 价值 |
|---|---|---|
| `ChatClient` | `AiController` 统一构建流式对话 | 屏蔽底层模型调用细节，支持 SSE 输出 |
| `PromptChatMemoryAdvisor` | Mongo backed `ChatMemory` | 自动注入短期对话窗口 |
| 自定义 `AgentMemoryService` | MongoDB `agent_memory` | 沉淀长期用户画像、偏好、槽位和最近意图 |
| `QuestionAnswerAdvisor` | Redis Vector Store + 知识库切片 | 政策/FAQ 走 RAG，降低幻觉 |
| Function Calling | `AiToolsConfig` 11 个业务工具 | 模型通过受控工具访问实时业务数据 |
| 自定义 Advisor | `AiChatLoggingAdvisor` | 记录 conversationId、记忆窗口、RAG 参数、流式耗时 |
| Actuator Info | `AgentRuntimeInfoContributor` | 暴露 Agent 运行时配置，便于演示与排障 |

## 可在简历中强调的表达

> 基于 Spring AI 构建校园活动 Agent Runtime，整合 ChatClient、ChatMemory Advisor、QuestionAnswerAdvisor、Function Calling 与自定义 Advisor，实现 RAG 知识问答、业务工具编排、MongoDB 双层记忆、流式响应与运行时可观测；相比简单 SDK 调用，系统将模型能力纳入 Spring Boot 的配置、服务、日志和权限边界，支持可维护的生产级 Agent 架构。

## 后续可量化指标

- 工具调用准确率
- RAG 命中率与答案忠实度
- 长期记忆字段提取准确率
- 多步任务完成率
- 危险操作误执行率
- 首 token 延迟、完整响应耗时、工具调用耗时、RAG 检索耗时
