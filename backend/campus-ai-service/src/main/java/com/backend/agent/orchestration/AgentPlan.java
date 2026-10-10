package com.backend.agent.orchestration;

import java.util.List;

import com.backend.agent.domain.AgentRole;
import com.backend.agent.domain.RiskLevel;

public record AgentPlan(
        String intent,
        List<PlannedTask> tasks,
        boolean approvalRequired,
        String approvalReason) {

    public record PlannedTask(
            String taskId,
            AgentRole role,
            String objective,
            RiskLevel riskLevel,
            List<String> dependsOn) {
    }
}
