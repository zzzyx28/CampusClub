package com.backend.agent.orchestration;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.backend.agent.domain.AgentRunEvent;
import com.backend.config.AgentRuntimeProperties;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class AgentEventStream {

    private final ConcurrentHashMap<String, Sinks.Many<AgentRunEvent>> streams = new ConcurrentHashMap<>();
    private final int replayLimit;

    public AgentEventStream(AgentRuntimeProperties properties) {
        this.replayLimit = properties.getMaxRunEvents();
    }

    public void open(String runId) {
        streams.computeIfAbsent(runId, ignored -> Sinks.many().replay().limit(replayLimit));
    }

    public void publish(AgentRunEvent event) {
        streams.computeIfAbsent(event.getRunId(), ignored -> Sinks.many().replay().limit(replayLimit))
                .tryEmitNext(event);
    }

    public Flux<AgentRunEvent> live(String runId, long afterSequence) {
        return streams.computeIfAbsent(runId, ignored -> Sinks.many().replay().limit(replayLimit))
                .asFlux()
                .filter(event -> event.getSequence() > afterSequence);
    }

    public void complete(String runId) {
        Sinks.Many<AgentRunEvent> sink = streams.remove(runId);
        if (sink != null) {
            sink.tryEmitComplete();
        }
    }
}
