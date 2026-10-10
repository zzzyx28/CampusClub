package com.backend.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.backend.agent.domain.AgentRun;
import com.backend.agent.domain.RunStatus;
import com.backend.agent.orchestration.AgentCircuitBreaker;
import com.backend.agent.orchestration.AgentEventStream;
import com.backend.agent.orchestration.AgentExecutionContext;
import com.backend.agent.orchestration.AgentOrchestrationService;
import com.backend.agent.orchestration.AgentWorker;
import com.backend.agent.orchestration.IntentRouter;
import com.backend.agent.repository.AgentRunRepository;
import com.backend.config.AgentRuntimeProperties;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class AgentOrchestrationServiceTest {

    private final Map<String, AgentRun> store = new ConcurrentHashMap<>();
    private ExecutorService executor;
    private AgentOrchestrationService service;

    @BeforeEach
    void setUp() {
        AgentRunRepository repository = mock(AgentRunRepository.class);
        when(repository.save(any(AgentRun.class))).thenAnswer(invocation -> {
            AgentRun run = invocation.getArgument(0);
            store.put(run.getId(), run);
            return run;
        });
        when(repository.findById(any(String.class)))
                .thenAnswer(invocation -> Optional.ofNullable(store.get(invocation.getArgument(0))));
        when(repository.findByIdAndUserId(any(String.class), any(Long.class)))
                .thenAnswer(invocation -> Optional.ofNullable(store.get(invocation.getArgument(0)))
                        .filter(run -> run.getUserId().equals(invocation.getArgument(1))));
        when(repository.findByUserIdAndIdempotencyKey(any(Long.class), any(String.class)))
                .thenAnswer(invocation -> store.values().stream()
                        .filter(run -> run.getUserId().equals(invocation.getArgument(0)))
                        .filter(run -> run.getIdempotencyKey().equals(invocation.getArgument(1)))
                        .findFirst());

        AgentWorker worker = mock(AgentWorker.class);
        when(worker.execute(any(), any(), any(), any(AgentExecutionContext.class), any(Boolean.class)))
                .thenAnswer(invocation -> "已核验：" + invocation.getArgument(0));
        when(worker.review(any(), any(), any(AgentExecutionContext.class), any(Boolean.class)))
                .thenReturn("这是基于专职 Agent 证据生成的答复");

        AgentRuntimeProperties properties = new AgentRuntimeProperties();
        properties.setTaskTimeoutSeconds(2);
        properties.setMaxAttempts(1);
        executor = Executors.newVirtualThreadPerTaskExecutor();
        service = new AgentOrchestrationService(
                repository,
                new IntentRouter(),
                worker,
                new AgentCircuitBreaker(properties),
                new AgentEventStream(properties),
                properties,
                executor,
                new SimpleMeterRegistry());
    }

    @AfterEach
    void tearDown() {
        executor.close();
    }

    @Test
    void readOnlyRunExecutesSpecialistsAndReviewerToCompletion() {
        AgentRun run = service.start("最近有什么讲座？", "conv-1", "idem-read", 7L, "alice", 1);

        awaitStatus(run, RunStatus.COMPLETED);

        assertEquals("这是基于专职 Agent 证据生成的答复", run.getFinalAnswer());
        assertTrue(run.getTasks().size() >= 2);
        assertTrue(run.getEvents().stream().anyMatch(event -> event.getType().equals("RUN_COMPLETED")));
    }

    @Test
    void mutationRunWaitsForApprovalBeforeTransactionAgent() {
        AgentRun run = service.start("帮我报名算法分享会", "conv-2", "idem-write", 7L, "alice", 1);

        awaitStatus(run, RunStatus.WAITING_APPROVAL);
        assertTrue(run.isApprovalRequired());
        assertTrue(run.getTasks().stream().noneMatch(task -> "task-transaction".equals(task.getTaskId())));

        service.decideApproval(run.getId(), 7L, true);
        awaitStatus(run, RunStatus.COMPLETED);

        assertNotNull(run.getApprovedAt());
        assertTrue(run.getTasks().stream().anyMatch(task -> "task-transaction".equals(task.getTaskId())));
    }

    @Test
    void duplicateIdempotencyKeyReturnsOriginalRun() {
        AgentRun original = service.start("最近有什么活动？", "conv-3", "same-key", 7L, "alice", 1);
        AgentRun duplicate = service.start("这条内容不会创建新运行", "conv-3", "same-key", 7L, "alice", 1);

        assertSame(original, duplicate);
        assertEquals(1, store.size());
        awaitStatus(original, RunStatus.COMPLETED);
    }

    @Test
    void rejectedApprovalNeverCreatesTransactionTask() {
        AgentRun run = service.start("我要取消报名", "conv-4", "idem-reject", 7L, "alice", 1);
        awaitStatus(run, RunStatus.WAITING_APPROVAL);

        service.decideApproval(run.getId(), 7L, false);

        assertEquals(RunStatus.CANCELLED, run.getStatus());
        assertTrue(run.getTasks().stream().noneMatch(task -> "task-transaction".equals(task.getTaskId())));
    }

    private void awaitStatus(AgentRun run, RunStatus expected) {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(5));
        while (run.getStatus() != expected && Instant.now().isBefore(deadline)) {
            Thread.onSpinWait();
        }
        assertEquals(expected, run.getStatus());
    }
}
