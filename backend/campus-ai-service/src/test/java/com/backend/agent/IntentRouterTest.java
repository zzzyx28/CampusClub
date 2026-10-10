package com.backend.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.backend.agent.domain.AgentRole;
import com.backend.agent.orchestration.AgentPlan;
import com.backend.agent.orchestration.IntentRouter;

class IntentRouterTest {

    private final IntentRouter router = new IntentRouter();

    @Test
    void routesCrossDomainQuestionToParallelSpecialists() {
        AgentPlan plan = router.plan("最近有哪些讲座，报名后最晚什么时候可以取消？");

        List<AgentRole> roles = plan.tasks().stream().map(AgentPlan.PlannedTask::role).toList();
        assertTrue(roles.contains(AgentRole.ACTIVITY_RESEARCHER));
        assertTrue(roles.contains(AgentRole.REGISTRATION_SPECIALIST));
        assertTrue(roles.contains(AgentRole.POLICY_ANALYST));
        assertFalse(plan.approvalRequired(), "read-only policy question must not trigger write approval");
    }

    @Test
    void putsExplicitRegistrationMutationBehindApprovalGate() {
        AgentPlan plan = router.plan("帮我报名算法分享会");

        assertTrue(plan.approvalRequired());
        assertTrue(plan.tasks().stream().allMatch(task -> task.riskLevel().name().equals("READ_ONLY")));
    }

    @Test
    void usesGeneralGuideForChitchat() {
        AgentPlan plan = router.plan("你好，你能做什么？");

        assertEquals(List.of(AgentRole.CAMPUS_GUIDE),
                plan.tasks().stream().map(AgentPlan.PlannedTask::role).toList());
    }

    @Test
    void doesNotTreatPolicyWordingAsAWriteCommand() {
        AgentPlan plan = router.plan("我要知道报名规则和退票政策");

        assertFalse(plan.approvalRequired());
        assertTrue(plan.tasks().stream().anyMatch(task -> task.role() == AgentRole.POLICY_ANALYST));
    }
}
