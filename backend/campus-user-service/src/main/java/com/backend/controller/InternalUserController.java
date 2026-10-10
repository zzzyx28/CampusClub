package com.backend.controller;
import com.backend.service.UserService;
import com.backend.contract.UserDto;
import com.backend.util.AuthContext;
import com.backend.security.InternalAuth;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController
@RequestMapping("/internal/users")
public class InternalUserController {
 private final UserService users;private final ObjectMapper json;
 public InternalUserController(UserService users,ObjectMapper json){this.users=users;this.json=json;}
 private void allowed(){InternalAuth.requireCaller("campus-ticket-service","campus-ai-service");}
 @GetMapping("/{id}") public UserDto get(@PathVariable Long id){allowed();AuthContext.ensureSelfOrServiceOrAdmin(id);return json.convertValue(users.getById(id),UserDto.class);}
 @PostMapping("/batch") public List<UserDto> batch(@RequestBody List<Long> ids){
  allowed();if(ids.size()>200)throw new IllegalArgumentException("批量查询最多 200 个用户");
  ids.forEach(AuthContext::ensureSelfOrServiceOrAdmin);
  if(ids.isEmpty())return List.of();return users.listByIds(ids).stream().map(u->json.convertValue(u,UserDto.class)).toList();
 }
 @GetMapping("/by-name") public UserDto name(@RequestParam String name){allowed();if(!AuthContext.isServiceOrAdmin())throw new com.backend.exception.ForbiddenException("仅工作人员可按姓名查询");return json.convertValue(users.getUserByRealName(name),UserDto.class);}
}
