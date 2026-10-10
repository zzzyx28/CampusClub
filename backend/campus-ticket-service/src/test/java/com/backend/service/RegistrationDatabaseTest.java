package com.backend.service;
import com.backend.client.UserClient;
import com.backend.contract.UserDto;
import com.backend.messaging.OutboxStore;
import com.backend.service.impl.*;
import com.backend.util.*;
import org.junit.jupiter.api.*;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@SpringBootTest(classes=RegistrationDatabaseTest.Config.class,webEnvironment=SpringBootTest.WebEnvironment.NONE,properties={
 "spring.application.name=registration-test","spring.config.import=","spring.cloud.nacos.discovery.enabled=false","spring.cloud.nacos.config.enabled=false",
 "spring.cloud.nacos.config.import-check.enabled=false","spring.cloud.sentinel.enabled=false","app.messaging.enabled=false",
 "spring.flyway.locations=classpath:db/migration","spring.datasource.hikari.maximum-pool-size=20","management.tracing.enabled=false"})
@Testcontainers(disabledWithoutDocker=true)
class RegistrationDatabaseTest {
 @Container static final MySQLContainer<?> MYSQL=new MySQLContainer<>("mysql:8.0.46");
 @DynamicPropertySource static void db(DynamicPropertyRegistry p){p.add("spring.datasource.url",MYSQL::getJdbcUrl);p.add("spring.datasource.username",MYSQL::getUsername);p.add("spring.datasource.password",MYSQL::getPassword);}
 @SpringBootConfiguration @EnableAutoConfiguration @MapperScan("com.backend.dao.mapper")
 @Import({RegistrationServiceImpl.class,ActivityServiceImpl.class,RegistrationCommandService.class,OutboxStore.class}) static class Config{}
 @Autowired RegistrationCommandService commands;@Autowired JdbcTemplate jdbc;@Autowired PlatformTransactionManager manager;
 @MockitoBean UserClient users;
 @BeforeEach void setup(){
  for(String table:List.of("registration_operation","outbox_event","registration","ticket_tier","activity"))jdbc.update("DELETE FROM "+table);
  jdbc.update("INSERT INTO activity(id,activity_code,title,club_name,activity_type,venue,registration_start,event_start,status) VALUES(10,'TEST','Concurrency','Club','Lecture','Hall',NOW(),DATE_ADD(NOW(),INTERVAL 1 DAY),1)");
  jdbc.update("INSERT INTO ticket_tier(id,activity_id,ticket_code,tier_name,price,capacity,is_default) VALUES(100,10,'TICKET','Standard',0,10,1)");
  when(users.getById(anyLong())).thenAnswer(call->{UserDto u=new UserDto();u.setId(call.getArgument(0));return u;});
  identity(1L);
 }
 @AfterEach void clean(){RequestContextHolder.resetRequestAttributes();}
 private void identity(long id){var request=new MockHttpServletRequest();request.setAttribute(AuthConstants.ATTR_USER_ID,id);request.setAttribute(AuthConstants.ATTR_USER_TYPE,1);RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));}
 private int count(String table){return jdbc.queryForObject("SELECT COUNT(*) FROM "+table,Integer.class);}
 private int capacity(){return jdbc.queryForObject("SELECT capacity FROM ticket_tier WHERE id=100",Integer.class);}
 private List<Future<?>> parallel(int n,java.util.function.IntConsumer action)throws Exception{
  try(var executor=Executors.newVirtualThreadPerTaskExecutor()){
   var gate=new CountDownLatch(1);List<Future<?>> futures=new ArrayList<>();
   for(int i=0;i<n;i++){final int index=i;futures.add(executor.submit(()->{try{gate.await();action.accept(index);}catch(InterruptedException e){Thread.currentThread().interrupt();}finally{RequestContextHolder.resetRequestAttributes();}}));}
   gate.countDown();for(var future:futures)future.get(60,TimeUnit.SECONDS);return futures;
  }
 }
 @Test void hundredUsersCompeteForTenSeatsWithoutOverselling()throws Exception{
  AtomicInteger succeeded=new AtomicInteger();
  parallel(100,i->{identity(i+1);try{commands.create((long)i+1,10L,null,"load-"+i);succeeded.incrementAndGet();}catch(RuntimeException expected){}});
  assertEquals(10,succeeded.get());assertEquals(10,count("registration"));assertEquals(0,capacity());assertEquals(10,count("outbox_event"));
 }
 @Test void concurrentSameOperationReturnsOneCommittedReceipt()throws Exception{
  Set<Long> ids=ConcurrentHashMap.newKeySet();parallel(20,i->{identity(1);ids.add(commands.create(1L,10L,null,"same-key").registration().getId());});
  assertEquals(1,ids.size());assertEquals(1,count("registration_operation"));assertEquals(1,count("registration"));assertEquals(1,count("outbox_event"));assertEquals(9,capacity());
 }
 @Test void duplicateUserDifferentKeysCannotConsumeTwoSeats()throws Exception{
  AtomicInteger succeeded=new AtomicInteger();parallel(20,i->{identity(1);try{commands.create(1L,10L,null,"different-"+i);succeeded.incrementAndGet();}catch(RuntimeException expected){}});
  assertEquals(1,succeeded.get());assertEquals(9,capacity());assertEquals(1,count("outbox_event"));
 }
 @Test void cancelRaceRestoresCapacityOnce()throws Exception{
  long id=commands.create(1L,10L,null,"create").registration().getId();AtomicInteger succeeded=new AtomicInteger();
  parallel(20,i->{identity(1);try{if(commands.cancel(id,"cancel-"+i).cancelled())succeeded.incrementAndGet();}catch(RuntimeException expected){}});
  assertEquals(1,succeeded.get());assertEquals(10,capacity());assertEquals(2,count("outbox_event"));
 }
 @Test void conflictAndReplayAfterLostResponse(){
  var first=commands.create(1L,10L,null,"receipt");reset(users);when(users.getById(anyLong())).thenThrow(new IllegalStateException("offline"));
  assertEquals(first,commands.create(1L,10L,null,"receipt"));assertEquals(first,commands.find("receipt"));
  assertThrows(IllegalStateException.class,()->commands.create(1L,11L,null,"receipt"));assertEquals(9,capacity());
 }
 @Test void rollbackRemovesBusinessOperationAndOutbox(){
  assertThrows(IllegalStateException.class,()->new TransactionTemplate(manager).execute(s->{commands.create(1L,10L,null,"rollback");throw new IllegalStateException("simulated failure after insert");}));
  assertEquals(0,count("registration"));assertEquals(0,count("registration_operation"));assertEquals(0,count("outbox_event"));assertEquals(10,capacity());
 }
 @Test void operationsCannotBeReadByAnotherUser(){commands.create(1L,10L,null,"private");identity(2);assertNull(commands.find("private"));}
}
