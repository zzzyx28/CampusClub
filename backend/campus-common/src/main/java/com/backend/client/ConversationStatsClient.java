package com.backend.client;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import com.backend.contract.*;
import com.backend.vo.*;
import java.util.List;
@FeignClient(name="campus-ai-service",contextId="ConversationStatsClient")
public interface ConversationStatsClient {
 @GetMapping("/internal/conversations/transferred-count") Integer transferredCount();
}
