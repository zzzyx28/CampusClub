package com.backend.service;

import com.backend.entity.Registration;
import com.backend.vo.RegistrationDetail;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface RegistrationService extends IService<Registration> {

    List<Registration> listByUserId(Long userId);

    List<Registration> listByActivityId(Long activityId);

    Registration createRegistration(Long userId, Long activityId, String tierName);

    boolean cancelRegistration(Long registrationId);

    boolean completeRegistration(Long registrationId);

    List<Registration> listRegistrationsWithDetail(Long userId);

    RegistrationDetail getRegistrationDetailById(Long id);

    List<RegistrationDetail> getRegistrationDetailsByName(String realName);
}
