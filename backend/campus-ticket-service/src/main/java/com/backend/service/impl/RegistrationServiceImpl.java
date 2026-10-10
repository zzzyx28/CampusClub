package com.backend.service.impl;

import com.backend.dao.mapper.RegistrationMapper;
import com.backend.dao.mapper.TicketTierMapper;
import com.backend.entity.Activity;
import com.backend.entity.Registration;
import com.backend.entity.TicketTier;
import com.backend.contract.UserDto;
import com.backend.service.ActivityService;
import com.backend.service.RegistrationService;
import com.backend.client.UserClient;
import com.backend.messaging.OutboxStore;
import com.backend.vo.RegistrationDetail;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RegistrationServiceImpl extends ServiceImpl<RegistrationMapper, Registration> implements RegistrationService {

    @Autowired
    private UserClient userService;

    @Autowired
    private ActivityService activityService;

    @Autowired
    private RegistrationMapper registrationMapper;

    @Autowired
    private TicketTierMapper ticketTierMapper;

    @Autowired
    private OutboxStore outbox;

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
        Activity activity = activityService.getById(activityId);
        if (activity == null) {
            throw new IllegalStateException("活动不存在");
        }
        if (activity.getStatus() == null || activity.getStatus() != 1) {
            throw new IllegalStateException("该活动当前不可报名");
        }

        LambdaQueryWrapper<Registration> duplicateQuery = new LambdaQueryWrapper<>();
        duplicateQuery.eq(Registration::getUserId, userId)
                .eq(Registration::getActivityId, activityId)
                .eq(Registration::getStatus, 1);
        if (count(duplicateQuery) > 0) {
            throw new IllegalStateException("您已报名该活动，请勿重复报名");
        }

        TicketTier selectedTier;
        if (tierName != null && !tierName.isBlank()) {
            selectedTier = ticketTierMapper.getByActivityIdAndTierName(activityId, tierName);
            if (selectedTier == null) {
                throw new IllegalStateException("所选票档不存在");
            }
        } else {
            selectedTier = ticketTierMapper.getDefaultByActivityId(activityId);
            if (selectedTier == null) {
                throw new IllegalStateException("该活动未配置默认票档");
            }
        }
        if (selectedTier.getCapacity() == null || selectedTier.getCapacity() <= 0) {
            throw new IllegalStateException("所选票档名额已满");
        }

        int decremented = ticketTierMapper.decrementCapacityIfAvailable(selectedTier.getId());
        if (decremented == 0) {
            throw new IllegalStateException("所选票档名额已满");
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
        enqueueNotification(registration, 1);

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
            throw new IllegalStateException("仅已报名的记录可取消");
        }

        UpdateWrapper<Registration> cancelUpdate = new UpdateWrapper<>();
        cancelUpdate.eq("id", registrationId)
                .eq("status", 1)
                .set("status", 2);
        boolean updated = update(cancelUpdate);
        if (!updated) {
            throw new IllegalStateException("报名状态已变更，请刷新后重试");
        }
        ticketTierMapper.incrementCapacity(registration.getTicketTierId(), registration.getTicketCount());
        if (updated) {
            enqueueNotification(registration, 2);
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean completeRegistration(Long registrationId) {
        Registration registration = getById(registrationId);
        if (registration == null) {
            return false;
        }
        if (registration.getStatus() == null || registration.getStatus() != 1) {
            throw new IllegalStateException("仅已报名的记录可标记为已参加");
        }
        registration.setStatus(3);
        return updateById(registration);
    }

    @Override
    public List<Registration> listRegistrationsWithDetail(Long userId) {
        List<Registration> registrations = listByUserId(userId);

        java.util.Map<Long,UserDto> users=userService.batch(registrations.stream().map(Registration::getUserId).distinct().toList())
            .stream().collect(java.util.stream.Collectors.toMap(UserDto::getId, u->u));
        return registrations.stream().map(reg -> {
            UserDto user = users.get(reg.getUserId());
            reg.setUser(user);

            Activity activity = activityService.getById(reg.getActivityId());
            reg.setActivity(activity);

            return reg;
        }).collect(Collectors.toList());
    }

    @Override
    public RegistrationDetail getRegistrationDetailById(Long id) {
        RegistrationDetail detail=registrationMapper.getRegistrationDetailById(id);
        if(detail!=null) {
            UserDto user=userService.getById(detail.getUserId());
            if(user!=null){ detail.setUsername(user.getUsername());detail.setRealName(user.getRealName());detail.setPhone(user.getPhone()); }
        }
        return detail;
    }

    @Override
    public List<RegistrationDetail> getRegistrationDetailsByName(String realName) {
        UserDto user = userService.getUserByRealName(realName);
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

    private void enqueueNotification(Registration registration, int status) {
        RegistrationDetail detail=registrationMapper.getRegistrationDetailById(registration.getId());
        if(detail==null) throw new IllegalStateException("报名详情不存在");
        outbox.append("REGISTRATION", new com.backend.contract.RegistrationNotification(
            registration.getUserId(),registration.getId(),detail.getTicketCode(),detail.getTierName(),
            detail.getEventStart()==null?"":detail.getEventStart().toString(),status));
    }
}
