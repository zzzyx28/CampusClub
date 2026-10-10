package com.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.agent")
public class AgentRuntimeProperties {

    /**
     * Number of recent turns replayed by Spring AI ChatMemory advisor.
     */
    private int memoryWindow = 24;

    /**
     * Number of RAG chunks retrieved from the VectorStore.
     */
    private int ragTopK = 5;

    /**
     * Minimum similarity score accepted by the RAG advisor.
     */
    private double ragSimilarityThreshold = 0.2;

    /**
     * Hard deadline for one specialist invocation. Slow agents degrade independently
     * instead of blocking the whole workflow.
     */
    private int taskTimeoutSeconds = 25;

    /**
     * Total attempts for a transiently failed specialist invocation.
     */
    private int maxAttempts = 2;

    /**
     * Maximum number of orchestration events retained in a run document.
     */
    private int maxRunEvents = 200;

    /**
     * Consecutive failures before a specialist circuit is opened temporarily.
     */
    private int circuitBreakerThreshold = 3;

    /**
     * How long an open specialist circuit rejects new work.
     */
    private int circuitBreakerOpenSeconds = 30;

    public int getMemoryWindow() {
        return memoryWindow;
    }

    public void setMemoryWindow(int memoryWindow) {
        this.memoryWindow = Math.max(1, memoryWindow);
    }

    public int getRagTopK() {
        return ragTopK;
    }

    public void setRagTopK(int ragTopK) {
        this.ragTopK = Math.max(1, ragTopK);
    }

    public double getRagSimilarityThreshold() {
        return ragSimilarityThreshold;
    }

    public void setRagSimilarityThreshold(double ragSimilarityThreshold) {
        this.ragSimilarityThreshold = Math.max(0.0, Math.min(1.0, ragSimilarityThreshold));
    }

    public int getTaskTimeoutSeconds() {
        return taskTimeoutSeconds;
    }

    public void setTaskTimeoutSeconds(int taskTimeoutSeconds) {
        this.taskTimeoutSeconds = Math.max(1, taskTimeoutSeconds);
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = Math.max(1, Math.min(5, maxAttempts));
    }

    public int getMaxRunEvents() {
        return maxRunEvents;
    }

    public void setMaxRunEvents(int maxRunEvents) {
        this.maxRunEvents = Math.max(20, maxRunEvents);
    }

    public int getCircuitBreakerThreshold() {
        return circuitBreakerThreshold;
    }

    public void setCircuitBreakerThreshold(int circuitBreakerThreshold) {
        this.circuitBreakerThreshold = Math.max(1, circuitBreakerThreshold);
    }

    public int getCircuitBreakerOpenSeconds() {
        return circuitBreakerOpenSeconds;
    }

    public void setCircuitBreakerOpenSeconds(int circuitBreakerOpenSeconds) {
        this.circuitBreakerOpenSeconds = Math.max(1, circuitBreakerOpenSeconds);
    }
}
