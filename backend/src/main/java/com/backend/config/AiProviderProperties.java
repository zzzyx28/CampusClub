package com.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.ai")
public class AiProviderProperties {

    /**
     * Logical provider name used for observability and resume-worthy runtime reporting.
     */
    private String provider = "dashscope";

    private boolean toolCallingEnabled = true;

    private ModelConfig chat = new ModelConfig("qwen-plus");

    private ModelConfig embedding = new ModelConfig("text-embedding-v1");

    private OpenAiCompatibleConfig openaiCompatible = new OpenAiCompatibleConfig();

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public boolean isToolCallingEnabled() {
        return toolCallingEnabled;
    }

    public void setToolCallingEnabled(boolean toolCallingEnabled) {
        this.toolCallingEnabled = toolCallingEnabled;
    }

    public ModelConfig getChat() {
        return chat;
    }

    public void setChat(ModelConfig chat) {
        this.chat = chat;
    }

    public ModelConfig getEmbedding() {
        return embedding;
    }

    public void setEmbedding(ModelConfig embedding) {
        this.embedding = embedding;
    }

    public OpenAiCompatibleConfig getOpenaiCompatible() {
        return openaiCompatible;
    }

    public void setOpenaiCompatible(OpenAiCompatibleConfig openaiCompatible) {
        this.openaiCompatible = openaiCompatible;
    }

    public static class ModelConfig {
        private String model;

        public ModelConfig() {
        }

        public ModelConfig(String model) {
            this.model = model;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }
    }

    public static class OpenAiCompatibleConfig {
        private String baseUrl = "https://llm-xa2inbbuopkt0upv.cn-beijing.maas.aliyuncs.com/compatible-mode/v1";

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }
}
