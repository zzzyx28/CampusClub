package com.backend.service;

import java.util.List;
import java.util.Map;

import com.backend.contract.ActivityDto;
import com.backend.contract.RegistrationDto;
import com.backend.vo.DashboardData;
import com.backend.vo.RegistrationDetail;
import com.backend.vo.UserProfileVo;

public interface AiService {

    void cancelRegistration(Long registrationId);

    RegistrationDto createRegistration(Long activityId, String activityCode, String tierName);

    RegistrationDetail getRegistrationDetail(Long registrationId);

    List<RegistrationDetail> getMyRegistrations();

    ActivityDto getActivityByTicketCode(String ticketCode);

    List<ActivityDto> searchActivities(String activityType, String venue, String dateStr);

    List<ActivityDto> getActivitiesByClub(String clubName);

    List<ActivityDto> listUpcomingActivities(Integer limit);

    DashboardData getDashboardOverview();

    UserProfileVo getCurrentUserProfile();

    List<Map<String, Object>> getActivityStats(String activityTitle);
}
