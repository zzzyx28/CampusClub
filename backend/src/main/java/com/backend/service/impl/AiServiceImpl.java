package com.backend.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.backend.entity.Activity;
import com.backend.entity.Registration;
import com.backend.entity.User;
import com.backend.exception.ForbiddenException;
import com.backend.service.ActivityService;
import com.backend.service.AiService;
import com.backend.service.DashboardService;
import com.backend.service.RegistrationService;
import com.backend.service.UserService;
import com.backend.util.AiRequestContext;
import com.backend.vo.DashboardData;
import com.backend.vo.RegistrationDetail;
import com.backend.vo.UserProfileVo;

@Service
public class AiServiceImpl implements AiService {

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private ActivityService activityService;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private UserService userService;

    @Override
    public void cancelRegistration(Long registrationId) {
        if (registrationId == null) {
            throw new RuntimeException("报名 ID 不能为空");
        }
        RegistrationDetail detail = registrationService.getRegistrationDetailById(registrationId);
        if (detail == null) {
            throw new RuntimeException("报名记录不存在");
        }
        ensureRegistrationAccess(detail);
        boolean result = registrationService.cancelRegistration(registrationId);
        if (!result) {
            throw new RuntimeException("取消报名失败");
        }
    }

    @Override
    public Registration createRegistration(Long activityId, String activityCode, String tierName) {
        Long userId = AiRequestContext.requireUserId();
        Activity activity = resolveActivity(activityId, activityCode);
        Registration registration = registrationService.createRegistration(userId, activity.getId(), tierName);
        if (registration == null) {
            throw new RuntimeException("报名失败");
        }
        return registration;
    }

    @Override
    public RegistrationDetail getRegistrationDetail(Long registrationId) {
        if (registrationId == null) {
            throw new RuntimeException("报名 ID 不能为空");
        }
        RegistrationDetail detail = registrationService.getRegistrationDetailById(registrationId);
        if (detail == null) {
            throw new RuntimeException("报名记录不存在");
        }
        ensureRegistrationAccess(detail);
        return detail;
    }

    @Override
    public List<RegistrationDetail> getMyRegistrations() {
        Long userId = AiRequestContext.requireUserId();
        List<Registration> registrations = registrationService.listByUserId(userId);
        if (registrations == null || registrations.isEmpty()) {
            return List.of();
        }
        return registrations.stream()
                .filter(reg -> reg.getStatus() == null || reg.getStatus() != 2)
                .map(reg -> registrationService.getRegistrationDetailById(reg.getId()))
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public Activity getActivityByTicketCode(String ticketCode) {
        if (ticketCode == null || ticketCode.trim().isEmpty()) {
            throw new RuntimeException("票种编码不能为空");
        }
        Activity activity = activityService.getByTicketCode(ticketCode);
        if (activity == null) {
            throw new RuntimeException("未找到该活动");
        }
        return activity;
    }

    @Override
    public List<Activity> searchActivities(String activityType, String venue, String dateStr) {
        LocalDateTime startTime = null;
        LocalDateTime endTime = null;
        if (dateStr != null && !dateStr.isBlank()) {
            LocalDate date = LocalDate.parse(dateStr.trim());
            startTime = date.atStartOfDay();
            endTime = date.atTime(LocalTime.MAX);
        }

        List<Activity> activities = activityService.searchActivities(
                activityType != null ? activityType.trim() : null,
                venue != null ? venue.trim() : null,
                startTime,
                endTime);

        if (activities == null || activities.isEmpty()) {
            throw new RuntimeException("未找到符合条件的活动");
        }
        return activities;
    }

    @Override
    public List<Activity> getActivitiesByClub(String clubName) {
        if (clubName == null || clubName.trim().isEmpty()) {
            throw new RuntimeException("社团名称不能为空");
        }
        List<Activity> activities = activityService.listByClubName(clubName.trim());
        if (activities == null || activities.isEmpty()) {
            throw new RuntimeException("未找到该社团的活动");
        }
        return activities;
    }

    @Override
    public List<Activity> listUpcomingActivities(Integer limit) {
        int n = limit == null ? 5 : limit;
        List<Activity> activities = activityService.listUpcoming(n);
        return activities != null ? activities : List.of();
    }

    @Override
    public DashboardData getDashboardOverview() {
        if (!AiRequestContext.isAdmin()) {
            throw new ForbiddenException("仪表盘数据仅管理员可查看");
        }
        return dashboardService.getDashboardData();
    }

    @Override
    public UserProfileVo getCurrentUserProfile() {
        Long userId = AiRequestContext.requireUserId();
        User user = userService.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return UserProfileVo.from(user);
    }

    @Override
    public List<Map<String, Object>> getActivityStats(String activityTitle) {
        List<Activity> activities;
        if (activityTitle != null && !activityTitle.trim().isEmpty()) {
            activities = activityService.searchActivities(null, null, null, null);
            activities = activities.stream()
                    .filter(a -> a.getTitle().contains(activityTitle.trim()))
                    .toList();
        } else {
            activities = activityService.listUpcoming(20);
        }

        List<Map<String, Object>> stats = new ArrayList<>();
        for (Activity activity : activities) {
            List<Registration> regs = registrationService.listByActivityId(activity.getId());
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("activityId", activity.getId());
            entry.put("activityCode", activity.getActivityCode());
            entry.put("title", activity.getTitle());
            entry.put("clubName", activity.getClubName());
            entry.put("activityType", activity.getActivityType());
            entry.put("venue", activity.getVenue());
            entry.put("eventStart", activity.getEventStart() != null ? activity.getEventStart().toString() : null);
            entry.put("registrationCount", regs != null ? regs.size() : 0);
            entry.put("status", activity.getStatus() == 1 ? "开放报名" : "已关闭");
            stats.add(entry);
        }
        return stats;
    }

    private Activity resolveActivity(Long activityId, String activityCode) {
        if (activityId != null) {
            Activity activity = activityService.getById(activityId);
            if (activity != null) {
                return activity;
            }
        }
        if (StringUtils.hasText(activityCode)) {
            Activity activity = activityService.getByActivityCode(activityCode.trim());
            if (activity != null) {
                return activity;
            }
        }
        throw new RuntimeException("活动不存在，请先调用活动查询工具，并使用返回结果中的 id 或 activityCode");
    }

    private void ensureRegistrationAccess(RegistrationDetail detail) {
        Long currentUserId = AiRequestContext.currentUserId();
        if (currentUserId == null) {
            throw new ForbiddenException("未登录");
        }
        if (!AiRequestContext.isAdmin() && !currentUserId.equals(detail.getUserId())) {
            throw new ForbiddenException("无权访问他人报名记录");
        }
    }
}
