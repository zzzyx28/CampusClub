package com.backend.controller;

import com.backend.common.Result;
import com.backend.common.enumeration.ResultCode;
import com.backend.entity.Registration;
import com.backend.exception.ForbiddenException;
import com.backend.service.RegistrationService;
import com.backend.util.AuthContext;
import com.backend.vo.RegistrationDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/registration")
public class RegistrationController {

    @Autowired
    private RegistrationService registrationService;

    @GetMapping("/{id}")
    public Result getRegistrationById(@PathVariable Long id) {
        ensureRegistrationAccess(id);
        Registration registration = registrationService.getById(id);
        if (registration == null) {
            return Result.error(ResultCode.NOT_FOUND, "报名记录不存在");
        }
        return Result.success(registration);
    }

    @GetMapping("/{id}/detail")
    public Result getRegistrationDetailById(@PathVariable Long id) {
        ensureRegistrationAccess(id);
        RegistrationDetail detail = registrationService.getRegistrationDetailById(id);
        if (detail == null) {
            return Result.error(ResultCode.NOT_FOUND, "报名记录不存在");
        }
        return Result.success(detail);
    }

    @GetMapping("/user/{userId}")
    public Result listRegistrationsByUserId(@PathVariable Long userId) {
        List<Registration> registrations = registrationService.listByUserId(userId);
        return Result.success(registrations);
    }

    @GetMapping("/user/{userId}/detail")
    public Result listRegistrationsWithDetail(@PathVariable Long userId) {
        List<Registration> registrations = registrationService.listRegistrationsWithDetail(userId);
        return Result.success(registrations);
    }

    @GetMapping("/activity/{activityId}")
    public Result listRegistrationsByActivityId(@PathVariable Long activityId) {
        List<Registration> registrations = registrationService.listByActivityId(activityId);
        return Result.success(registrations);
    }

    @PostMapping
    public Result createRegistration(@RequestBody Map<String, Object> body) {
        if (body.get("userId") == null || body.get("activityId") == null) {
            return Result.error(ResultCode.PARAM_ERROR, "用户ID与活动ID不能为空");
        }
        Long userId = Long.valueOf(body.get("userId").toString());
        if (!AuthContext.isAdmin() && !userId.equals(AuthContext.getUserId())) {
            throw new ForbiddenException("只能为本人创建报名");
        }
        Long activityId = Long.valueOf(body.get("activityId").toString());
        Object tierOpt = body.get("tierName");
        String tierName = tierOpt != null ? tierOpt.toString() : null;

        Registration registration = registrationService.createRegistration(userId, activityId, tierName);
        if (registration != null) {
            return Result.success("报名成功", registration);
        }
        return Result.error("报名失败");
    }

    @PutMapping("/{id}/cancel")
    public Result cancelRegistration(@PathVariable Long id) {
        ensureRegistrationAccess(id);
        Registration registration = registrationService.getById(id);
        if (registration == null) {
            return Result.error(ResultCode.NOT_FOUND, "报名记录不存在");
        }

        boolean success = registrationService.cancelRegistration(id);
        if (success) {
            return Result.success("报名已取消", null);
        }
        return Result.error("取消报名失败");
    }

    @PutMapping("/{id}/complete")
    public Result completeRegistration(@PathVariable Long id) {
        Registration registration = registrationService.getById(id);
        if (registration == null) {
            return Result.error(ResultCode.NOT_FOUND, "报名记录不存在");
        }
        boolean success = registrationService.completeRegistration(id);
        if (success) {
            return Result.success("已标记为已参加", null);
        }
        return Result.error("更新报名状态失败");
    }

    @GetMapping("/admin/list")
    public Result listAllRegistrations() {
        List<Registration> registrations = registrationService.list();
        return Result.success(registrations);
    }

    private void ensureRegistrationAccess(Long registrationId) {
        if (AuthContext.isServiceOrAdmin()) {
            return;
        }
        Registration registration = registrationService.getById(registrationId);
        if (registration == null) {
            return;
        }
        Long currentUserId = AuthContext.getUserId();
        if (currentUserId == null || !currentUserId.equals(registration.getUserId())) {
            throw new ForbiddenException("无权访问该报名记录");
        }
    }
}
