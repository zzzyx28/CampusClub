package com.backend.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardData {
    private Integer todayActivities;
    private Integer todayRegistrations;
    private Integer activeUsers;
    private Integer serviceTickets;

    private List<String> dates;
    private List<Integer> dailyActivities;
    private List<Integer> dailyRegistrations;
}
