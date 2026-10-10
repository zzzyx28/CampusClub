package com.backend.service;

import com.backend.entity.Activity;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDateTime;
import java.util.List;

public interface ActivityService extends IService<Activity> {

    Activity getByTicketCode(String ticketCode);

    Activity getByActivityCode(String activityCode);

    List<Activity> listWithTiers();

    Activity getByIdWithTiers(Long id);

    boolean saveWithTiers(Activity activity);

    boolean updateWithTiers(Activity activity);

    List<Activity> searchActivities(String activityType, String venue,
            LocalDateTime startTime, LocalDateTime endTime);

    List<Activity> listByClubName(String clubName);

    List<Activity> listUpcoming(int limit);
}
