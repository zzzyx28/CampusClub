package com.backend.config;

import com.backend.dao.mongo.ConversationRepository;
import com.backend.memory.MongoChatMemory;
import com.backend.service.AgentMemoryService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MongoChatMemoryConfig {

    @Bean
    public MongoChatMemory mongoChatMemory(ConversationRepository conversationRepository,
                                           AgentMemoryService agentMemoryService) {
        return new MongoChatMemory(conversationRepository, agentMemoryService);
    }
}
