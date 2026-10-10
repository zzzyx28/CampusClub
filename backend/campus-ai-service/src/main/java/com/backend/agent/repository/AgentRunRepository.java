package com.backend.agent.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.backend.agent.domain.AgentRun;
import com.backend.agent.domain.RunStatus;

@Repository
public interface AgentRunRepository extends MongoRepository<AgentRun, String> {

    Optional<AgentRun> findByIdAndUserId(String id, Long userId);

    Optional<AgentRun> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

    List<AgentRun> findTop20ByUserIdOrderByCreatedAtDesc(Long userId);

    List<AgentRun> findByStatusIn(List<RunStatus> statuses);
}
