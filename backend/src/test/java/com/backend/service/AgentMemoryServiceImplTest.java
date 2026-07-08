package com.backend.service;

import com.backend.dao.mongo.ConversationRepository;
import com.backend.entity.ConversationDocument;
import com.backend.entity.User;
import com.backend.service.impl.AgentMemoryServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentMemoryServiceImplTest {

    private final ConversationRepository conversationRepository = mock(ConversationRepository.class);
    private final UserService userService = mock(UserService.class);
    private final AgentMemoryService service = new AgentMemoryServiceImpl(conversationRepository, userService);

    @Test
    void updateFromMessagesExtractsPreferencesAndSlots() {
        ConversationDocument doc = new ConversationDocument();
        doc.setConversationUuid("conv-1");
        when(conversationRepository.findByConversationUuid("conv-1")).thenReturn(Optional.of(doc));

        service.updateFromMessages("conv-1", java.util.List.of(
                new UserMessage("我是张三，想报名 2026-07-10 在图书馆的计算机协会讲座，要普通票")));

        verify(conversationRepository).save(any(ConversationDocument.class));
        assertThat(doc.getAgentMemory()).isNotNull();
        assertThat(doc.getAgentMemory().getLastIntent()).isEqualTo("报名活动");
        assertThat(doc.getAgentMemory().getSlots())
                .containsEntry("realName", "张三")
                .containsEntry("date", "2026-07-10")
                .containsEntry("tierName", "普通票");
        assertThat(doc.getAgentMemory().getPreferences())
                .containsEntry("club", "计算机协会")
                .containsEntry("activityType", "讲座");
        assertThat(doc.getAgentMemory().getSummary()).contains("报名活动");
    }

    @Test
    void buildMemoryContextMergesUserProfileAndConversationMemory() {
        ConversationDocument.AgentMemory memory = new ConversationDocument.AgentMemory();
        memory.getPreferences().put("activityType", "志愿");
        memory.getSlots().put("realName", "李四");
        memory.setLastIntent("活动查询");
        memory.setSummary("活动查询：想看志愿活动");

        ConversationDocument doc = new ConversationDocument();
        doc.setConversationUuid("conv-2");
        doc.setAgentMemory(memory);

        User user = new User();
        user.setRealName("李四");
        user.setMemberLevel(3);
        user.setEmail("li@example.edu");

        when(conversationRepository.findByConversationUuid("conv-2")).thenReturn(Optional.of(doc));
        when(userService.getById(7L)).thenReturn(user);

        String context = service.buildMemoryContext("conv-2", 7L);

        assertThat(context)
                .contains("登录用户画像")
                .contains("李四")
                .contains("会员等级 3")
                .contains("会话长期摘要")
                .contains("activityType=志愿")
                .contains("realName=李四")
                .contains("上次识别意图：活动查询");
    }
}
