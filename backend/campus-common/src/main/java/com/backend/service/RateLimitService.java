package com.backend.service;

import com.backend.dao.redis.RedisDao;
import com.backend.exception.ForbiddenException;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class RateLimitService {

    private static final long EMAIL_COOLDOWN_SECONDS = 60;
    private static final int AI_MAX_REQUESTS_PER_MINUTE = 20;
    private static final long AI_WINDOW_MS = 60_000L;

    private final RedisDao redisDao;
    private final Map<Long, Deque<Long>> aiRequestTimestamps = new ConcurrentHashMap<>();

    public RateLimitService(RedisDao redisDao) {
        this.redisDao = redisDao;
    }

    public void checkEmailSendLimit(String email, String type) {
        String key = "email:send:cooldown:" + type + ":" + email;
        if (redisDao.getString(key) != null) {
            throw new ForbiddenException("验证码发送过于频繁，请稍后再试");
        }
        redisDao.setString(key, "1", EMAIL_COOLDOWN_SECONDS, TimeUnit.SECONDS);
    }

    @org.springframework.beans.factory.annotation.Value("${app.gateway.manages-ai-limit:false}") private boolean gatewayManaged;

    public void checkAiRateLimit(Long userId) {
        if(gatewayManaged) return;
        if (userId == null) {
            return;
        }
        long now = System.currentTimeMillis();
        Deque<Long> timestamps = aiRequestTimestamps.computeIfAbsent(userId, id -> new ArrayDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && now - timestamps.peekFirst() > AI_WINDOW_MS) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= AI_MAX_REQUESTS_PER_MINUTE) {
                throw new ForbiddenException("AI 请求过于频繁，请稍后再试");
            }
            timestamps.addLast(now);
        }
    }
}
