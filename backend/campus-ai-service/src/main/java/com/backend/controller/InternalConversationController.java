package com.backend.controller;
import com.backend.security.InternalAuth;
import com.backend.util.AuthContext;
import com.backend.exception.ForbiddenException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
@RestController
public class InternalConversationController {
 private final JdbcTemplate jdbc;public InternalConversationController(JdbcTemplate jdbc){this.jdbc=jdbc;}
 @GetMapping("/internal/conversations/transferred-count") public Integer count(){
  InternalAuth.requireCaller("campus-ticket-service");if(!AuthContext.isAdmin())throw new ForbiddenException("仅管理员可查看统计");
  return jdbc.queryForObject("SELECT COUNT(*) FROM conversation_index WHERE status=2",Integer.class);
 }
}
