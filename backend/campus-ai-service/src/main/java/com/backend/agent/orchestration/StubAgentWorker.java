package com.backend.agent.orchestration;
import com.backend.agent.domain.*;
import com.backend.service.AiService;
import com.backend.util.AiRequestContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.util.*;
@Component @Profile("stub")
public class StubAgentWorker implements AgentWorker {
 private final AiService ai;public StubAgentWorker(AiService ai){this.ai=ai;}
 public String execute(AgentRole role,String query,List<AgentTaskExecution> evidence,AgentExecutionContext context,boolean approved){
  AiRequestContext.set(new AiRequestContext.Holder(context.userId(),context.username(),context.userType(),context.runId(),approved && role==AgentRole.TRANSACTION_EXECUTOR));
  try{return switch(role){
   case ACTIVITY_RESEARCHER -> "[替身] 查到活动："+ai.listUpcomingActivities(5).stream().map(a->a.getActivityCode()+":"+a.getTitle()).toList();
   case REGISTRATION_SPECIALIST -> "[替身] 本人报名："+ai.getMyRegistrations().size();
   case TRANSACTION_EXECUTOR -> {
    if(!approved)throw new IllegalStateException("审批未通过");
    var code=java.util.regex.Pattern.compile("ACT-[A-Z0-9-]+").matcher(query);
    if(!code.find())throw new IllegalArgumentException("替身报名演示需要活动编码，例如 ACT-ALGO-001");
    var registration=ai.createRegistration(null,code.group(),null);yield "[替身] 报名成功："+registration.getId();
   }
   case DATA_ANALYST -> "[替身] 仪表盘："+ai.getDashboardOverview();
   case POLICY_ANALYST -> "[替身] 政策查询替身；真实政策问答需启用模型。";
   default -> "[替身] 你好，校园活动助手已就绪。";
  };}finally{AiRequestContext.clear();}
 }
 public String review(String query,List<AgentTaskExecution> evidence,AgentExecutionContext context,boolean mutation){return "[本地模型替身] "+evidence.stream().filter(e->e.getStatus()==TaskStatus.COMPLETED).map(AgentTaskExecution::getOutput).toList();}
}
