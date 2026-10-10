package com.backend.agent.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentTaskExecution {

    private String taskId;
    private AgentRole role;
    private String objective;
    private RiskLevel riskLevel;

    @Builder.Default
    private List<String> dependsOn = new ArrayList<>();

    @Builder.Default
    private TaskStatus status = TaskStatus.PENDING;

    private int attempts;
    private Instant startedAt;
    private Instant finishedAt;
    private Long latencyMs;
    private String output;
    private String error;
}
