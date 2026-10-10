package com.backend.controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import com.backend.common.Result;
@RestController @RequestMapping("/api/admin/notifications")
public class NotificationController {
 private final JdbcTemplate jdbc;public NotificationController(JdbcTemplate jdbc){this.jdbc=jdbc;}
 @GetMapping public Result list(){return Result.success(jdbc.queryForList("SELECT event_id,status,attempts,last_error,trace_id,created_at,updated_at FROM notification_task ORDER BY created_at DESC LIMIT 200"));}
 @PostMapping("/{id}/retry") public Result retry(@PathVariable String id,@RequestParam(defaultValue="false") boolean confirmed){
  if(!confirmed)throw new IllegalArgumentException("请核对供应商记录后明确确认补发");
  int changed=jdbc.update("UPDATE notification_task SET status='PENDING',updated_at=NOW(6) WHERE event_id=? AND status IN ('FAILED','UNCERTAIN')",id);
  if(changed!=1)throw new IllegalStateException("任务不存在或当前状态不允许补发");return Result.success("已安排补发",null);
 }
}
