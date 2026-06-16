package com.backend.aop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.AdvisedRequest;
import org.springframework.ai.chat.client.RequestResponseAdvisor;

import java.util.Map;

public class AiChatLoggingAdvisor implements RequestResponseAdvisor {

    private static final Logger log = LoggerFactory.getLogger(AiChatLoggingAdvisor.class);

    @Override
    public AdvisedRequest adviseRequest(AdvisedRequest request, Map<String, Object> context) {
        String conversationId = context != null ? String.valueOf(context.getOrDefault("conversationId", "unknown")) : "unknown";
        log.info("AI chat request started, conversationId={}", conversationId);
        return request;
    }
}
