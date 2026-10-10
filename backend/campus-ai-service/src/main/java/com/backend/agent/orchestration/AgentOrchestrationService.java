package com.backend.agent.orchestration;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.backend.agent.domain.AgentRole;
import com.backend.agent.domain.AgentRun;
import com.backend.agent.domain.AgentRunEvent;
import com.backend.agent.domain.AgentTaskExecution;
import com.backend.agent.domain.RiskLevel;
import com.backend.agent.domain.RunStatus;
import com.backend.agent.domain.TaskStatus;
import com.backend.agent.repository.AgentRunRepository;
import com.backend.config.AgentRuntimeProperties;
import com.backend.exception.ForbiddenException;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

/**
 * Durable control plane for multi-agent runs: deterministic planning, bounded
 * parallelism, retry/timeout/circuit-breaker isolation, human approval and audit
 * events. LLM workers never own workflow state transitions.
 */
@Service
@Slf4j
public class AgentOrchestrationService {

    private final AgentRunRepository repository;
    private final IntentRouter router;
    private final AgentWorker worker;
    private final AgentCircuitBreaker circuitBreaker;
    private final AgentEventStream eventStream;
    private final AgentRuntimeProperties properties;
    private final ExecutorService executor;
    private final MeterRegistry meterRegistry;
    private final ConcurrentHashMap<String, Object> runLocks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> cancellationSignals = new ConcurrentHashMap<>();

    public AgentOrchestrationService(AgentRunRepository repository,
                                     IntentRouter router,
                                     AgentWorker worker,
                                     AgentCircuitBreaker circuitBreaker,
                                     AgentEventStream eventStream,
                                     AgentRuntimeProperties properties,
                                     @Qualifier("agentOrchestrationExecutor") ExecutorService executor,
                                     MeterRegistry meterRegistry) {
        this.repository = repository;
        this.router = router;
        this.worker = worker;
        this.circuitBreaker = circuitBreaker;
        this.eventStream = eventStream;
        this.properties = properties;
        this.executor = executor;
        this.meterRegistry = meterRegistry;
    }

    public AgentRun start(String message,
                          String conversationId,
                          String idempotencyKey,
                          Long userId,
                          String username,
                          Integer userType) {
        if (userId == null) {
            throw new ForbiddenException("登录后才能启动 Agent 编排");
        }
        if (!StringUtils.hasText(message)) {
            throw new IllegalArgumentException("message 不能为空");
        }
        String safeMessage = message.trim();
        if (safeMessage.length() > 4_000) {
            throw new IllegalArgumentException("message 不能超过 4000 字符");
        }
        String safeIdempotencyKey = normalizeIdempotencyKey(idempotencyKey);
        Optional<AgentRun> existing = repository.findByUserIdAndIdempotencyKey(userId, safeIdempotencyKey);
        if (existing.isPresent()) {
            return existing.get();
        }

        AgentPlan plan = router.plan(safeMessage);
        Instant now = Instant.now();
        List<AgentTaskExecution> tasks = plan.tasks().stream()
                .map(this::toExecution)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        tasks.add(AgentTaskExecution.builder()
                .taskId("task-review")
                .role(AgentRole.QUALITY_REVIEWER)
                .objective("汇总并审校专职 Agent 证据")
                .riskLevel(RiskLevel.READ_ONLY)
                .dependsOn(plan.tasks().stream().map(AgentPlan.PlannedTask::taskId).toList())
                .status(TaskStatus.PENDING)
                .build());

        AgentRun run = AgentRun.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .username(username)
                .userType(userType)
                .conversationId(StringUtils.hasText(conversationId) ? conversationId.trim() : UUID.randomUUID().toString())
                .idempotencyKey(safeIdempotencyKey)
                .query(safeMessage)
                .intent(plan.intent())
                .status(RunStatus.QUEUED)
                .approvalRequired(plan.approvalRequired())
                .approvalReason(plan.approvalReason())
                .tasks(tasks)
                .createdAt(now)
                .updatedAt(now)
                .build();

        AgentRunEvent created = appendEvent(run, "RUN_CREATED", null, null,
                "编排运行已创建", Map.of(
                        "intent", plan.intent(),
                        "roles", plan.tasks().stream().map(task -> task.role().name()).toList(),
                        "approvalRequired", plan.approvalRequired()));
        try {
            run = repository.save(run);
        } catch (DataIntegrityViolationException duplicate) {
            return repository.findByUserIdAndIdempotencyKey(userId, safeIdempotencyKey).orElseThrow(() -> duplicate);
        }

        eventStream.open(run.getId());
        eventStream.publish(created);
        runLocks.putIfAbsent(run.getId(), new Object());
        cancellationSignals.put(run.getId(), false);
        Counter.builder("campusclub.agent.runs")
                .description("Multi-agent orchestration runs")
                .tag("event", "started")
                .register(meterRegistry)
                .increment();
        String runId = run.getId();
        CompletableFuture.runAsync(() -> executeInitialPhase(runId), executor);
        return run;
    }

