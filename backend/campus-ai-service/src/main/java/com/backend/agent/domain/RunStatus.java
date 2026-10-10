package com.backend.agent.domain;

public enum RunStatus {
    QUEUED,
    RUNNING,
    WAITING_APPROVAL,
    COMPLETED,
    PARTIALLY_COMPLETED,
    FAILED,
    CANCELLED;

    public boolean isTerminal() {
        return this == COMPLETED || this == PARTIALLY_COMPLETED || this == FAILED || this == CANCELLED;
    }
}
