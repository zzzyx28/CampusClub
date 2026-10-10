package com.backend.agent.orchestration;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.backend.agent.domain.AgentRole;
import com.backend.config.AgentRuntimeProperties;

@Component
public class AgentCircuitBreaker {

    private final ConcurrentHashMap<AgentRole, CircuitState> states = new ConcurrentHashMap<>();
    private final AgentRuntimeProperties properties;

    public AgentCircuitBreaker(AgentRuntimeProperties properties) {
        this.properties = properties;
    }

    public void assertCallAllowed(AgentRole role) {
        CircuitState state = states.get(role);
        if (state == null || state.openedAt == null) {
            return;
        }
        Duration elapsed = Duration.between(state.openedAt, Instant.now());
        if (elapsed.getSeconds() >= properties.getCircuitBreakerOpenSeconds()) {
            states.remove(role, state);
            return;
        }
        throw new AgentCircuitOpenException(role + " circuit is open");
    }

    public void recordSuccess(AgentRole role) {
        states.remove(role);
    }

    public void recordFailure(AgentRole role) {
        states.compute(role, (ignored, previous) -> {
            int failures = previous == null ? 1 : previous.failures + 1;
            Instant openedAt = failures >= properties.getCircuitBreakerThreshold() ? Instant.now() : null;
            return new CircuitState(failures, openedAt);
        });
    }

    private record CircuitState(int failures, Instant openedAt) {
    }

    public static class AgentCircuitOpenException extends RuntimeException {
        public AgentCircuitOpenException(String message) {
            super(message);
        }
    }
}
