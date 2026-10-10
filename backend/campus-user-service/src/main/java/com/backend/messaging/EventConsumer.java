package com.backend.messaging;
import com.backend.contract.CloudEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.*;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
@Component
@ConditionalOnProperty(name="app.messaging.enabled",havingValue="true")
public class EventConsumer {
 private final EventInbox inbox;private final ObjectMapper json;private final String nameserver,topic;private DefaultMQPushConsumer consumer;
 public EventConsumer(EventInbox inbox,ObjectMapper json,@Value("${app.messaging.nameserver}")String nameserver,@Value("${app.messaging.topic:campus-events}")String topic){this.inbox=inbox;this.json=json;this.nameserver=nameserver;this.topic=topic;}
 @PostConstruct public void start()throws Exception {
  consumer=new DefaultMQPushConsumer("campus-user-events");consumer.setInstanceName(java.util.UUID.randomUUID().toString());consumer.setNamesrvAddr(nameserver);
  consumer.setConsumeThreadMin(2);consumer.setConsumeThreadMax(4);consumer.setMaxReconsumeTimes(5);consumer.subscribe(topic,"*");
  consumer.registerMessageListener((MessageListenerConcurrently)(messages,context)->{
   try{for(var message:messages)inbox.accept(json.readValue(message.getBody(),CloudEvent.class));return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;}
   catch(Exception e){return ConsumeConcurrentlyStatus.RECONSUME_LATER;}
  });consumer.start();
 }
 @PreDestroy public void close(){if(consumer!=null)consumer.shutdown();}
}
