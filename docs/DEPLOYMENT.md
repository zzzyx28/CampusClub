# CampusClub 部署指南（可选）

## Docker Compose 本地全栈

```bash
# 设置 AI Key（可选）
export DASHSCOPE_API_KEY=your_key

docker compose up -d --build
```

访问：http://localhost（前端）、http://localhost:8080/actuator/health（后端健康检查）

## 环境变量

| 变量 | 说明 |
|------|------|
| `DASHSCOPE_API_KEY` | 阿里云百炼 API Key |
| `APP_JWT_SECRET` | JWT 签名密钥（至少 32 字符） |
| `VITE_APP_BASE_API` | 前端构建时 API 前缀（Docker 默认 `/api`） |

## Nginx SSE 要点

- `proxy_buffering off` 禁用缓冲
- `proxy_read_timeout` 拉长超时
- AI 流式接口路径：`/api/ai/generateStreamAsString`

## CI

GitHub Actions 工作流见 `.github/workflows/ci.yml`，推送时自动执行 `mvn test` 与 `npm run build`。
