package com.backend.memory;

import com.backend.dao.mongo.ConversationRepository;
import com.backend.entity.ConversationDocument;
import com.backend.service.AgentMemoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class MongoChatMemory implements ChatMemory {

    private final ConversationRepository conversationRepository;
    private final AgentMemoryService agentMemoryService;

    @Override
    public void add(String conversationId, List<Message> messages) {
        ConversationDocument conversation = conversationRepository.findByConversationUuid(conversationId)
                .orElseGet(() -> {
                    ConversationDocument doc = new ConversationDocument();
                    doc.setConversationUuid(conversationId);
                    doc.setMessages(new ArrayList<>());
                    ConversationDocument.Metadata metadata = new ConversationDocument.Metadata();
                    metadata.setSessionStart(new Date());
                    doc.setMetadata(metadata);
                    return doc;
                });

        if (conversation.getMessages() == null) {
            conversation.setMessages(new ArrayList<>());
        }

        for (Message message : messages) {
            conversation.getMessages().add(toStoredMessage(message));
        }

        if (conversation.getMetadata() != null) {
            conversation.getMetadata().setSessionEnd(new Date());
        }
        conversationRepository.save(conversation);
        agentMemoryService.updateFromMessages(conversationId, messages);
    }

    @Override
    public List<Message> get(String conversationId, int lastN) {
        Optional<ConversationDocument> optional = conversationRepository.findByConversationUuid(conversationId);
        if (optional.isEmpty()) {
            return Collections.emptyList();
        }

        List<ConversationDocument.Message> stored = optional.get().getMessages();
        if (stored == null || stored.isEmpty()) {
            return Collections.emptyList();
        }

        return stored.stream()
                .skip(Math.max(0, stored.size() - lastN))
                .map(this::toAiMessage)
                .toList();
    }

    @Override
    public void clear(String conversationId) {
        conversationRepository.findByConversationUuid(conversationId).ifPresent(doc -> {
            doc.setMessages(new ArrayList<>());
            if (doc.getMetadata() != null) {
                doc.getMetadata().setSessionEnd(new Date());
            }
            conversationRepository.save(doc);
        });
    }

    private ConversationDocument.Message toStoredMessage(Message message) {
        ConversationDocument.Message stored = new ConversationDocument.Message();
        if (message instanceof AssistantMessage) {
            stored.setSpeaker("assistant");
        } else {
            stored.setSpeaker("user");
        }
        stored.setText(message.getContent());
        stored.setTimestamp(new Date());
        return stored;
    }

    private Message toAiMessage(ConversationDocument.Message stored) {
        if ("assistant".equalsIgnoreCase(stored.getSpeaker())) {
            return new AssistantMessage(stored.getText());
        }
        return new UserMessage(stored.getText());
    }
}
