package com.backend.exception;

import com.backend.common.Result;
import com.backend.common.enumeration.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @Value("${spring.profiles.active:}")
    private String activeProfile;

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Result> handleUnauthorized(UnauthorizedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Result.error(ResultCode.UNAUTHORIZED, ex.getMessage()));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Result> handleForbidden(ForbiddenException ex) {
        if (ex.getMessage() != null && ex.getMessage().contains("频繁")) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Result.error(ResultCode.TOO_MANY_REQUESTS, ex.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Result.error(ResultCode.FORBIDDEN, ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Result> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Result.error(ResultCode.PARAM_ERROR, ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Result> handleConflict(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Result.error(ResultCode.CONFLICT, ex.getMessage()));
    }

    @ExceptionHandler(feign.FeignException.class)
    public ResponseEntity<Result> remote(feign.FeignException ex) {
        int status=ex.status();
        if(status!=400 && status!=401 && status!=403 && status!=404 && status!=409 && status!=429) status=503;
        return ResponseEntity.status(status).body(Result.error("依赖服务暂不可用或请求被拒绝"));
    }
    @ExceptionHandler(DependencyUnavailableException.class)
    public ResponseEntity<Result> dependency(DependencyUnavailableException ex) {
        return ResponseEntity.status(503).body(Result.error(ex.getMessage()));
    }
    @ExceptionHandler(MutationUncertainException.class)
    public ResponseEntity<Result> uncertain(MutationUncertainException ex) {
        return ResponseEntity.status(503).body(Result.error("操作结果待核对，请使用原操作标识查询"));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result> handleException(Exception ex) {
        log.error("Unhandled exception", ex);
        if (isProdProfile()) {
            return ResponseEntity.internalServerError()
                    .body(Result.error("对不起,操作失败,请联系管理员"));
        }
        return ResponseEntity.internalServerError()
                .body(Result.error("对不起,操作失败,请联系管理员", ex.getMessage()));
    }

    private boolean isProdProfile() {
        return activeProfile != null && activeProfile.contains("prod");
    }
}
