# SmartShop：基于 MCP 与 RAG 的电商智能客服系统

本地 Git 目录名可仍为 `SmartAir`，与产品品牌 **SmartShop**、MySQL 库名 **`smartshop`** 相互独立。

## 项目概述

大语言模型与检索增强生成（RAG）正在改变电商客服的形态：用户期望即时、准确地获得订单、商品与售后政策类回答。传统人工或脚本客服在知识更新、多渠道一致性和峰值并发上往往吃力。

本系统将 **MCP（Model Context Protocol）** 与 **RAG** 结合，提供带会话记忆与向量检索的电商智能客服能力。业务数据以 **商品（SKU、类目、发货地、开售时间等）** 与 **订单** 为核心；静态政策类内容通过 `backend/src/main/resources/rag/ecommerce-knowledge.txt` 入库参与检索，工具调用则访问 MySQL 中的实时商品与订单数据。

## 快速开始（本地运行）

仓库里默认激活 Spring 配置 **`local`**（见 `backend/src/main/resources/application.yml`），而 **`application-*.yml` 被 `.gitignore` 忽略**，克隆后需要你在本机新建 `application-local.yml` 并准备 MySQL / MongoDB / Redis 与模型密钥，后端才能正常启动。

### 1. 环境要求

| 组件 | 说明 |
|------|------|
| **JDK** | 17（与 `backend/pom.xml` 中 `java.version` 一致） |
| **Maven** | 3.8+ |
| **Node.js** | 建议 18+（前端使用 Vite 7） |
| **MySQL** | 8.x，用于业务数据 |
| **MongoDB** | 用于会话内容、知识库文档等 |
| **Redis** | 用于验证码等缓存 |
| **百炼 / DashScope** | Spring AI 使用阿里云模型时需 [API Key](https://bailian.console.aliyun.com/) |

### 2. 初始化数据库

1. **全新安装**：在 MySQL 中执行 `backend/src/main/resources/schema.sql`（创建库 **`smartshop`** 及表结构），再执行 `backend/src/main/resources/data.sql`（示例用户、商品、订单等）。
2. **从旧版「航班 / smartair」升级**：若旧数据在库 `smartair` 且仍有 `flight` / `flight_order`，可将 `migration_flight_to_ecommerce.sql` 首行改为 `USE smartair` 执行删表与建表，再将整库迁移或重定向 JDBC 到 **`smartshop`**；或直接在新库执行 `schema.sql` + `data.sql` 重新初始化。
3. **仅刷新示例商品数据（保留用户等）**：可选用 `backend/src/main/resources/refresh_data.sql`（会清空并重灌 `product`、`product_order`）。

MongoDB、Redis 只需本机服务已启动，库名可在配置中自定（常用默认即可）。

### 3. 后端配置

在 **`backend/src/main/resources/application-local.yml`** 新建文件（勿提交到 Git），可按下面模板修改为你的本机地址与密钥：

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/smartshop?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: 你的密码
  data:
    mongodb:
      uri: mongodb://localhost:27017/smartshop
    redis:
      host: localhost
      port: 6379

  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY:请填写你的DashScope_API_Key}
      chat:
        options:
          model: qwen-plus

# 启动时若不存在则创建管理员账号（见 AdminInitializer）
admin:
  username: admin
  password: 请修改为强密码

# 发邮件验证码功能依赖；若暂无阿里云邮件，可填占位字符串保证能启动，真正发信需开通邮件推送并改为真实值
app:
  aliyun:
    mail:
      accessKeyId: placeholder
      accessKeySecret: placeholder
      accountName: placeholder@example.com
      fromAlias: SmartShop
```

也可将 `DASHSCOPE_API_KEY` 设为环境变量，在 YAML 里用占位符引用，避免把密钥写进文件。

在 `backend` 目录启动：

```bash
cd backend
mvn spring-boot:run
```

默认接口前缀为 **`/api/...`**，例如 AI 流式接口：`GET http://localhost:8080/api/ai/generateStreamAsString?message=你好`。

### 4. 前端配置与启动

开发环境 API 基址在 `frontend/.env.development` 中默认为 `http://localhost:8080/api`，与 Vite 代理一致。

**端口说明**：该文件里 `PORT=80` 在 Windows 上常需管理员权限。若启动失败，可将 `PORT` 改为 **`5173`**（或其它空闲端口）后再执行：

```bash
cd frontend
npm install
npm run dev
```

浏览器会按 Vite 配置自动打开；若改了端口，请访问控制台输出的本地地址（例如 `http://localhost:5173`）。

### 5. 常见问题

