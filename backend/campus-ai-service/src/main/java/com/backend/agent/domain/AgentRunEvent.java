package com.backend.agent.domain;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentRunEvent {

    private long sequence;
    private String type;
    private String runId;
    private String taskId;
    private AgentRole role;
    private String message;

    @Builder.Default
    private Map<String, Object> payload = new LinkedHashMap<>();

    private Instant occurredAt;
}
