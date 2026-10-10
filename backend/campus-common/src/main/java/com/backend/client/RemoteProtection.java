package com.backend.client;
import com.alibaba.csp.sentinel.*;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.flow.*;
import com.alibaba.csp.sentinel.slots.block.degrade.*;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.core.env.Environment;
import org.springframework.context.event.EventListener;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import jakarta.annotation.PostConstruct;
import java.util.*;
@Aspect @Component
public class RemoteProtection {
 private final Environment env;public RemoteProtection(Environment env){this.env=env;}
 @PostConstruct public void load(){
  List<FlowRule> flow=new ArrayList<>();List<DegradeRule> degrade=new ArrayList<>();
  for(String name:List.of("UserClient","ActivityClient","RegistrationClient","DashboardClient","ConversationStatsClient")){
   String resource="remote:"+name;
   FlowRule f=new FlowRule(resource);f.setGrade(0);f.setCount(env.getProperty("app.remote.max-concurrent",Integer.class,20));flow.add(f);
   DegradeRule d=new DegradeRule(resource);d.setGrade(1);d.setCount(0.5);d.setMinRequestAmount(5);d.setStatIntervalMs(10000);d.setTimeWindow(10);degrade.add(d);
  }
  FlowRuleManager.loadRules(flow);DegradeRuleManager.loadRules(degrade);
 }
 @EventListener(EnvironmentChangeEvent.class) public void refresh(){load();}
 @Around("execution(* com.backend.client.*Client.*(..))")
 public Object invoke(ProceedingJoinPoint call)throws Throwable {
  String resource="remote:"+call.getSignature().getDeclaringType().getSimpleName();
  try(Entry entry=SphU.entry(resource)){
   try{return call.proceed();}catch(Throwable e){
    if(!(e instanceof feign.FeignException f) || f.status()<0 || f.status()>=500)Tracer.traceEntry(e,entry);
    throw e;
   }
  }catch(BlockException e){throw new com.backend.exception.DependencyUnavailableException("依赖调用已限流或熔断",e);}
 }
}
