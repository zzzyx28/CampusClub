package com.backend.controller;

import java.time.LocalDate;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.backend.aop.AiChatLoggingAdvisor;
import com.backend.config.SmartShopAgentPrompts;

import reactor.core.publisher.Flux;

/**
 * AI 对话：会话级记忆、向量检索补充知识、工具 JSON 结果由模型解析。
 */
@RestController
@RequestMapping("/api/ai")
public class AiController {

    private static final int MEMORY_WINDOW = 24;

    private final ChatClient chatClient;

    public AiController(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory, VectorStore vectorStore) {
        SearchRequest ragSearch = SearchRequest.defaults()
                .withTopK(5)
                .withSimilarityThreshold(0.2);
        this.chatClient = chatClientBuilder
                .defaultSystem(SmartShopAgentPrompts.SYSTEM_TEMPLATE)
                .defaultAdvisors(
                        new PromptChatMemoryAdvisor(chatMemory),
                        new QuestionAnswerAdvisor(vectorStore, ragSearch),
                        new AiChatLoggingAdvisor())
                .defaultFunctions(
                        "cancelProductOrder",
                        "getProductOrderDetail",
                        "getBuyerProductOrders",
                        "createProductOrder",
                        "getProductBySku",
                        "searchProducts",
                        "getProductsByBrand",
                        "listRecentlyAvailableProducts")
                .build();
    }

    /**
     * @param message 用户输入
     * @param conversationId   与业务会话 UUID 一致，保证多轮与工具上下文连续
     * @param userId           当前登录用户 ID（下单）；未传则须向用户确认身份
     */
    @GetMapping(value = "/generateStreamAsString", produces = MediaType.TEXT_EVENT_STREAM_VALUE + ";charset=UTF-8")
    public Flux<String> generateStreamAsString(
            @RequestParam(value = "message", defaultValue = "你好") String message,
            @RequestParam(value = "conversationId", required = false) String conversationId,
            @RequestParam(value = "userId", required = false) Long userId) {

        String convId = (conversationId != null && !conversationId.isBlank())
                ? conversationId
                : "anonymous";

        String userLabel = userId != null ? String.valueOf(userId) : "未知（请引导用户登录或询问后再下单）";

        Flux<String> content = this.chatClient.prompt()
                .user(message)
                .system(spec -> spec
                        .param("current_date", LocalDate.now().toString())
                        .param("user_id", userLabel)
                        .param("conversation_id", convId))
                .advisors(advisorSpec -> advisorSpec
                        .param(AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY, convId)
                        .param(AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY, MEMORY_WINDOW))
                .stream()
                .content();
        return content.concatWith(Flux.just("[complete]"));
    }
}
