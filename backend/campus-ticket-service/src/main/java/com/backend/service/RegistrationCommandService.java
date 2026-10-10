package com.backend.service;
import com.backend.client.UserClient;
import com.backend.contract.*;
import com.backend.util.AuthContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import java.util.*;
import java.util.function.Supplier;
@Service
public class RegistrationCommandService {
 @org.springframework.beans.factory.annotation.Autowired private org.springframework.core.env.Environment environment;
 private final RegistrationService registrations;private final UserClient users;private final JdbcTemplate jdbc;private final TransactionTemplate tx;private final ObjectMapper json;
 public RegistrationCommandService(RegistrationService registrations,UserClient users,JdbcTemplate jdbc,PlatformTransactionManager manager,ObjectMapper json){
  this.registrations=registrations;this.users=users;this.jdbc=jdbc;this.tx=new TransactionTemplate(manager);this.json=json;
 }
 public OperationReceipt create(Long userId,Long activityId,String tierName,String key) {
  AuthContext.ensureSelfOrAdmin(userId);
  String params=encode(Arrays.asList("CREATE",userId,activityId,tierName));
  OperationReceipt previous=existing(key,params);if(previous!=null)return previous;
  if(!environment.getProperty("app.registration.enabled",Boolean.class,true))throw new IllegalStateException("报名暂时关闭");
  if(users.getById(userId)==null)throw new IllegalArgumentException("用户不存在");
  return execute(key,params,()->new OperationReceipt("CREATE",json.convertValue(registrations.createRegistration(userId,activityId,tierName),RegistrationDto.class),false));
 }
 public OperationReceipt cancel(Long id,String key) {
  String params=encode(Arrays.asList("CANCEL",id));OperationReceipt previous=existing(key,params);if(previous!=null)return previous;
  var registration=registrations.getById(id);if(registration==null)throw new IllegalArgumentException("报名不存在");
  AuthContext.ensureSelfOrServiceOrAdmin(registration.getUserId());
  return execute(key,params,()->new OperationReceipt("CANCEL",null,registrations.cancelRegistration(id)));
 }
 public OperationReceipt find(String key){validate(key);var rows=jdbc.queryForList("SELECT result_json FROM registration_operation WHERE actor_id=? AND operation_key=?",actor(),key);return rows.isEmpty()?null:decode((String)rows.getFirst().get("result_json"));}
 private OperationReceipt existing(String key,String params){
  validate(key);var rows=jdbc.queryForList("SELECT request_json,result_json FROM registration_operation WHERE actor_id=? AND operation_key=?",actor(),key);
  if(rows.isEmpty())return null;var row=rows.getFirst();if(!params.equals(row.get("request_json")))throw new IllegalStateException("幂等键已用于不同参数");return decode((String)row.get("result_json"));
 }
 private OperationReceipt execute(String key,String params,Supplier<OperationReceipt> mutation) {
  return tx.execute(status->{
   jdbc.update("INSERT INTO registration_operation(actor_id,operation_key,request_json) VALUES(?,?,?) ON DUPLICATE KEY UPDATE operation_key=operation_key",actor(),key,params);
   var row=jdbc.queryForMap("SELECT request_json,result_json FROM registration_operation WHERE actor_id=? AND operation_key=? FOR UPDATE",actor(),key);
   if(!params.equals(row.get("request_json")))throw new IllegalStateException("幂等键已用于不同参数");
   if(row.get("result_json")!=null)return decode((String)row.get("result_json"));
   OperationReceipt receipt=mutation.get();
   jdbc.update("UPDATE registration_operation SET result_json=? WHERE actor_id=? AND operation_key=?",encode(receipt),actor(),key);
   return receipt;
  });
 }
 private Long actor(){Long id=AuthContext.getUserId();if(id==null)throw new com.backend.exception.UnauthorizedException("未登录");return id;}
 private void validate(String key){if(key==null || key.isBlank() || key.length()>160)throw new IllegalArgumentException("Idempotency-Key 必须为 1–160 个字符");}
 private String encode(Object value){try{return json.writeValueAsString(value);}catch(Exception e){throw new IllegalStateException("不能序列化操作",e);}}
 private OperationReceipt decode(String value){if(value==null)return null;try{return json.readValue(value,OperationReceipt.class);}catch(Exception e){throw new IllegalStateException("不能读取操作结果",e);}}
}
