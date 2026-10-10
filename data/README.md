# CampusClub 演示数据

微服务通过各自的 Flyway `db/demo` 迁移装载演示数据，使用 `demo` profile 时自动执行。旧单体脚本仍保存在 `data/seed/dev-data.sql`，只作为历史数据参考。

| 数据所有者 | 表 | 演示内容 |
|---|---|---|
| User | `user`、`system_log` | 普通用户、社团助手和管理员 |
| Ticket | `activity`、`ticket_tier`、`registration` | 4 场活动、票档和 3 条报名 |
| AI | `conversation_index`、MongoDB | 4 条会话索引；新会话写入 MongoDB |

演示账号：

| 角色 | 用户名 | 密码 |
|---|---|---|
| 系统管理员 | `admin` | `admin123` |
| 普通用户 | `alice123` | `pass123` |
| 普通用户 | `david_user` | `david2023` |
| 普通用户 | `eva_user` | `eva321` |
| 社团助手 | `charlie_cs` | `cs789` |
| 管理员 | `bob_admin` | `admin456` |

重建微服务数据不会触碰旧 `campusclub` 库：

```bash
docker compose down -v
./scripts/cloud-up.sh
```

表结构迁移位于每个服务的 `src/main/resources/db/migration`，演示迁移位于 `src/main/resources/db/demo`。`scripts/prepare_load_demo.sh` 单独准备带 `load_` 前缀的压测用户和 10 个名额，不应在生产环境运行。
