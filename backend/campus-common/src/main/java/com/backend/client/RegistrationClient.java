package com.backend.client;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import com.backend.contract.*;
import com.backend.vo.*;
import java.util.List;
@FeignClient(name="campus-ticket-service",contextId="RegistrationClient")
public interface RegistrationClient {
 @GetMapping("/internal/registrations/user/{id}") List<RegistrationDto> listByUserId(@PathVariable("id") Long id);
 @GetMapping("/internal/registrations/activity/{id}") List<RegistrationDto> listByActivityId(@PathVariable("id") Long id);
 @GetMapping("/internal/registrations/{id}/detail") RegistrationDetail getRegistrationDetailById(@PathVariable("id") Long id);
 @PostMapping("/internal/registrations") RegistrationDto create(@RequestParam("userId") Long userId,@RequestParam("activityId") Long activityId,@RequestParam(value="tierName",required=false) String tierName,@RequestHeader("Idempotency-Key") String key);
 @PutMapping("/internal/registrations/{id}/cancel") boolean cancel(@PathVariable("id") Long id,@RequestHeader("Idempotency-Key") String key);
 @GetMapping("/internal/registrations/operations/{key}") OperationReceipt operation(@PathVariable("key") String key);
 default String operationKey() {
  var context=com.backend.util.AiRequestContext.get();
  if(context==null || !context.writeApproved() || context.operationId()==null) throw new com.backend.exception.ForbiddenException("写操作尚未审批");
  return "agent:"+context.operationId();
 }
 default RegistrationDto createRegistration(Long userId,Long activityId,String tierName) {
  String key=operationKey();
  try { return create(userId,activityId,tierName,key); }
  catch(feign.RetryableException e) {
   try { var receipt=operation(key); if(receipt!=null && "CREATE".equals(receipt.action())) return receipt.registration(); } catch(Exception ignored) {}
   com.backend.util.AiRequestContext.mutationUncertain();
   throw new com.backend.exception.MutationUncertainException("报名结果待核对，请使用原操作标识查询",e);
  }
 }
 default boolean cancelRegistration(Long id) {
  String key=operationKey();
  try { return cancel(id,key); }
  catch(feign.RetryableException e) {
   try { var receipt=operation(key); if(receipt!=null && "CANCEL".equals(receipt.action())) return receipt.cancelled(); } catch(Exception ignored) {}
   com.backend.util.AiRequestContext.mutationUncertain();
   throw new com.backend.exception.MutationUncertainException("取消结果待核对，请使用原操作标识查询",e);
  }
 }
}
