package com.backend.init;

import com.backend.entity.Activity;
import com.backend.entity.TicketTier;
import com.backend.entity.User;
import com.backend.service.ActivityService;
import com.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Configuration
@Profile("local")
@RequiredArgsConstructor
public class DevDataSeeder {

    private final UserService userService;
    private final ActivityService activityService;
    private final PasswordEncoder passwordEncoder;

    @Bean
    @ConditionalOnProperty(name = "app.seed.dev-data", havingValue = "true", matchIfMissing = true)
    ApplicationRunner seedDevData() {
        return args -> {
            if (userService.getByUsername("alice123") != null) {
                log.info("开发演示数据已存在，跳过 DevDataSeeder");
                return;
            }
            log.info("正在初始化本地开发演示数据...");
            seedUsers();
            seedActivities();
            log.info("本地开发演示数据初始化完成（演示账号密码均为 pass123 / admin456 / cs789）");
        };
    }

    private void seedUsers() {
        createUser("alice123", "pass123", "alice@campus.edu", "Alice Wang", 1, 1);
        createUser("bob_admin", "admin456", "bob@campusclub.edu", "Bob Li", 3, 5);
        createUser("charlie_cs", "cs789", "charlie@campusclub.edu", "Charlie Zhang", 2, 2);
        createUser("david_user", "david2023", "david@163.com", "David Chen", 1, 3);
        createUser("eva_user", "eva321", "eva@qq.com", "Eva Liu", 1, 2);
    }

    private void createUser(String username, String rawPassword, String email, String realName, int userType, int memberLevel) {
        if (userService.getByUsername(username) != null) {
            return;
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setEmail(email);
        user.setRealName(realName);
        user.setUserType(userType);
        user.setMemberLevel(memberLevel);
        userService.save(user);
    }

    private void seedActivities() {
        if (!activityService.list().isEmpty()) {
            return;
        }
        Activity algo = buildActivity("ACT-ALGO-001", "算法分享会：图论入门", "计算机协会", "讲座",
                "学生活动中心 201", "2026-05-20 09:00:00", "2026-05-25 19:00:00");
        algo.setTierList(List.of(
                tier("TCK-ALGO-STD", "普通票", 0, 80, 1),
                tier("TCK-ALGO-VIP", "前排预留", 0, 20, 0)
        ));
        activityService.saveWithTiers(algo);
        log.debug("Seeded demo activity: {}", algo.getTitle());
    }

    private Activity buildActivity(String code, String title, String club, String type, String venue,
                                   String regStart, String eventStart) {
        Activity activity = new Activity();
        activity.setActivityCode(code);
        activity.setTitle(title);
        activity.setClubName(club);
        activity.setActivityType(type);
        activity.setVenue(venue);
        activity.setRegistrationStart(LocalDateTime.parse(regStart.replace(" ", "T")));
        activity.setEventStart(LocalDateTime.parse(eventStart.replace(" ", "T")));
        activity.setStatus(1);
        return activity;
    }

    private TicketTier tier(String code, String name, double price, int capacity, int isDefault) {
        TicketTier tier = new TicketTier();
        tier.setTicketCode(code);
        tier.setTierName(name);
        tier.setPrice(BigDecimal.valueOf(price));
        tier.setCapacity(capacity);
        tier.setIsDefault(isDefault);
        return tier;
    }
}
