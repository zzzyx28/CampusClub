package com.backend.service;

import org.springframework.ai.chat.messages.Message;

import java.util.List;

public interface AgentMemoryService {

    void updateFromMessages(String conversationId, List<Message> messages);

    String buildMemoryContext(String conversationId, Long userId);
}
