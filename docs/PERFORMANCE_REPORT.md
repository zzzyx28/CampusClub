# CampusClub 本机并发验证报告

本报告由 `python3 scripts/benchmark_cloud.py` 实测生成。测试使用 Docker Compose 中的 1 个 Gateway、2 个 User、2 个 Ticket 和 1 个 AI 实例；Docker 可用内存约 7.8 GiB。主机环境：`macOS-26.5.1-arm64-arm-64bit-Mach-O`。测量时间：`2026-09-04T12:13:28+0800`。

## 工作负载与结果

- 100 个并发用户各提交一次报名，初始库存 10。
- 成功 10 次，最终有效报名 10 条，剩余库存 0，没有超卖。
- 状态分布：{'200': 10, '409': 60, '503': 30}。409 表示库存或并发业务冲突；503 表示请求超过当前 Sentinel 远程依赖并发隔离阈值，是预期的保护行为。
- 总耗时 0.821 秒，吞吐 121.88 请求/秒；P50 571.1 ms，P95 801.47 ms，最大 811.25 ms。

这是单台开发电脑、模型与邮件替身下的正确性和治理验证，不能外推为生产容量。调整 Nacos 中的 `app.remote.max-concurrent` 后应重新生成报告，结合数据库连接池、CPU、错误比例和延迟共同选择阈值。原始数据见 [PERFORMANCE_REPORT.json](PERFORMANCE_REPORT.json)。
