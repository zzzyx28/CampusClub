package com.backend.client;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import com.backend.contract.*;
import com.backend.vo.*;
import java.util.List;
@FeignClient(name="campus-ticket-service",contextId="DashboardClient")
public interface DashboardClient {
 @GetMapping("/internal/dashboard") DashboardData getDashboardData();
}
