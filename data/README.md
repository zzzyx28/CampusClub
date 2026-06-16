# CampusClub 数据文件

本目录集中存放数据库相关脚本。**表结构**由 Flyway 管理（`backend/src/main/resources/db/migration/`），**演示数据**在本目录的 `seed/` 下维护。

## 目录结构

```
data/
├── README.md           # 本说明
├── seed/
│   └── dev-data.sql    # 本地开发演示数据（幂等，可重复执行）
└── scripts/
    └── reset-dev.sql   # 清空演示数据（保留 admin 账号）
```

## 自动灌数（推荐）

本地 `local` profile 且 `app.seed.dev-data=true`（默认开启）时，后端启动会自动执行 `seed/dev-data.sql`。

关闭自动灌数：在 `application-local.yml` 中设置：

```yaml
app:
  seed:
    dev-data: false
```

## 演示账号

| 角色 | 用户名 | 密码 | 说明 |
|------|--------|------|------|
| 系统管理员 | `admin` | `admin123` | 由 `AdminInitializer` 创建，不在 seed SQL 中 |
| 普通用户 | `alice123` | `pass123` | |
| 普通用户 | `david_user` | `david2023` | |
| 普通用户 | `eva_user` | `eva321` | |
| 社团助手 | `charlie_cs` | `cs789` | |
| 管理员(演示) | `bob_admin` | `admin456` | user_type=3，可用于测试多管理员 |

## 演示内容

- **4 场活动**（讲座、辩论、志愿、音乐节）及对应票档
- **8 条报名记录**
- **4 条会话索引**

## 手动重置并重新导入

```bash
# 1. 清空演示数据（MySQL 容器示例）
docker exec -i campusclub-mysql-1 mysql -uroot -proot campusclub < data/scripts/reset-dev.sql

# 2. 重启后端，将自动重新执行 dev-data.sql
cd backend && mvn spring-boot:run
```

或手动导入种子：

```bash
mysql -uroot -p campusclub < data/seed/dev-data.sql
```

## 与 Flyway 的关系

| 类型 | 位置 | 执行时机 |
|------|------|----------|
| 建表迁移 | `backend/src/main/resources/db/migration/V*.sql` | 每次启动，Flyway 自动 |
| 演示数据 | `data/seed/dev-data.sql` | local profile 首次缺数据时 |

> 遗留的 `schema.sql` / `data.sql` 已弃用，请勿再使用。
