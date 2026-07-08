package com.backend.service.impl;

import com.backend.dao.mongo.ConversationRepository;
import com.backend.entity.ConversationDocument;
import com.backend.entity.User;
import com.backend.service.AgentMemoryService;
import com.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AgentMemoryServiceImpl implements AgentMemoryService {

    private static final int SUMMARY_LIMIT = 6;
    private static final Pattern DATE_PATTERN = Pattern.compile("(20\\d{2}-\\d{1,2}-\\d{1,2})");
    private static final Pattern NAME_PATTERN = Pattern.compile("(?:我叫|我是|姓名是|名字叫)\\s*([\\p{IsHan}A-Za-z][\\p{IsHan}A-Za-z\\s·]{1,30})");
    private static final Pattern CLUB_PATTERN = Pattern.compile("([\\p{IsHan}A-Za-z0-9]{2,20}(?:社|社团|协会|俱乐部))");
    private static final Pattern VENUE_PATTERN = Pattern.compile("(?:在|地点|场地|去)([\\p{IsHan}A-Za-z0-9楼室馆厅中心\\-]{2,12}?)(?:的|有|举办|$|[，,。\\s])");

    private final ConversationRepository conversationRepository;
    private final UserService userService;

    @Override
    public void updateFromMessages(String conversationId, List<Message> messages) {
        if (!StringUtils.hasText(conversationId) || messages == null || messages.isEmpty()) {
            return;
        }

        ConversationDocument conversation = conversationRepository.findByConversationUuid(conversationId)
                .orElseGet(() -> newConversation(conversationId));
        ConversationDocument.AgentMemory memory = ensureMemory(conversation);

        for (Message message : messages) {
            if (!(message instanceof UserMessage) || message instanceof AssistantMessage
                    || !StringUtils.hasText(message.getContent())) {
                continue;
            }
            mergeUserSignal(memory, message.getContent());
        }

        memory.setUpdatedAt(new Date());
        conversationRepository.save(conversation);
    }

    @Override
    public String buildMemoryContext(String conversationId, Long userId) {
        StringBuilder context = new StringBuilder();

        if (userId != null) {
            User user = userService.getById(userId);
            if (user != null) {
                context.append("登录用户画像：userId=")
                        .append(user.getId())
                        .append("，用户名 ")
                        .append(nullSafe(user.getUsername(), "未知"))
                        .append("，姓名 ")
                        .append(nullSafe(user.getRealName(), "姓名未知"))
                        .append("，会员等级 ")
                        .append(user.getMemberLevel() != null ? user.getMemberLevel() : "未知")
                        .append("。");
                appendIfPresent(context, "邮箱", user.getEmail());
                appendIfPresent(context, "手机", user.getPhone());
                context.append('\n');
            }
        }

        if (!StringUtils.hasText(conversationId)) {
            return fallback(context);
        }

        Optional<ConversationDocument> optional = conversationRepository.findByConversationUuid(conversationId);
        if (optional.isEmpty() || optional.get().getAgentMemory() == null) {
            return fallback(context);
        }

        ConversationDocument.AgentMemory memory = optional.get().getAgentMemory();
        if (StringUtils.hasText(memory.getSummary())) {
            context.append("会话长期摘要：").append(memory.getSummary()).append('\n');
        }
        if (memory.getPreferences() != null && !memory.getPreferences().isEmpty()) {
            context.append("用户偏好：").append(renderMap(memory.getPreferences())).append('\n');
        }
        if (memory.getSlots() != null && !memory.getSlots().isEmpty()) {
            context.append("当前任务槽位：").append(renderMap(memory.getSlots())).append('\n');
        }
        if (StringUtils.hasText(memory.getLastIntent())) {
            context.append("上次识别意图：").append(memory.getLastIntent()).append('\n');
        }

        return fallback(context);
    }

    private ConversationDocument newConversation(String conversationId) {
        ConversationDocument conversation = new ConversationDocument();
        conversation.setConversationUuid(conversationId);
        ConversationDocument.Metadata metadata = new ConversationDocument.Metadata();
        metadata.setSessionStart(new Date());
        conversation.setMetadata(metadata);
        return conversation;
    }

    private ConversationDocument.AgentMemory ensureMemory(ConversationDocument conversation) {
        if (conversation.getAgentMemory() == null) {
            conversation.setAgentMemory(new ConversationDocument.AgentMemory());
        }
        ConversationDocument.AgentMemory memory = conversation.getAgentMemory();
        if (memory.getPreferences() == null) {
            memory.setPreferences(new LinkedHashMap<>());
        }
        if (memory.getSlots() == null) {
            memory.setSlots(new LinkedHashMap<>());
        }
        return memory;
    }

    private void mergeUserSignal(ConversationDocument.AgentMemory memory, String text) {
        String normalized = text.trim();
        String intent = inferIntent(normalized);
        memory.setLastIntent(intent);

        rememberSummary(memory, intent + "：" + compact(normalized));
        extractName(memory, normalized);
        extractClub(memory, normalized);
        extractVenue(memory, normalized);
        extractDate(memory, normalized);
        extractActivityType(memory, normalized);
        extractTicketTier(memory, normalized);
    }

    private void extractName(ConversationDocument.AgentMemory memory, String text) {
        Matcher matcher = NAME_PATTERN.matcher(text);
        if (matcher.find()) {
            memory.getSlots().put("realName", matcher.group(1).trim());
        }
    }

    private void extractClub(ConversationDocument.AgentMemory memory, String text) {
        Matcher matcher = CLUB_PATTERN.matcher(text);
        if (matcher.find()) {
            String clubName = matcher.group(1).trim();
            int possessiveIndex = clubName.lastIndexOf('的');
            if (possessiveIndex >= 0 && possessiveIndex < clubName.length() - 1) {
                clubName = clubName.substring(possessiveIndex + 1);
            }
            memory.getPreferences().put("club", clubName);
        }
    }

    private void extractVenue(ConversationDocument.AgentMemory memory, String text) {
        Matcher matcher = VENUE_PATTERN.matcher(text);
        if (matcher.find()) {
            memory.getPreferences().put("venue", matcher.group(1).trim());
        }
    }

    private void extractDate(ConversationDocument.AgentMemory memory, String text) {
        Matcher matcher = DATE_PATTERN.matcher(text);
        if (matcher.find()) {
            String raw = matcher.group(1);
            try {
                LocalDate date = LocalDate.parse(raw);
                memory.getSlots().put("date", date.toString());
            } catch (DateTimeParseException ignored) {
                memory.getSlots().put("date", raw);
            }
        }
    }

    private void extractActivityType(ConversationDocument.AgentMemory memory, String text) {
        Map<String, String> aliases = Map.of(
                "讲座", "讲座",
                "比赛", "比赛",
                "招新", "招新",
                "志愿", "志愿",
                "工作坊", "工作坊",
                "分享会", "分享会");
        for (Map.Entry<String, String> entry : aliases.entrySet()) {
            if (text.contains(entry.getKey())) {
                memory.getPreferences().put("activityType", entry.getValue());
                return;
            }
        }
    }

    private void extractTicketTier(ConversationDocument.AgentMemory memory, String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        if (text.contains("VIP") || lower.contains("vip")) {
            memory.getSlots().put("tierName", "VIP");
        } else if (text.contains("普通票")) {
            memory.getSlots().put("tierName", "普通票");
        } else if (text.contains("学生票")) {
            memory.getSlots().put("tierName", "学生票");
        }
    }

    private String inferIntent(String text) {
        if (containsAny(text, "报名", "参加", "预约", "抢票")) {
            return "报名活动";
        }
        if (containsAny(text, "取消", "退票", "不去了")) {
            return "取消报名";
        }
        if (containsAny(text, "有哪些", "有什么", "查询", "搜索", "看看")) {
            return "活动查询";
        }
        if (containsAny(text, "规则", "政策", "能不能", "可以吗")) {
            return "政策咨询";
        }
        if (containsAny(text, "我的", "等级", "权益", "个人信息")) {
            return "个人信息查询";
        }
        return "一般咨询";
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private void rememberSummary(ConversationDocument.AgentMemory memory, String item) {
        List<String> items = new ArrayList<>();
        if (StringUtils.hasText(memory.getSummary())) {
            items.addAll(List.of(memory.getSummary().split("；")));
        }
        String compacted = compact(item);
        items.removeIf(existing -> existing.equals(compacted));
        items.add(compacted);
        int fromIndex = Math.max(0, items.size() - SUMMARY_LIMIT);
        memory.setSummary(String.join("；", items.subList(fromIndex, items.size())));
    }

    private String compact(String text) {
        String normalized = text.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 80 ? normalized : normalized.substring(0, 80) + "...";
    }

    private String renderMap(Map<String, String> map) {
        List<String> pairs = new ArrayList<>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (StringUtils.hasText(entry.getValue())) {
                pairs.add(entry.getKey() + "=" + entry.getValue());
            }
        }
        return String.join("，", pairs);
    }

    private void appendIfPresent(StringBuilder builder, String label, String value) {
        if (StringUtils.hasText(value)) {
            builder.append("，").append(label).append(" ").append(value);
        }
    }

    private String nullSafe(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String fallback(StringBuilder context) {
        if (context.isEmpty()) {
            return "暂无可用长期记忆。";
        }
        return context.toString().trim();
    }
}
