package com.backend.controller;

import com.backend.common.Result;
import com.backend.common.enumeration.ResultCode;
import com.backend.entity.Activity;
import com.backend.service.ActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/activity")
public class ActivityController {

    @Autowired
    private ActivityService activityService;

    @GetMapping("/list")
    public Result listActivities() {
        List<Activity> activities = activityService.listWithTiers();
        return Result.success(activities);
    }

    @GetMapping("/{id}")
    public Result getActivityById(@PathVariable Long id) {
        Activity activity = activityService.getByIdWithTiers(id);
        if (activity == null) {
            return Result.error(ResultCode.NOT_FOUND, "活动不存在");
        }
        return Result.success(activity);
    }

    @GetMapping("/ticket/{ticketCode}")
    public Result getActivityByTicketCode(@PathVariable String ticketCode) {
        Activity activity = activityService.getByTicketCode(ticketCode);
        if (activity == null) {
            return Result.error(ResultCode.NOT_FOUND, "活动不存在");
        }
        return Result.success(activity);
    }

    @GetMapping("/search")
    public Result searchActivities(
            @RequestParam(required = false) String activityType,
            @RequestParam(required = false) String venue,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        List<Activity> activities = activityService.searchActivities(activityType, venue, startTime, endTime);
        return Result.success(activities);
    }

    @GetMapping("/club/{clubName}")
    public Result listByClubName(@PathVariable String clubName) {
        List<Activity> activities = activityService.listByClubName(clubName);
        return Result.success(activities);
    }

    @PostMapping
    public Result addActivity(@RequestBody Activity activity) {
        if (activity.getActivityCode() != null && !activity.getActivityCode().isBlank()) {
            Activity existing = activityService.getByActivityCode(activity.getActivityCode());
            if (existing != null) {
                return Result.error(ResultCode.PARAM_ERROR, "活动编号已存在");
            }
        }

        boolean success = activityService.saveWithTiers(activity);
        if (success) {
            return Result.success("添加活动成功", activity);
        }
        return Result.error("添加活动失败");
    }

    @PutMapping("/{id}")
    public Result updateActivity(@PathVariable Long id, @RequestBody Activity activity) {
        activity.setId(id);
        Activity existing = activityService.getById(id);
        if (existing == null) {
            return Result.error(ResultCode.NOT_FOUND, "活动不存在");
        }

        if (activity.getActivityCode() != null && !activity.getActivityCode().isBlank()
                && !activity.getActivityCode().equals(existing.getActivityCode())) {
            Activity sameCode = activityService.getByActivityCode(activity.getActivityCode());
            if (sameCode != null) {
                return Result.error(ResultCode.PARAM_ERROR, "活动编号已被其他活动使用");
            }
        }

        boolean success = activityService.updateWithTiers(activity);
        if (success) {
            return Result.success("更新活动成功", activity);
        }
        return Result.error("更新活动失败");
    }

    @DeleteMapping("/{id}")
    public Result deleteActivity(@PathVariable Long id) {
        Activity activity = activityService.getById(id);
        if (activity == null) {
            return Result.error(ResultCode.NOT_FOUND, "活动不存在");
        }

        boolean success = activityService.removeById(id);
        if (success) {
            return Result.success("删除活动成功", null);
        }
        return Result.error("删除活动失败");
    }
}