- **启动报缺少配置项**：检查 `application-local.yml` 是否放在 `backend/src/main/resources/` 下，且文件名是否为 **`application-local.yml`**（与 `spring.profiles.active: local` 对应）。
- **MySQL 连不上**：确认已执行 `schema.sql` / `data.sql`，账号密码与 JDBC URL 一致。
- **AI 接口报错**：确认 DashScope API Key 有效、账户有对应模型额度，且 `spring.ai.dashscope` 配置与当前 `spring-ai-alibaba-starter` 版本文档一致。

## 项目截图

![项目截图 (1).png](assets/%E9%A1%B9%E7%9B%AE%E6%88%AA%E5%9B%BE%20%281%29.png)

![项目截图 (2).png](assets/%E9%A1%B9%E7%9B%AE%E6%88%AA%E5%9B%BE%20%282%29.png)

![项目截图 (5).png](assets/%E9%A1%B9%E7%9B%AE%E6%88%AA%E5%9B%BE%20%285%29.png)

![项目截图 (6).png](assets/%E9%A1%B9%E7%9B%AE%E6%88%AA%E5%9B%BE%20%286%29.png)

![项目截图 (7).png](assets/%E9%A1%B9%E7%9B%AE%E6%88%AA%E5%9B%BE%20%287%29.png)

## 主要功能

系统整体功能围绕以下三类核心角色展开：

### 用户端
- 通过自然语言完成商品检索（类目、发货地、日期）、SKU 查询、下单与取消等（由后端工具调用数据库）
- 基于 RAG 检索退换货、物流、会员与支付等说明（以知识库文本为准）
- 用户身份与订单关联后可提供更一致的回答

### 客服端
- 智能判断并处理AI无法解决的异常请求
- 灵敏的"人工转接"机制，在必要时无缝转接
- 快速查看完整对话记录与用户信息，避免重复沟通
- 会话管理工具，包括实时消息监控、快速回复选项

### 管理端
- 统一后台管理知识库内容、配置提示词
- 管理模型行为和监控系统运行效果
- 上传和更新电商相关文档（售后规则、活动说明、商品政策等）
- 调整模型上下文参数，实现多轮对话的连贯控制
- 分析用户对话日志和系统反馈，持续优化AI表现
- 权限控制与敏感信息监管功能

## 技术栈

### 前端技术
- **核心框架**：Vue 3 + Composition API
- **UI设计**：针对购物用户、客服和管理员三类角色的界面
- **数据通信**：Axios（RESTful API请求封装、拦截与错误处理）
- **状态管理**：Pinia（对话记录暂存、用户信息保持和页面状态同步）
- **功能特点**：
  - 用户端：聊天窗口为核心，多轮对话和实时消息展示
  - 客服端：会话管理工具，实时消息监控，快速回复，转人工接入
  - 管理员端：知识文档维护，提示词配置，权限管理，系统日志查看

### 后端技术
- **开发框架**：Spring Boot
- **数据访问**：MyBatis-Plus（高效SQL操作，灵活映射数据结构）
- **架构设计**：MVC架构，AOP实现统一日志记录、安全控制和异常处理

### 数据库技术
- **关系型数据库**：MySQL
  - 存储内容：用户基础信息、商品与订单记录、会话索引、系统日志等
  - 特点：保证数据完整性和一致性，支持复杂查询和事务操作
  
- **非关系型数据库**：MongoDB
  - 存储内容：多轮对话文本、知识库文档、提示词模板
  - 特点：支持灵活JSON格式，易于存储管理非规则或动态变化数据
  
- **向量数据库**：Redis Stack
  - 存储内容：知识文本的高维语义向量表示
  - 算法：HNSW索引算法支持近似最近邻(ANN)搜索
  - 特点：内存数据库，提供极低延迟和高吞吐

## 技术实现亮点

### 智能服务与实际业务深度融合
- 自然语言交互作为核心入口，降低用户操作门槛
- "智能优先+人工兜底+人控知识"的协作机制
- AI智能检测问题难度，必要时自动转接人工客服
- 管理员可实时更新知识库，确保AI回答的专业性和实时性

### 基于MCP与RAG的对话增强机制
- MCP协议打破传统AI系统数据接入局限
- 系统可灵活接入多种信息源（知识文档、数据库、历史记录等）
- RAG技术实现"先检索、再生成"的答案生成流程
- 语义向量匹配定位相关知识，确保回答有据可依
- 特别适用于电商场景下政策条文多、需与订单数据对齐的客服需求

### 三种数据架构协同的存储策略
- 混合型数据架构满足不同类型数据管理需求
- 原始文本与向量数据分离存储提升检索效率
- 知识更新时自动重新生成向量表示并同步索引
- 结合高性能向量检索确保快速响应用户查询