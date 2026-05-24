package com.backend.service.impl;

import com.backend.dao.mapper.RegistrationMapper;
import com.backend.dao.mapper.TicketTierMapper;
import com.backend.entity.Activity;
import com.backend.entity.Registration;
import com.backend.entity.TicketTier;
import com.backend.entity.User;
import com.backend.service.ActivityService;
import com.backend.service.RegistrationService;
import com.backend.service.UserService;
import com.backend.vo.RegistrationDetail;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RegistrationServiceImpl extends ServiceImpl<RegistrationMapper, Registration> implements RegistrationService {

    @Autowired
    private UserService userService;

    @Autowired
    private ActivityService activityService;

    @Autowired
    private RegistrationMapper registrationMapper;

    @Autowired
    private TicketTierMapper ticketTierMapper;

    @Override
    public List<Registration> listByUserId(Long userId) {
        LambdaQueryWrapper<Registration> q = new LambdaQueryWrapper<>();
        q.eq(Registration::getUserId, userId)
                .orderByDesc(Registration::getCreatedAt);
        return list(q);
    }

    @Override
    public List<Registration> listByActivityId(Long activityId) {
        LambdaQueryWrapper<Registration> q = new LambdaQueryWrapper<>();
        q.eq(Registration::getActivityId, activityId)
                .orderByDesc(Registration::getCreatedAt);
        return list(q);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Registration createRegistration(Long userId, Long activityId, String tierName) {
        User user = userService.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        Activity activity = activityService.getById(activityId);
        if (activity == null) {
            throw new RuntimeException("活动不存在");
        }
        if (activity.getStatus() == null || activity.getStatus() != 1) {
            throw new RuntimeException("该活动当前不可报名");
        }

        TicketTier selectedTier;
        if (tierName != null && !tierName.isBlank()) {
            selectedTier = ticketTierMapper.getByActivityIdAndTierName(activityId, tierName);
            if (selectedTier == null) {
                throw new RuntimeException("所选票档不存在");
            }
        } else {
            selectedTier = ticketTierMapper.getDefaultByActivityId(activityId);
            if (selectedTier == null) {
                throw new RuntimeException("该活动未配置默认票档");
            }
        }
        if (selectedTier.getCapacity() == null || selectedTier.getCapacity() <= 0) {
            throw new RuntimeException("所选票档名额已满");
        }

        Registration registration = new Registration();
        registration.setUserId(userId);
        registration.setActivityId(activityId);
        registration.setTicketTierId(selectedTier.getId());
        registration.setTicketCount(1);
        registration.setUnitPrice(selectedTier.getPrice());
        registration.setTotalPrice(selectedTier.getPrice());
        registration.setStatus(1);

        save(registration);

        selectedTier.setCapacity(selectedTier.getCapacity() - 1);
        ticketTierMapper.updateById(selectedTier);

        return registration;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelRegistration(Long registrationId) {
        Registration registration = getById(registrationId);
        if (registration == null) {
            return false;
        }
        if (registration.getStatus() == null || registration.getStatus() != 1) {
            throw new RuntimeException("仅已报名的记录可取消");
        }

        registration.setStatus(2);

        TicketTier tier = ticketTierMapper.selectById(registration.getTicketTierId());
        if (tier != null) {
            tier.setCapacity(tier.getCapacity() + registration.getTicketCount());
            ticketTierMapper.updateById(tier);
        }

        return updateById(registration);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean completeRegistration(Long registrationId) {
        Registration registration = getById(registrationId);
        if (registration == null) {
            return false;
        }
        if (registration.getStatus() == null || registration.getStatus() != 1) {
            throw new RuntimeException("仅已报名的记录可标记为已参加");
        }
        registration.setStatus(3);
        return updateById(registration);
    }

    @Override
    public List<Registration> listRegistrationsWithDetail(Long userId) {
        List<Registration> registrations = listByUserId(userId);

        return registrations.stream().map(reg -> {
            User user = userService.getById(reg.getUserId());
            reg.setUser(user);

            Activity activity = activityService.getById(reg.getActivityId());
            reg.setActivity(activity);

            return reg;
        }).collect(Collectors.toList());
    }

    @Override
    public RegistrationDetail getRegistrationDetailById(Long id) {
        return registrationMapper.getRegistrationDetailById(id);
    }

    @Override
    public List<RegistrationDetail> getRegistrationDetailsByName(String realName) {
        User user = userService.getUserByRealName(realName);
        if (user == null) {
            return List.of();
        }

        List<Registration> registrations = listByUserId(user.getId());

        registrations = registrations.stream()
                .filter(reg -> reg.getStatus() != 2)
                .toList();

        return registrations.stream()
                .map(reg -> registrationMapper.getRegistrationDetailById(reg.getId()))
                .collect(Collectors.toList());
    }
}