    /**
     * Replays interrupted read-only work after a process restart. A transaction
     * task found in RUNNING state is deliberately not replayed because its commit
     * outcome is ambiguous and requires reconciliation.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void recoverInterruptedRuns() {
        List<AgentRun> interrupted;
        try {
            interrupted = repository.findByStatusIn(List.of(RunStatus.QUEUED, RunStatus.RUNNING));
        } catch (Exception unavailable) {
            return;
        }
        for (AgentRun run : interrupted) {
            Object lock = lockFor(run.getId());
            synchronized (lock) {
                cancellationSignals.put(run.getId(), false);
                eventStream.open(run.getId());
                Optional<AgentTaskExecution> transaction = run.getTasks().stream()
                        .filter(task -> task.getRole() == AgentRole.TRANSACTION_EXECUTOR)
                        .findFirst();
                if (transaction.isPresent() && (transaction.get().getStatus() == TaskStatus.RUNNING
                        || transaction.get().getStatus() == TaskStatus.TIMED_OUT)) {
                    run.setStatus(RunStatus.FAILED);
                    run.setError("服务重启时事务执行结果不确定，已停止自动重放，请人工核对报名记录");
                    run.setFinalAnswer(run.getError() + "。运行 ID：" + run.getId());
                    run.setCompletedAt(Instant.now());
                    AgentRunEvent event = appendEvent(run, "RECOVERY_REQUIRES_RECONCILIATION", transaction.get().getTaskId(),
                            AgentRole.TRANSACTION_EXECUTOR, run.getError(), Map.of("answer", run.getFinalAnswer()));
                    repository.save(run);
                    eventStream.publish(event);
                    eventStream.complete(run.getId());
                    cleanupTerminalState(run.getId());
                    continue;
                }
                run.getTasks().stream()
                        .filter(task -> task.getStatus() == TaskStatus.RUNNING)
                        .forEach(task -> task.setStatus(TaskStatus.PENDING));
                AgentRunEvent event = appendEvent(run, "RUN_RECOVERED", null, null,
                        "服务重启后恢复安全的未完成任务", Map.of());
                repository.save(run);
                eventStream.publish(event);
                if (transaction.isPresent()) {
                    CompletableFuture.runAsync(() -> executeApprovedPhase(run.getId()), executor);
                } else {
                    CompletableFuture.runAsync(() -> executeInitialPhase(run.getId()), executor);
                }
            }
        }
    }

    public AgentRun getOwnedRun(String runId, Long userId) {
        return repository.findByIdAndUserId(runId, userId)
                .orElseThrow(() -> new ForbiddenException("运行不存在或无权访问"));
    }

    public List<AgentRun> listOwnedRuns(Long userId) {
        return repository.findTop20ByUserIdOrderByCreatedAtDesc(userId);
    }

    public Flux<AgentRunEvent> streamOwnedEvents(String runId, Long userId, long afterSequence) {
        AgentRun run = getOwnedRun(runId, userId);
        List<AgentRunEvent> snapshot = run.getEvents().stream()
                .filter(event -> event.getSequence() > afterSequence)
                .toList();
        long baseline = snapshot.stream()
                .mapToLong(AgentRunEvent::getSequence)
                .max()
                .orElse(afterSequence);
        Flux<AgentRunEvent> persisted = Flux.fromIterable(snapshot);
        if (run.getStatus().isTerminal()) {
            return persisted;
        }
        return Flux.concat(persisted, eventStream.live(runId, baseline));
    }

    public AgentRun decideApproval(String runId, Long userId, boolean approved) {
        Object lock = lockFor(runId);
        AgentRun result;
        AgentRunEvent event;
        synchronized (lock) {
            AgentRun run = getOwnedRun(runId, userId);
            if (run.getStatus() != RunStatus.WAITING_APPROVAL) {
                if (run.getStatus().isTerminal()) {
                    return run;
                }
                throw new IllegalStateException("当前运行不处于待审批状态");
            }
            if (!approved) {
                cancellationSignals.put(runId, true);
                run.setStatus(RunStatus.CANCELLED);
                run.setFinalAnswer("已按你的选择取消本次写操作，没有修改报名数据。");
                run.setCompletedAt(Instant.now());
                event = appendEvent(run, "RUN_CANCELLED", null, null,
                        "用户拒绝了写操作审批", Map.of("answer", run.getFinalAnswer()));
                result = repository.save(run);
            } else {
                run.setApprovedAt(Instant.now());
                run.setApprovedBy(userId);
                run.setStatus(RunStatus.RUNNING);
                AgentTaskExecution transaction = AgentTaskExecution.builder()
                        .taskId("task-transaction")
                        .role(AgentRole.TRANSACTION_EXECUTOR)
                        .objective("执行用户已批准的报名数据变更")
                        .riskLevel(RiskLevel.WRITE_REQUIRES_APPROVAL)
                        .dependsOn(run.getTasks().stream()
                                .filter(task -> task.getRole() != AgentRole.QUALITY_REVIEWER)
                                .map(AgentTaskExecution::getTaskId)
                                .toList())
                        .status(TaskStatus.PENDING)
                        .build();
                AgentTaskExecution finalReview = AgentTaskExecution.builder()
                        .taskId("task-final-review")
                        .role(AgentRole.QUALITY_REVIEWER)
                        .objective("校验事务回执并生成最终答复")
                        .riskLevel(RiskLevel.READ_ONLY)
                        .dependsOn(List.of(transaction.getTaskId()))
                        .status(TaskStatus.PENDING)
                        .build();
                run.getTasks().add(transaction);
                run.getTasks().add(finalReview);
                event = appendEvent(run, "APPROVAL_GRANTED", null, null,
                        "写操作已由当前用户批准", Map.of("approvedBy", userId));
                result = repository.save(run);
                cancellationSignals.put(runId, false);
            }
        }
        eventStream.publish(event);
        if (!approved) {
            eventStream.complete(runId);
            cleanupTerminalState(runId);
            markRunMetric(RunStatus.CANCELLED);
        } else {
            CompletableFuture.runAsync(() -> executeApprovedPhase(runId), executor);
        }
        return result;
    }

    public AgentRun cancel(String runId, Long userId) {
        Object lock = lockFor(runId);
        AgentRun result;
        AgentRunEvent event;
        synchronized (lock) {
            AgentRun run = getOwnedRun(runId, userId);
            if (run.getStatus().isTerminal()) {
                return run;
            }
            cancellationSignals.put(runId, true);
            run.setStatus(RunStatus.CANCELLED);
            run.setFinalAnswer("运行已取消；尚未执行的 Agent 任务不会继续提交结果。");
            run.setCompletedAt(Instant.now());
            event = appendEvent(run, "RUN_CANCELLED", null, null,
                    "用户取消了编排运行", Map.of("answer", run.getFinalAnswer()));
            result = repository.save(run);
        }
        eventStream.publish(event);
        eventStream.complete(runId);
        cleanupTerminalState(runId);
        markRunMetric(RunStatus.CANCELLED);
        return result;
    }

    private void executeInitialPhase(String runId) {
        AgentRun run = repository.findById(runId).orElse(null);
        if (run == null || isCancellationRequested(runId)) {
            return;
        }
        try {
            transitionToRunning(run);
            AgentExecutionContext context = contextOf(run);
            List<AgentTaskExecution> specialists = run.getTasks().stream()
                    .filter(task -> task.getRole() != AgentRole.QUALITY_REVIEWER)
                    .filter(task -> task.getStatus() != TaskStatus.COMPLETED)
                    .toList();
            List<CompletableFuture<Void>> futures = specialists.stream()
                    .map(task -> CompletableFuture.runAsync(
                            () -> executeAndPersist(run, task, List.of(), context, false), executor))
                    .toList();
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
            if (isCancellationRequested(runId)) {
                return;
            }

            AgentTaskExecution reviewTask = findTask(run, "task-review");
            if (reviewTask.getStatus() != TaskStatus.COMPLETED) {
                executeReviewAndPersist(run, reviewTask, evidenceTasks(run), context, false);
            }
            String answer = successfulOutput(reviewTask).orElseGet(() -> fallbackAnswer(evidenceTasks(run)));
            if (run.isApprovalRequired()) {
                waitForApproval(run, answer);
            } else {
                finish(run, answer, hasTaskFailures(run) ? RunStatus.PARTIALLY_COMPLETED : RunStatus.COMPLETED);
            }
        } catch (Exception exception) {
            failRun(run, exception);
        }
    }

    private void executeApprovedPhase(String runId) {
        AgentRun run = repository.findById(runId).orElse(null);
        if (run == null || isCancellationRequested(runId)) {
            return;
        }
        try {
            AgentExecutionContext context = contextOf(run);
            AgentTaskExecution transaction = findTask(run, "task-transaction");
            if (transaction.getStatus() != TaskStatus.COMPLETED) {
                executeAndPersist(run, transaction, evidenceTasks(run), context, true);
            }
            if (isCancellationRequested(runId)) {
                return;
            }
            if (transaction.getStatus() != TaskStatus.COMPLETED) {
                finish(run, "操作结果待核对。请查询原操作标识 agent:" + run.getId() + "，确认报名记录后再决定下一步。", RunStatus.PARTIALLY_COMPLETED);
                return;
            }
            AgentTaskExecution finalReview = findTask(run, "task-final-review");
            if (finalReview.getStatus() != TaskStatus.COMPLETED) {
                executeReviewAndPersist(run, finalReview, evidenceTasks(run), context, true);
            }
            String answer = successfulOutput(finalReview).orElseGet(() -> fallbackAnswer(evidenceTasks(run)));
            RunStatus status = transaction.getStatus() == TaskStatus.COMPLETED && finalReview.getStatus() == TaskStatus.COMPLETED
                    ? RunStatus.COMPLETED
                    : RunStatus.PARTIALLY_COMPLETED;
            finish(run, answer, status);
        } catch (Exception exception) {
            failRun(run, exception);
        }
    }

    private void transitionToRunning(AgentRun run) {
        AgentRunEvent event;
        synchronized (lockFor(run.getId())) {
            if (isCancellationRequested(run.getId())) {
                return;
            }
            run.setStatus(RunStatus.RUNNING);
            event = appendEvent(run, "PLAN_STARTED", null, null,
                    "专职 Agent 将按任务图并行执行", Map.of("taskCount", run.getTasks().size() - 1));
            repository.save(run);
        }
        eventStream.publish(event);
    }

    private void executeAndPersist(AgentRun run,
                                   AgentTaskExecution task,
                                   List<AgentTaskExecution> evidence,
                                   AgentExecutionContext context,
                                   boolean writeApproved) {
        if (isCancellationRequested(run.getId())) {
            return;
        }
        markTaskStarted(run, task);
        TaskResult result = invokeBounded(task.getRole(),
                () -> worker.execute(task.getRole(), run.getQuery(), evidence, context, writeApproved));
        applyTaskResult(run, task, result);
    }

    private void executeReviewAndPersist(AgentRun run,
                                         AgentTaskExecution task,
                                         List<AgentTaskExecution> evidence,
                                         AgentExecutionContext context,
                                         boolean mutationExecuted) {
        if (isCancellationRequested(run.getId())) {
            return;
        }
        markTaskStarted(run, task);
        TaskResult result = invokeBounded(AgentRole.QUALITY_REVIEWER,
                () -> worker.review(run.getQuery(), evidence, context, mutationExecuted));
        applyTaskResult(run, task, result);
    }

    private TaskResult invokeBounded(AgentRole role, Supplier<String> invocation) {
        Instant started = Instant.now();
        String lastError = null;
        TaskStatus lastStatus = TaskStatus.FAILED;
        int attempts = 0;
        int allowedAttempts = role == AgentRole.TRANSACTION_EXECUTOR ? 1 : properties.getMaxAttempts();
        for (int attempt = 1; attempt <= allowedAttempts; attempt++) {
            attempts = attempt;
            Future<String> future = null;
            try {
                circuitBreaker.assertCallAllowed(role);
                future = executor.submit(io.micrometer.context.ContextSnapshot.captureAll().wrap(invocation::get));
                String output = future.get(properties.getTaskTimeoutSeconds(), TimeUnit.SECONDS);
                circuitBreaker.recordSuccess(role);
                return new TaskResult(TaskStatus.COMPLETED, attempts, output, null,
                        Duration.between(started, Instant.now()).toMillis());
            } catch (TimeoutException timeout) {
                if (future != null) {
                    future.cancel(true);
                }
                circuitBreaker.recordFailure(role);
                lastStatus = TaskStatus.TIMED_OUT;
                lastError = "Agent 执行超过 " + properties.getTaskTimeoutSeconds() + " 秒";
                break;
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                lastError = "Agent 执行被中断";
                break;
            } catch (ExecutionException execution) {
                circuitBreaker.recordFailure(role);
                Throwable cause = execution.getCause() != null ? execution.getCause() : execution;
                log.warn("Agent invocation failed: role={}, attempt={}, type={}",
                        role, attempt, cause.getClass().getSimpleName());
                lastError = safeError(cause);
            } catch (RuntimeException exception) {
                lastError = safeError(exception);
                if (!(exception instanceof AgentCircuitBreaker.AgentCircuitOpenException)) {
                    circuitBreaker.recordFailure(role);
                }
                break;
            }
        }
        return new TaskResult(lastStatus, attempts, null, lastError,
                Duration.between(started, Instant.now()).toMillis());
    }

    private void markTaskStarted(AgentRun run, AgentTaskExecution task) {
        AgentRunEvent event;
        synchronized (lockFor(run.getId())) {
            if (isCancellationRequested(run.getId())) {
                return;
            }
            task.setStatus(TaskStatus.RUNNING);
            task.setStartedAt(Instant.now());
            event = appendEvent(run, "TASK_STARTED", task.getTaskId(), task.getRole(),
                    task.getRole().getDisplayName() + "开始执行", Map.of("objective", task.getObjective()));
            repository.save(run);
        }
        eventStream.publish(event);
    }

    private void applyTaskResult(AgentRun run, AgentTaskExecution task, TaskResult result) {
        if (isCancellationRequested(run.getId())) {
            return;
        }
        AgentRunEvent event;
        synchronized (lockFor(run.getId())) {
            if (isCancellationRequested(run.getId())) {
                return;
            }
            task.setStatus(result.status());
            task.setAttempts(result.attempts());
            task.setOutput(result.output());
            task.setError(result.error());
            task.setFinishedAt(Instant.now());
            task.setLatencyMs(result.latencyMs());
            String eventType = result.status() == TaskStatus.COMPLETED ? "TASK_COMPLETED" : "TASK_FAILED";
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("status", result.status().name());
            payload.put("attempts", result.attempts());
            payload.put("latencyMs", result.latencyMs());
            if (StringUtils.hasText(result.error())) {
                payload.put("error", result.error());
            }
            event = appendEvent(run, eventType, task.getTaskId(), task.getRole(),
                    result.status() == TaskStatus.COMPLETED
                            ? task.getRole().getDisplayName() + "执行完成"
                            : task.getRole().getDisplayName() + "降级失败",
                    payload);
            repository.save(run);
        }
        eventStream.publish(event);
        Timer.builder("campusclub.agent.task.duration")
                .description("Agent task execution latency")
                .tag("role", task.getRole().name())
                .tag("status", result.status().name())
                .register(meterRegistry)
                .record(Duration.ofMillis(result.latencyMs()));
    }

    private void waitForApproval(AgentRun run, String draftAnswer) {
        AgentRunEvent event;
        synchronized (lockFor(run.getId())) {
            if (isCancellationRequested(run.getId())) {
                return;
            }
            run.setDraftAnswer(draftAnswer);
            run.setStatus(RunStatus.WAITING_APPROVAL);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("reason", run.getApprovalReason());
            payload.put("draftAnswer", draftAnswer);
            event = appendEvent(run, "APPROVAL_REQUIRED", null, AgentRole.TRANSACTION_EXECUTOR,
                    "需要当前用户批准后才能执行写操作", payload);
            repository.save(run);
        }
        eventStream.publish(event);
    }

    private void finish(AgentRun run, String answer, RunStatus status) {
        if (isCancellationRequested(run.getId())) {
            return;
        }
        AgentRunEvent event;
        synchronized (lockFor(run.getId())) {
            if (isCancellationRequested(run.getId())) {
                return;
            }
            run.setStatus(status);
            run.setFinalAnswer(answer);
            run.setCompletedAt(Instant.now());
            event = appendEvent(run, "RUN_COMPLETED", null, AgentRole.QUALITY_REVIEWER,
                    status == RunStatus.COMPLETED ? "编排运行完成" : "编排运行降级完成",
                    Map.of("answer", answer, "status", status.name()));
            repository.save(run);
        }
        eventStream.publish(event);
        eventStream.complete(run.getId());
        cleanupTerminalState(run.getId());
        markRunMetric(status);
    }

    private void failRun(AgentRun run, Exception exception) {
        if (run == null || isCancellationRequested(run.getId())) {
            return;
        }
        AgentRunEvent event;
        synchronized (lockFor(run.getId())) {
            run.setStatus(RunStatus.FAILED);
            run.setError(safeError(exception));
            run.setFinalAnswer("本次 Agent 编排未能完成，请稍后重试。运行 ID：" + run.getId());
            run.setCompletedAt(Instant.now());
            event = appendEvent(run, "RUN_FAILED", null, null,
                    "编排运行失败", Map.of("error", run.getError(), "answer", run.getFinalAnswer()));
            repository.save(run);
        }
        eventStream.publish(event);
        eventStream.complete(run.getId());
        cleanupTerminalState(run.getId());
        markRunMetric(RunStatus.FAILED);
    }

    private AgentRunEvent appendEvent(AgentRun run,
                                      String type,
                                      String taskId,
                                      AgentRole role,
                                      String message,
                                      Map<String, Object> payload) {
        long sequence = run.getNextEventSequence() + 1;
        run.setNextEventSequence(sequence);
        run.setUpdatedAt(Instant.now());
        AgentRunEvent event = AgentRunEvent.builder()
                .sequence(sequence)
                .type(type)
                .runId(run.getId())
                .taskId(taskId)
                .role(role)
                .message(message)
                .payload(payload)
                .occurredAt(Instant.now())
                .build();
        run.getEvents().add(event);
        int overflow = run.getEvents().size() - properties.getMaxRunEvents();
        if (overflow > 0) {
            run.setEvents(new ArrayList<>(run.getEvents().subList(overflow, run.getEvents().size())));
        }
        return event;
    }

    private AgentTaskExecution toExecution(AgentPlan.PlannedTask task) {
        return AgentTaskExecution.builder()
                .taskId(task.taskId())
                .role(task.role())
                .objective(task.objective())
                .riskLevel(task.riskLevel())
                .dependsOn(new ArrayList<>(task.dependsOn()))
                .status(TaskStatus.PENDING)
                .build();
    }

    private AgentExecutionContext contextOf(AgentRun run) {
        return new AgentExecutionContext(run.getId(), run.getConversationId(), run.getUserId(), run.getUsername(), run.getUserType());
    }

    private List<AgentTaskExecution> evidenceTasks(AgentRun run) {
        return run.getTasks().stream()
                .filter(task -> task.getRole() != AgentRole.QUALITY_REVIEWER)
                .toList();
    }

    private AgentTaskExecution findTask(AgentRun run, String taskId) {
        return run.getTasks().stream()
                .filter(task -> Objects.equals(taskId, task.getTaskId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("编排任务不存在：" + taskId));
    }

    private Optional<String> successfulOutput(AgentTaskExecution task) {
        return task.getStatus() == TaskStatus.COMPLETED && StringUtils.hasText(task.getOutput())
                ? Optional.of(task.getOutput())
                : Optional.empty();
    }

    private String fallbackAnswer(List<AgentTaskExecution> evidence) {
        String joined = evidence.stream()
                .filter(task -> task.getStatus() == TaskStatus.COMPLETED && StringUtils.hasText(task.getOutput()))
                .map(task -> task.getOutput())
                .limit(3)
                .reduce((left, right) -> left + "\n\n" + right)
                .orElse("");
        if (StringUtils.hasText(joined)) {
            return joined;
        }
        return "相关专职 Agent 暂时都无法返回可靠结果，请稍后重试。";
    }

    private boolean hasTaskFailures(AgentRun run) {
        return run.getTasks().stream().anyMatch(task -> task.getStatus() == TaskStatus.FAILED
                || task.getStatus() == TaskStatus.TIMED_OUT);
    }

    private boolean isCancellationRequested(String runId) {
        Boolean signal = cancellationSignals.get(runId);
        if (signal != null) {
            return signal;
        }
        try {
            return repository.findById(runId).map(run -> run.getStatus().isTerminal()).orElse(true);
        } catch (Exception unavailable) {
            return false;
        }
    }

    private void cleanupTerminalState(String runId) {
        cancellationSignals.remove(runId);
        runLocks.remove(runId);
    }

    private Object lockFor(String runId) {
        return runLocks.computeIfAbsent(runId, ignored -> new Object());
    }

    private String normalizeIdempotencyKey(String value) {
        String key = StringUtils.hasText(value) ? value.trim() : UUID.randomUUID().toString();
        if (key.length() > 128) {
            throw new IllegalArgumentException("Idempotency-Key 不能超过 128 字符");
        }
        return key;
    }

    private String safeError(Throwable throwable) {
        String message = throwable.getMessage();
        if (!StringUtils.hasText(message)) {
            message = throwable.getClass().getSimpleName();
        }
        String redacted = message
                .replaceAll("(?i)(api[-_ ]?key|authorization|token|secret)\\s*[:=]\\s*[^\\s,;]+", "$1=[REDACTED]")
                .replaceAll("(?i)sk-[a-z0-9_-]{10,}", "[REDACTED]");
        return redacted.length() > 500 ? redacted.substring(0, 500) : redacted;
    }

    private void markRunMetric(RunStatus status) {
        Counter.builder("campusclub.agent.runs")
                .description("Multi-agent orchestration runs")
                .tag("event", status.name().toLowerCase())
                .register(meterRegistry)
                .increment();
    }

    private record TaskResult(TaskStatus status, int attempts, String output, String error, long latencyMs) {
    }
}
