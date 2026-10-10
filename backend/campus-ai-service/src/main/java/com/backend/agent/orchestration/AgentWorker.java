package com.backend.agent.orchestration;

import java.util.List;

import com.backend.agent.domain.AgentRole;
import com.backend.agent.domain.AgentTaskExecution;

public interface AgentWorker {

    String execute(AgentRole role,
                   String userQuery,
                   List<AgentTaskExecution> evidence,
                   AgentExecutionContext context,
                   boolean writeApproved);

    String review(String userQuery,
                  List<AgentTaskExecution> evidence,
                  AgentExecutionContext context,
                  boolean mutationExecuted);
}
