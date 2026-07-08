package com.backend.config;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AgentRuntimeInfoContributor implements InfoContributor {

    private static final int TOOL_COUNT = 11;

    private final AgentRuntimeProperties properties;
    private final AiProviderProperties aiProviderProperties;

    public AgentRuntimeInfoContributor(AgentRuntimeProperties properties,
                                       AiProviderProperties aiProviderProperties) {
        this.properties = properties;
        this.aiProviderProperties = aiProviderProperties;
    }

    @Override
    public void contribute(Info.Builder builder) {
        builder.withDetail("agentRuntime", Map.of(
                "framework", "Spring AI ChatClient + Advisors",
                "provider", Map.of(
                        "name", aiProviderProperties.getProvider(),
                        "chatModel", aiProviderProperties.getChat().getModel(),
                        "embeddingModel", aiProviderProperties.getEmbedding().getModel(),
                        "toolCallingEnabled", aiProviderProperties.isToolCallingEnabled(),
                        "openAiCompatibleBaseUrl", aiProviderProperties.getOpenaiCompatible().getBaseUrl()),
                "memory", Map.of(
                        "shortTerm", "PromptChatMemoryAdvisor",
                        "longTerm", "MongoDB agent_memory",
                        "window", properties.getMemoryWindow()),
                "rag", Map.of(
                        "advisor", "QuestionAnswerAdvisor",
                        "vectorStore", "Redis Vector Store",
                        "topK", properties.getRagTopK(),
                        "similarityThreshold", properties.getRagSimilarityThreshold()),
                "toolCalling", Map.of(
                        "registeredTools", TOOL_COUNT,
                        "businessBoundary", "Spring beans wrapping Activity, Registration, Dashboard and User services")));
    }
}
