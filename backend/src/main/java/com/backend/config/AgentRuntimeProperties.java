package com.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.agent")
public class AgentRuntimeProperties {

    /**
     * Number of recent turns replayed by Spring AI ChatMemory advisor.
     */
    private int memoryWindow = 24;

    /**
     * Number of RAG chunks retrieved from the VectorStore.
     */
    private int ragTopK = 5;

    /**
     * Minimum similarity score accepted by the RAG advisor.
     */
    private double ragSimilarityThreshold = 0.2;

    public int getMemoryWindow() {
        return memoryWindow;
    }

    public void setMemoryWindow(int memoryWindow) {
        this.memoryWindow = Math.max(1, memoryWindow);
    }

    public int getRagTopK() {
        return ragTopK;
    }

    public void setRagTopK(int ragTopK) {
        this.ragTopK = Math.max(1, ragTopK);
    }

    public double getRagSimilarityThreshold() {
        return ragSimilarityThreshold;
    }

    public void setRagSimilarityThreshold(double ragSimilarityThreshold) {
        this.ragSimilarityThreshold = Math.max(0.0, Math.min(1.0, ragSimilarityThreshold));
    }
}
