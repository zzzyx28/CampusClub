package com.backend.service;

import java.util.List;
import java.util.Map;

import com.backend.entity.Activity;
import com.backend.entity.Registration;
import com.backend.vo.DashboardData;
import com.backend.vo.RegistrationDetail;
import com.backend.vo.UserProfileVo;

public interface AiService {

    void cancelRegistration(Long registrationId);

    Registration createRegistration(Long activityId, String activityCode, String tierName);

    RegistrationDetail getRegistrationDetail(Long registrationId);

    List<RegistrationDetail> getMyRegistrations();

    Activity getActivityByTicketCode(String ticketCode);

    List<Activity> searchActivities(String activityType, String venue, String dateStr);

    List<Activity> getActivitiesByClub(String clubName);

    List<Activity> listUpcomingActivities(Integer limit);

    DashboardData getDashboardOverview();

    UserProfileVo getCurrentUserProfile();

    List<Map<String, Object>> getActivityStats(String activityTitle);
}
