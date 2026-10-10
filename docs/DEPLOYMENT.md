# 本地微服务部署与演示

## 启动

需要 JDK 21、Maven、Node.js 和 Docker Desktop。首次执行：

```bash
./scripts/cloud-up.sh
```

脚本生成权限为 `0600` 的 `.env`，构建所有 Maven 模块，启动 MySQL、MongoDB、Redis Stack、Nacos 3、RocketMQ 5、Jaeger、四个应用和前端，并初始化 Nacos 的非敏感配置。密钥不会写入 Nacos或仓库。

- 前端：http://localhost:8088
- Gateway：http://localhost:8080
- Nacos 控制台：http://localhost:18088
- Jaeger：http://localhost:16686

本地默认使用明确标记的模型替身和邮件替身。配置真实模型时设置 `AI_PROFILES=demo`、供应商开关和 API Key；真实邮件还需设置阿里云邮件变量并将 `MAIL_STUB=false`。

## 验证和扩容

```bash
mvn -f backend/pom.xml test
npm --prefix frontend run lint
npm --prefix frontend run build
docker compose config --quiet
docker compose -f docker-compose.yml -f deploy/compose-test.yml up -d
python3 scripts/smoke_cloud.py
./scripts/cloud-scale.sh
./scripts/verify-scale-failover.sh
```

动态配置演示：`./scripts/toggle-registration.py off` 会实时关闭报名，验证后执行 `./scripts/toggle-registration.py on` 恢复。消息恢复验证使用 `python3 scripts/test_broker_recovery.py`。压测前执行 `./scripts/prepare_load_demo.sh`，再按脚本输出的活动 ID 和管理员 Token 运行 `scripts/load_registration.js`；报告必须记录实际机器、并发、吞吐、P95、错误率和最终库存。

`compose-test.yml` 仅把三个业务服务绑定到本机回环端口，用于验证私有 API 鉴权；日常运行不公开这些端口。扩容后在 Nacos 查看两个 User 和两个 Ticket 实例，连续请求可观察负载分配。

监控组件：

```bash
docker compose --profile monitoring up -d prometheus grafana
```

Prometheus 位于 `localhost:9090`，Grafana 位于 `localhost:3000`，本地默认账号 `admin / campus-demo-admin`。执行 `./scripts/fault_demo.sh` 可以演示实例摘除和 503，不会删除数据。

## 数据与恢复

三个服务账号只拥有各自数据库。演示数据由 `demo` profile 的 Flyway V100 脚本创建，原来的 `campusclub` 库不会被修改。Outbox 与通知 worker 使用数据库租约；Broker 恢复后待发送事件继续投递。管理员在 `/api/admin/notifications` 查看 `FAILED` 和 `UNCERTAIN`，核对供应商记录后以 `confirmed=true` 调用补发接口。

清空本次微服务数据：

```bash
docker compose down -v
```
