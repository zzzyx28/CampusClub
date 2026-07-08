package com.backend.controller;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

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
import com.backend.config.AiProviderProperties;
import com.backend.config.AgentRuntimeProperties;
import com.backend.entity.Activity;
import com.backend.service.AgentMemoryService;
import com.backend.service.ActivityService;
import com.backend.service.SystemPromptProvider;

import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final ChatClient chatClient;
    private final SystemPromptProvider systemPromptProvider;
    private final AgentMemoryService agentMemoryService;
    private final AgentRuntimeProperties agentRuntimeProperties;
    private final AiProviderProperties aiProviderProperties;
    private final ActivityService activityService;

    public AiController(ChatClient.Builder chatClientBuilder,
                        ChatMemory chatMemory,
                        VectorStore vectorStore,
                        SystemPromptProvider systemPromptProvider,
                        AgentMemoryService agentMemoryService,
                        AgentRuntimeProperties agentRuntimeProperties,
                        AiProviderProperties aiProviderProperties,
                        ActivityService activityService) {
        SearchRequest ragSearch = SearchRequest.defaults()
                .withTopK(agentRuntimeProperties.getRagTopK())
                .withSimilarityThreshold(agentRuntimeProperties.getRagSimilarityThreshold());
        this.systemPromptProvider = systemPromptProvider;
        this.agentMemoryService = agentMemoryService;
        this.agentRuntimeProperties = agentRuntimeProperties;
        this.aiProviderProperties = aiProviderProperties;
        this.activityService = activityService;

        ChatClient.Builder builder = chatClientBuilder
                .defaultAdvisors(
                        new PromptChatMemoryAdvisor(chatMemory),
                        new QuestionAnswerAdvisor(vectorStore, ragSearch),
                        new AiChatLoggingAdvisor());
        if (aiProviderProperties.isToolCallingEnabled()) {
            builder.defaultFunctions(
                        "cancelRegistration",
                        "getRegistrationDetail",
                        "getRegistrationsByName",
                        "createRegistration",
                        "getActivityByTicketCode",
                        "searchActivities",
                        "getActivitiesByClub",
                        "listUpcomingActivities",
                        "getDashboardOverview",
                        "getUserProfile",
                        "getActivityStats");
        }
        this.chatClient = builder.build();
    }

    @GetMapping(value = "/generateStreamAsString", produces = MediaType.TEXT_EVENT_STREAM_VALUE + ";charset=UTF-8")
    public Flux<String> generateStreamAsString(
            @RequestParam(value = "message", defaultValue = "你好") String message,
            @RequestParam(value = "conversationId", required = false) String conversationId,
            @RequestParam(value = "userId", required = false) Long userId) {

        String convId = (conversationId != null && !conversationId.isBlank())
                ? conversationId
                : "anonymous";

        String userLabel = userId != null ? String.valueOf(userId) : "未知（请引导用户登录或询问后再报名）";
        String systemPrompt = systemPromptProvider.getSystemPrompt();
        String memoryContext = agentMemoryService.buildMemoryContext(convId, userId);
        String providerContext = buildProviderCompatibilityContext();

        Flux<String> content = this.chatClient.prompt()
                .system(systemPrompt)
                .system("【Agent 记忆上下文】\n" + memoryContext + "\n请把这些信息作为辅助上下文使用；若用户明确更正，以用户最新说法为准。")
                .system(providerContext)
                .user(message)
                .system(spec -> spec
                        .param("current_date", LocalDate.now().toString())
                        .param("user_id", userLabel)
                        .param("conversation_id", convId))
                .advisors(advisorSpec -> advisorSpec
                        .param(AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY, convId)
                        .param(AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY, agentRuntimeProperties.getMemoryWindow())
                        .param(AiChatLoggingAdvisor.RAG_TOP_K_KEY, agentRuntimeProperties.getRagTopK())
                        .param(AiChatLoggingAdvisor.RAG_SIMILARITY_THRESHOLD_KEY, agentRuntimeProperties.getRagSimilarityThreshold())
                        .param(AiChatLoggingAdvisor.AGENT_MEMORY_CONTEXT_LENGTH_KEY, memoryContext.length()))
                .stream()
                .content();
        return content
                .onErrorResume(ex -> Flux.just("\n\n[系统提示] AI 流式响应中断：" + ex.getMessage()))
                .concatWith(Flux.just("[complete]"));
    }

    private String buildProviderCompatibilityContext() {
        if (aiProviderProperties.isToolCallingEnabled()) {
            return "【模型供应商】" + aiProviderProperties.getProvider() + "，已启用业务工具调用。";
        }

        return "【模型供应商】" + aiProviderProperties.getProvider() + "，当前兼容模式关闭流式工具调用。"
                + "请优先基于以下本地业务数据快照回答活动推荐、报名咨询和活动查询问题；涉及真实写操作时，引导用户到页面按钮完成。\n"
                + buildActivitySnapshot();
    }

    private String buildActivitySnapshot() {
        List<Activity> activities = activityService.listWithTiers().stream()
                .sorted(Comparator.comparing(Activity::getEventStart, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(8)
                .toList();
        if (activities.isEmpty()) {
            return "暂无活动数据。";
        }
        return activities.stream()
                .map(activity -> "- " + activity.getTitle()
                        + "｜社团：" + activity.getClubName()
                        + "｜类型：" + activity.getActivityType()
                        + "｜地点：" + activity.getVenue()
                        + "｜开始：" + activity.getEventStart()
                        + "｜报名：" + activity.getRegistrationStart()
                        + "｜票档：" + formatTiers(activity))
                .collect(Collectors.joining("\n"));
    }

    private String formatTiers(Activity activity) {
        if (activity.getTierList() == null || activity.getTierList().isEmpty()) {
            return "暂无票档";
        }
        return activity.getTierList().stream()
                .map(tier -> tier.getTierName() + "(容量" + tier.getCapacity() + ")")
                .collect(Collectors.joining("、"));
    }
}
