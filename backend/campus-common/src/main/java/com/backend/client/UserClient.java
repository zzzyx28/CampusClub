package com.backend.client;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import com.backend.contract.*;
import com.backend.vo.*;
import java.util.List;
@FeignClient(name="campus-user-service",contextId="UserClient")
public interface UserClient {
 @GetMapping("/internal/users/{id}") UserDto getById(@PathVariable("id") Long id);
 @PostMapping("/internal/users/batch") List<UserDto> batch(@RequestBody List<Long> ids);
 @GetMapping("/internal/users/by-name") UserDto getUserByRealName(@RequestParam("name") String name);
}
