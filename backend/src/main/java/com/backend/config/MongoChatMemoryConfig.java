package com.backend.config;

import com.backend.dao.mongo.ConversationRepository;
import com.backend.memory.MongoChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MongoChatMemoryConfig {

    @Bean
    public MongoChatMemory mongoChatMemory(ConversationRepository conversationRepository) {
        return new MongoChatMemory(conversationRepository);
    }
}
