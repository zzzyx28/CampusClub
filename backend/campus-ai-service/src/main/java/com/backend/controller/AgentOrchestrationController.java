package com.backend.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.backend.agent.domain.AgentRun;
import com.backend.agent.domain.AgentRunEvent;
import com.backend.agent.orchestration.AgentOrchestrationService;
import com.backend.common.Result;
import com.backend.util.AuthConstants;
import com.backend.util.AuthContext;

import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/agent-runs")
public class AgentOrchestrationController {

    private final AgentOrchestrationService orchestrationService;

    public AgentOrchestrationController(AgentOrchestrationService orchestrationService) {
        this.orchestrationService = orchestrationService;
    }

    @PostMapping
    public ResponseEntity<Result> start(@RequestBody StartRunRequest request,
                                        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        AgentRun run = orchestrationService.start(
                request.message(),
                request.conversationId(),
                idempotencyKey,
                AuthContext.getUserId(),
                currentUsername(),
                AuthContext.getUserType());
        return ResponseEntity.accepted().body(Result.success("Agent 编排已受理", run));
    }

    @GetMapping("/{runId}")
    public Result get(@PathVariable String runId) {
        return Result.success(orchestrationService.getOwnedRun(runId, AuthContext.getUserId()));
    }

    @GetMapping
    public Result list() {
        List<AgentRun> runs = orchestrationService.listOwnedRuns(AuthContext.getUserId());
        return Result.success(runs);
    }

    @GetMapping(value = "/{runId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<AgentRunEvent>> events(
            @PathVariable String runId,
            @RequestParam(value = "after", defaultValue = "0") long after,
            @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId) {
        long cursor = Math.max(after, parseEventId(lastEventId));
        return orchestrationService.streamOwnedEvents(runId, AuthContext.getUserId(), cursor)
                .map(event -> ServerSentEvent.<AgentRunEvent>builder()
                        .id(String.valueOf(event.getSequence()))
                        .data(event)
                        .build());
    }

    @PostMapping("/{runId}/approval")
    public Result decideApproval(@PathVariable String runId, @RequestBody ApprovalRequest request) {
        return Result.success(orchestrationService.decideApproval(runId, AuthContext.getUserId(), request.approved()));
    }

    @PostMapping("/{runId}/cancel")
    public Result cancel(@PathVariable String runId) {
        return Result.success(orchestrationService.cancel(runId, AuthContext.getUserId()));
    }

    private String currentUsername() {
        var request = AuthContext.currentRequest();
        if (request == null) {
            return null;
        }
        Object username = request.getAttribute(AuthConstants.ATTR_USERNAME);
        return username != null ? username.toString() : null;
    }

    private long parseEventId(String value) {
        if (!StringUtils.hasText(value)) {
            return 0L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    public record StartRunRequest(String message, String conversationId) {
    }

    public record ApprovalRequest(boolean approved) {
    }
}
