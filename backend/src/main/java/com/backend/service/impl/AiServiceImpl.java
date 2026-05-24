package com.backend.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.backend.entity.Activity;
import com.backend.entity.Registration;
import com.backend.entity.User;
import com.backend.service.ActivityService;
import com.backend.service.AiService;
import com.backend.service.DashboardService;
import com.backend.service.RegistrationService;
import com.backend.service.UserService;
import com.backend.vo.DashboardData;
import com.backend.vo.RegistrationDetail;

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
    public void cancelRegistration(Long registrationId, String name) {
        RegistrationDetail detail = registrationService.getRegistrationDetailById(registrationId);
        if (detail == null) {
            throw new RuntimeException("报名记录不存在");
        }
        if (detail.getRealName().equals(name)) {
            boolean result = registrationService.cancelRegistration(registrationId);
            if (!result) {
                throw new RuntimeException("取消报名失败");
            }
        } else {
            throw new RuntimeException("姓名不匹配，无法取消报名");
        }
    }

    @Override
    public Registration createRegistration(Long userId, Long activityId, String tierName, String name) {
        if (userId == null || activityId == null) {
            throw new RuntimeException("用户ID和活动ID不能为空");
        }
        Registration registration = registrationService.createRegistration(userId, activityId, tierName);
        if (registration == null) {
            throw new RuntimeException("报名失败");
        }
        return registration;
    }

    @Override
    public RegistrationDetail getRegistrationDetail(Long registrationId) {
        if (registrationId == null) {
            throw new RuntimeException("报名ID不能为空");
        }
        RegistrationDetail detail = registrationService.getRegistrationDetailById(registrationId);
        if (detail == null) {
            throw new RuntimeException("报名记录不存在");
        }
        return detail;
    }

    @Override
    public List<RegistrationDetail> getRegistrationsByName(String realName) {
        if (realName == null || realName.trim().isEmpty()) {
            throw new RuntimeException("用户姓名不能为空");
        }
        List<RegistrationDetail> registrations = registrationService.getRegistrationDetailsByName(realName);
        if (registrations == null || registrations.isEmpty()) {
            throw new RuntimeException("未找到该用户的报名记录");
        }
        return registrations;
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
        return dashboardService.getDashboardData();
    }

    @Override
    public User getUserProfile(Long userId) {
        if (userId == null) {
            throw new RuntimeException("用户ID不能为空，请先登录");
        }
        User user = userService.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return user;
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
}
