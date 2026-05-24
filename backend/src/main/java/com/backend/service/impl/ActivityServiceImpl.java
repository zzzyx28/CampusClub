package com.backend.service.impl;

import com.backend.dao.mapper.ActivityMapper;
import com.backend.dao.mapper.TicketTierMapper;
import com.backend.entity.Activity;
import com.backend.entity.TicketTier;
import com.backend.service.ActivityService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class ActivityServiceImpl extends ServiceImpl<ActivityMapper, Activity> implements ActivityService {

    @Autowired
    private TicketTierMapper ticketTierMapper;

    @Override
    public Activity getByTicketCode(String ticketCode) {
        TicketTier tier = ticketTierMapper.getByTicketCode(ticketCode);
        if (tier == null) {
            return null;
        }
        return getByIdWithTiers(tier.getActivityId());
    }

    @Override
    public Activity getByActivityCode(String activityCode) {
        LambdaQueryWrapper<Activity> q = new LambdaQueryWrapper<>();
        q.eq(Activity::getActivityCode, activityCode);
        return getOne(q);
    }

    @Override
    public List<Activity> listWithTiers() {
        List<Activity> activities = list();
        for (Activity activity : activities) {
            activity.setTierList(ticketTierMapper.listByActivityId(activity.getId()));
        }
        return activities;
    }

    @Override
    public Activity getByIdWithTiers(Long id) {
        Activity activity = getById(id);
        if (activity == null) {
            return null;
        }
        activity.setTierList(ticketTierMapper.listByActivityId(id));
        return activity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveWithTiers(Activity activity) {
        List<TicketTier> tierList = activity.getTierList() == null ? List.of() : activity.getTierList();
        prepareActivityAndTiers(activity, tierList, true);
        validateTierList(tierList);

        boolean ok = save(activity);
        if (!ok) {
            return false;
        }
        for (TicketTier tier : tierList) {
            tier.setId(null);
            tier.setActivityId(activity.getId());
            ticketTierMapper.insert(tier);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateWithTiers(Activity activity) {
        List<TicketTier> tierList = activity.getTierList() == null ? List.of() : activity.getTierList();
        prepareActivityAndTiers(activity, tierList, false);
        validateTierList(tierList);

        boolean ok = updateById(activity);
        if (!ok) {
            return false;
        }

        List<TicketTier> existingTiers = ticketTierMapper.listByActivityId(activity.getId());
        Set<Long> submittedIds = new HashSet<>();

        for (TicketTier tier : tierList) {
            tier.setActivityId(activity.getId());
            if (tier.getId() != null) {
                submittedIds.add(tier.getId());
                ticketTierMapper.updateById(tier);
            } else {
                ticketTierMapper.insert(tier);
                if (tier.getId() != null) {
                    submittedIds.add(tier.getId());
                }
            }
        }

        for (TicketTier existing : existingTiers) {
            if (submittedIds.contains(existing.getId())) {
                continue;
            }
            int refs = ticketTierMapper.countRegistrationRefsByTierId(existing.getId());
            if (refs == 0) {
                ticketTierMapper.deleteById(existing.getId());
            }
        }
        return true;
    }

    private void validateTierList(List<TicketTier> tierList) {
        if (tierList.isEmpty()) {
            throw new RuntimeException("至少需要一个票档");
        }
        int defaultCount = 0;
        List<String> codes = new ArrayList<>();
        for (TicketTier tier : tierList) {
            if (tier.getTicketCode() == null || tier.getTicketCode().isBlank()) {
                throw new RuntimeException("票种编码不能为空");
            }
            if (tier.getTierName() == null || tier.getTierName().isBlank()) {
                throw new RuntimeException("票档名称不能为空");
            }
            if (tier.getPrice() == null) {
                throw new RuntimeException("票价不能为空");
            }
            if (tier.getCapacity() == null || tier.getCapacity() < 0) {
                throw new RuntimeException("名额不能小于 0");
            }
            if (codes.contains(tier.getTicketCode())) {
                throw new RuntimeException("同一活动下票种编码不能重复");
            }
            codes.add(tier.getTicketCode());
            if (tier.getIsDefault() != null && tier.getIsDefault() == 1) {
                defaultCount++;
            }
        }
        if (defaultCount != 1) {
            throw new RuntimeException("必须且仅能设置一个默认票档");
        }
    }

    private void prepareActivityAndTiers(Activity activity, List<TicketTier> tierList, boolean isCreate) {
        if (isCreate || activity.getActivityCode() == null || activity.getActivityCode().isBlank()) {
            activity.setActivityCode(generateCode("ACT"));
        }
        for (TicketTier tier : tierList) {
            if (tier.getTicketCode() == null || tier.getTicketCode().isBlank()) {
                tier.setTicketCode(generateCode("TCK"));
            }
        }
    }

    private String generateCode(String prefix) {
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
        return prefix + "-" + token;
    }

    @Override
    public List<Activity> searchActivities(String activityType, String venue,
            LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<Activity> q = new LambdaQueryWrapper<>();
        q.eq(activityType != null && !activityType.isBlank(), Activity::getActivityType, activityType)
                .like(venue != null && !venue.isBlank(), Activity::getVenue, venue)
                .ge(startTime != null, Activity::getEventStart, startTime)
                .le(endTime != null, Activity::getEventStart, endTime)
                .eq(Activity::getStatus, 1)
                .orderByAsc(Activity::getEventStart);
        return list(q);
    }

    @Override
    public List<Activity> listByClubName(String clubName) {
        LambdaQueryWrapper<Activity> q = new LambdaQueryWrapper<>();
        q.eq(Activity::getClubName, clubName)
                .eq(Activity::getStatus, 1)
                .orderByAsc(Activity::getEventStart);
        return list(q);
    }

    @Override
    public List<Activity> listUpcoming(int limit) {
        int n = Math.min(Math.max(limit, 1), 30);
        LambdaQueryWrapper<Activity> q = new LambdaQueryWrapper<>();
        q.eq(Activity::getStatus, 1)
                .ge(Activity::getRegistrationStart, LocalDateTime.now())
                .orderByAsc(Activity::getRegistrationStart)
                .last("LIMIT " + n);
        List<Activity> upcoming = list(q);
        if (!upcoming.isEmpty()) {
            return upcoming;
        }
        q = new LambdaQueryWrapper<>();
        q.eq(Activity::getStatus, 1)
                .ge(Activity::getEventStart, LocalDateTime.now())
                .orderByAsc(Activity::getEventStart)
                .last("LIMIT " + n);
        return list(q);
    }
}
