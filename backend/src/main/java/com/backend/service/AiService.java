package com.backend.service;

import java.util.List;
import java.util.Map;

import com.backend.entity.Activity;
import com.backend.entity.Registration;
import com.backend.entity.User;
import com.backend.vo.DashboardData;
import com.backend.vo.RegistrationDetail;

public interface AiService {

    void cancelRegistration(Long registrationId, String name);

    Registration createRegistration(Long userId, Long activityId, String tierName, String name);

    RegistrationDetail getRegistrationDetail(Long registrationId);

    List<RegistrationDetail> getRegistrationsByName(String realName);

    Activity getActivityByTicketCode(String ticketCode);

    List<Activity> searchActivities(String activityType, String venue, String dateStr);

    List<Activity> getActivitiesByClub(String clubName);

    List<Activity> listUpcomingActivities(Integer limit);

    DashboardData getDashboardOverview();

    User getUserProfile(Long userId);

    List<Map<String, Object>> getActivityStats(String activityTitle);
}
