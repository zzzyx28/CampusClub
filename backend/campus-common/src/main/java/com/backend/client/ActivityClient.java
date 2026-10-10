package com.backend.client;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import com.backend.contract.*;
import com.backend.vo.*;
import java.util.List;
@FeignClient(name="campus-ticket-service",contextId="ActivityClient")
public interface ActivityClient {
 @GetMapping("/internal/activities/list") List<ActivityDto> listWithTiers();
 @GetMapping("/internal/activities/{id}") ActivityDto getById(@PathVariable("id") Long id);
 @GetMapping("/internal/activities/ticket") ActivityDto getByTicketCode(@RequestParam("code") String code);
 @GetMapping("/internal/activities/code") ActivityDto getByActivityCode(@RequestParam("code") String code);
 @GetMapping("/internal/activities/upcoming") List<ActivityDto> listUpcoming(@RequestParam("limit") int limit);
 @GetMapping("/internal/activities/club") List<ActivityDto> listByClubName(@RequestParam("club") String club);
 @GetMapping("/internal/activities/search") List<ActivityDto> searchActivities(@RequestParam(value="type",required=false) String type,@RequestParam(value="venue",required=false) String venue,@RequestParam(value="start",required=false) java.time.LocalDateTime start,@RequestParam(value="end",required=false) java.time.LocalDateTime end);
}
