package com.backend.controller;
import com.backend.service.*;
import com.backend.entity.*;
import com.backend.contract.*;
import com.backend.vo.*;
import com.backend.util.AuthContext;
import com.backend.security.InternalAuth;
import com.backend.exception.ForbiddenException;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import java.util.List;
@RestController
@RequestMapping("/internal")
public class InternalTicketController {
 private final ActivityService activities; private final RegistrationService registrations;
 private final RegistrationCommandService commands;private final DashboardService dashboard;
 public InternalTicketController(ActivityService a,RegistrationService r,RegistrationCommandService c,DashboardService d){activities=a;registrations=r;commands=c;dashboard=d;}
 private void allowed(){InternalAuth.requireCaller("campus-ai-service");}
 private void staff(){allowed();if(!AuthContext.isServiceOrAdmin())throw new ForbiddenException("仅工作人员可查看统计");}
 private void owned(Long id){allowed();var r=registrations.getById(id);if(r!=null)AuthContext.ensureSelfOrServiceOrAdmin(r.getUserId());}
 @GetMapping("/activities/list") public List<Activity> list(){allowed();return activities.listWithTiers();}
 @GetMapping("/activities/{id}") public Activity get(@PathVariable Long id){allowed();return activities.getById(id);}
 @GetMapping("/activities/ticket") public Activity ticket(@RequestParam String code){allowed();return activities.getByTicketCode(code);}
 @GetMapping("/activities/code") public Activity code(@RequestParam String code){allowed();return activities.getByActivityCode(code);}
 @GetMapping("/activities/upcoming") public List<Activity> upcoming(@RequestParam int limit){allowed();return activities.listUpcoming(Math.min(Math.max(limit,1),100));}
 @GetMapping("/activities/club") public List<Activity> club(@RequestParam String club){allowed();return activities.listByClubName(club);}
 @GetMapping("/activities/search") public List<Activity> search(@RequestParam(required=false) String type,@RequestParam(required=false) String venue,
 @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
 @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) LocalDateTime end){allowed();return activities.searchActivities(type,venue,start,end);}
 @GetMapping("/registrations/user/{id}") public List<Registration> user(@PathVariable Long id){allowed();AuthContext.ensureSelfOrServiceOrAdmin(id);return registrations.listByUserId(id);}
 @GetMapping("/registrations/activity/{id}") public List<Registration> activity(@PathVariable Long id){staff();return registrations.listByActivityId(id);}
 @GetMapping("/registrations/{id}/detail") public RegistrationDetail detail(@PathVariable Long id){owned(id);return registrations.getRegistrationDetailById(id);}
 @PostMapping("/registrations") public RegistrationDto create(@RequestParam Long userId,@RequestParam Long activityId,@RequestParam(required=false) String tierName,@RequestHeader("Idempotency-Key") String key){InternalAuth.requireApproved(key);AuthContext.ensureSelfOrAdmin(userId);return commands.create(userId,activityId,tierName,key).registration();}
 @PutMapping("/registrations/{id}/cancel") public boolean cancel(@PathVariable Long id,@RequestHeader("Idempotency-Key") String key){InternalAuth.requireApproved(key);owned(id);return commands.cancel(id,key).cancelled();}
 @GetMapping("/registrations/operations/{key}") public OperationReceipt operation(@PathVariable String key){allowed();return commands.find(key);}
 @GetMapping("/dashboard") public DashboardData dashboard(){allowed();if(!AuthContext.isAdmin())throw new ForbiddenException("仅管理员可查看仪表盘");return dashboard.getDashboardData();}
}
