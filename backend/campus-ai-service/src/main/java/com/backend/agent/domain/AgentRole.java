package com.backend.agent.domain;

/**
 * Bounded roles in the CampusClub agent team. Each role receives only the tools
 * required by its responsibility (least privilege).
 */
public enum AgentRole {
    ACTIVITY_RESEARCHER("活动侦察员", "检索实时活动与票档"),
    REGISTRATION_SPECIALIST("报名顾问", "只读分析本人报名与报名诉求"),
    POLICY_ANALYST("政策分析员", "基于知识库解释规则与政策"),
    DATA_ANALYST("数据分析员", "分析报名与活动运营数据"),
    CAMPUS_GUIDE("校园向导", "处理无需业务工具的一般咨询"),
    TRANSACTION_EXECUTOR("事务执行员", "在审批后执行报名或取消"),
    QUALITY_REVIEWER("质量审校员", "汇总证据、消解冲突并生成答复");

    private final String displayName;
    private final String responsibility;

    AgentRole(String displayName, String responsibility) {
        this.displayName = displayName;
        this.responsibility = responsibility;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getResponsibility() {
        return responsibility;
    }
}
