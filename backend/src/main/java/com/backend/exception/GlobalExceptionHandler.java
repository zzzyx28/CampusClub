package com.backend.exception;

import com.backend.common.Result;
import com.backend.common.enumeration.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @Value("${spring.profiles.active:}")
    private String activeProfile;

    @ExceptionHandler(UnauthorizedException.class)
    public Result handleUnauthorized(UnauthorizedException ex) {
        return Result.error(ResultCode.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public Result handleForbidden(ForbiddenException ex) {
        if (ex.getMessage() != null && ex.getMessage().contains("频繁")) {
            return Result.error(ResultCode.TOO_MANY_REQUESTS, ex.getMessage());
        }
        return Result.error(ResultCode.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result handleException(Exception ex) {
        log.error("Unhandled exception", ex);
        if (isProdProfile()) {
            return Result.error("对不起,操作失败,请联系管理员");
        }
        return Result.error("对不起,操作失败,请联系管理员", ex.getMessage());
    }

    private boolean isProdProfile() {
        return activeProfile != null && activeProfile.contains("prod");
    }
}
