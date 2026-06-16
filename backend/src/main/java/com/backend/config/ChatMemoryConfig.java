package com.backend.config;

import com.backend.memory.MongoChatMemory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ChatMemoryConfig {

    @Bean
    @Primary
    @ConditionalOnProperty(name = "app.chat.memory", havingValue = "mongo", matchIfMissing = true)
    public ChatMemory chatMemory(MongoChatMemory mongoChatMemory) {
        return mongoChatMemory;
    }
}
