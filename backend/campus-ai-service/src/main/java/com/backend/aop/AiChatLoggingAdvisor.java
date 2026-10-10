package com.backend.aop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.AdvisedRequest;
import org.springframework.ai.chat.client.RequestResponseAdvisor;
import org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor;
import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class AiChatLoggingAdvisor implements RequestResponseAdvisor {

    public static final String RAG_TOP_K_KEY = "campusclub.rag.topK";
    public static final String RAG_SIMILARITY_THRESHOLD_KEY = "campusclub.rag.similarityThreshold";
    public static final String AGENT_MEMORY_CONTEXT_LENGTH_KEY = "campusclub.agent.memoryContextLength";

    private static final String START_NANO_KEY = "campusclub.agent.startNano";
    private static final Logger log = LoggerFactory.getLogger(AiChatLoggingAdvisor.class);

    @Override
    public AdvisedRequest adviseRequest(AdvisedRequest request, Map<String, Object> context) {
        if (context != null) {
            context.put(START_NANO_KEY, System.nanoTime());
        }
        log.info("AI chat request started, conversationId={}, memoryWindow={}, ragTopK={}, ragThreshold={}, memoryContextChars={}",
                value(context, AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY, "unknown"),
                value(context, AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY, "unknown"),
                value(context, RAG_TOP_K_KEY, "unknown"),
                value(context, RAG_SIMILARITY_THRESHOLD_KEY, "unknown"),
                value(context, AGENT_MEMORY_CONTEXT_LENGTH_KEY, "unknown"));
        return request;
    }

    @Override
    public ChatResponse adviseResponse(ChatResponse response, Map<String, Object> context) {
        logCompletion(context, "sync");
        return response;
    }

    @Override
    public Flux<ChatResponse> adviseResponse(Flux<ChatResponse> fluxResponse, Map<String, Object> context) {
        return fluxResponse.doFinally(signalType -> logCompletion(context, "stream:" + signalType.name()));
    }

    private void logCompletion(Map<String, Object> context, String mode) {
        long elapsedMs = -1;
        Object start = context != null ? context.get(START_NANO_KEY) : null;
        if (start instanceof Long startNano) {
            elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNano);
        }
        log.info("AI chat request finished, conversationId={}, mode={}, elapsedMs={}",
                value(context, AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY, "unknown"),
                mode,
                elapsedMs);
    }

    private Object value(Map<String, Object> context, String key, Object fallback) {
        if (context == null) {
            return fallback;
        }
        Object value = context.get(key);
        return value != null ? value : fallback;
    }
}
