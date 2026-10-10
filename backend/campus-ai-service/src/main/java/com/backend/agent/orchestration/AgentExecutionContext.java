package com.backend.agent.orchestration;

public record AgentExecutionContext(
        String runId,
        String conversationId,
        Long userId,
        String username,
        Integer userType) {
}
