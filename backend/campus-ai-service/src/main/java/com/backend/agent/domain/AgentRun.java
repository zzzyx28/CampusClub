package com.backend.agent.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document("agent_orchestration_runs")
@CompoundIndex(name = "uk_run_user_idempotency", def = "{'user_id': 1, 'idempotency_key': 1}", unique = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentRun {

    @Id
    private String id;

    @Version
    private Long version;

    @Field("user_id")
    @Indexed
    private Long userId;

    @Field("username")
    private String username;

    @Field("user_type")
    private Integer userType;

    @Field("conversation_id")
    @Indexed
    private String conversationId;

    @Field("idempotency_key")
    private String idempotencyKey;

    private String query;
    private String intent;
    private RunStatus status;

    @Field("approval_required")
    private boolean approvalRequired;

    @Field("approval_reason")
    private String approvalReason;

    @Field("approved_at")
    private Instant approvedAt;

    @Field("approved_by")
    private Long approvedBy;

    @Builder.Default
    private List<AgentTaskExecution> tasks = new ArrayList<>();

    @Builder.Default
    private List<AgentRunEvent> events = new ArrayList<>();

    @Field("next_event_sequence")
    private long nextEventSequence;

    @Field("draft_answer")
    private String draftAnswer;

    @Field("final_answer")
    private String finalAnswer;

    private String error;

    @Field("created_at")
    private Instant createdAt;

    @Field("updated_at")
    private Instant updatedAt;

    @Field("completed_at")
    private Instant completedAt;
}
