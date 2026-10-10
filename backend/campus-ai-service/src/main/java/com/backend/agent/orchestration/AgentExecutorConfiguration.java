package com.backend.agent.orchestration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgentExecutorConfiguration {

    @Bean(name = "agentOrchestrationExecutor", destroyMethod = "close")
    public ExecutorService agentOrchestrationExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
