package com.backend.service.impl;

import com.backend.dao.mapper.DashboardMapper;
import com.backend.service.DashboardService;
import com.backend.vo.DashboardData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DashboardServiceImpl implements DashboardService {

    @Autowired
    private DashboardMapper dashboardMapper;

    @Autowired
    private com.backend.client.ConversationStatsClient conversationStats;

    @Override
    public DashboardData getDashboardData() {
        DashboardData data = dashboardMapper.getBaseStats();

        List<String> dates = dashboardMapper.getRecentDates();
        List<Integer> activityCounts = dashboardMapper.getRecentDatesActivityCount();
        List<Integer> registrationCounts = dashboardMapper.getRecentDatesRegistrationCount();

        data.setServiceTickets(conversationStats.transferredCount());
        data.setDates(dates);
        data.setDailyActivities(activityCounts);
        data.setDailyRegistrations(registrationCounts);

        return data;
    }
}
