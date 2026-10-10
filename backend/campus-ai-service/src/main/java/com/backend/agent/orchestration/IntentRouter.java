package com.backend.agent.orchestration;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.backend.agent.domain.AgentRole;
import com.backend.agent.domain.RiskLevel;

/**
 * Deterministic control-plane router. Business safety does not depend on an LLM
 * returning perfectly formatted planning JSON.
 */
@Component
public class IntentRouter {

    private static final Pattern MUTATION_PATTERN = Pattern.compile(
            "(帮我|替我|给我|我要|我想|确认|现在|立即)(报名|参加|取消报名|取消|退票)|"
                    + "((报名|参加|取消报名|退票)(吧|一下|这个))|"
                    + "(退掉(我的|这个)?报名|confirm.{0,8}(register|cancel)|register me|cancel my)",
            Pattern.CASE_INSENSITIVE);

    public AgentPlan plan(String message) {
        String normalized = message == null ? "" : message.trim().toLowerCase(Locale.ROOT);
        Set<AgentRole> roles = new LinkedHashSet<>();

        boolean policy = containsAny(normalized,
                "规则", "政策", "须知", "能不能", "是否可以", "什么时候能", "最晚", "截止", "退款", "迟到", "policy", "rule");
        boolean registration = containsAny(normalized,
                "报名", "参加", "取消", "退票", "我的活动", "我的预约", "registration", "register", "cancel");
        boolean analytics = containsAny(normalized,
                "统计", "趋势", "多少人", "数据", "报表", "转化", "analytics", "dashboard", "count");
        boolean activity = containsAny(normalized,
                "活动", "讲座", "比赛", "招新", "志愿", "社团", "票档", "地点", "场地", "最近", "activity", "event");

        if (activity || registration) {
            roles.add(AgentRole.ACTIVITY_RESEARCHER);
        }
        if (registration) {
            roles.add(AgentRole.REGISTRATION_SPECIALIST);
        }
        if (policy) {
            roles.add(AgentRole.POLICY_ANALYST);
        }
        if (analytics) {
            roles.add(AgentRole.DATA_ANALYST);
        }
        if (roles.isEmpty()) {
            roles.add(AgentRole.CAMPUS_GUIDE);
        }

        boolean approvalRequired = registration && MUTATION_PATTERN.matcher(normalized).find();
        String approvalReason = approvalRequired
                ? "检测到报名数据写操作。系统会先完成只读核验，只有当前用户明确批准后才允许事务 Agent 调用写工具。"
                : null;

        List<AgentPlan.PlannedTask> tasks = new ArrayList<>();
        int index = 1;
        for (AgentRole role : roles) {
            tasks.add(new AgentPlan.PlannedTask(
                    "task-%02d".formatted(index++),
                    role,
                    objectiveFor(role),
                    RiskLevel.READ_ONLY,
                    List.of()));
        }

        String intent = roles.stream().map(Enum::name).reduce((left, right) -> left + "+" + right).orElse("GENERAL");
        return new AgentPlan(intent, List.copyOf(tasks), approvalRequired, approvalReason);
    }

    private String objectiveFor(AgentRole role) {
        return switch (role) {
            case ACTIVITY_RESEARCHER -> "检索与用户问题相关的实时活动、场地和票档事实";
            case REGISTRATION_SPECIALIST -> "只读核验当前用户报名记录并明确后续操作所需参数";
            case POLICY_ANALYST -> "从知识库检索适用政策，标注缺失或冲突信息";
            case DATA_ANALYST -> "查询并解释活动报名统计，区分事实与推断";
            case CAMPUS_GUIDE -> "识别一般咨询并提供简洁的校园社团指引";
            case TRANSACTION_EXECUTOR -> "仅在审批后执行报名数据变更并返回工具回执";
            case QUALITY_REVIEWER -> "基于各 Agent 证据形成一致、可执行的最终答复";
        };
    }

    private boolean containsAny(String source, String... values) {
        for (String value : values) {
            if (source.contains(value)) {
                return true;
            }
        }
        return false;
    }
}
