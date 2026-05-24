package com.backend.config;

import com.backend.entity.Activity;
import com.backend.entity.Registration;
import com.backend.entity.User;
import com.backend.service.AiService;
import com.backend.util.AiToolJson;
import com.backend.vo.DashboardData;
import com.backend.vo.RegistrationDetail;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** 增删工具时请同步 {@link CampusClubAgentPrompts#REGISTERED_TOOLS_CATALOG} 中的说明。 */
@Configuration
public class AiToolsConfig {

    private final AiService aiService;

    public AiToolsConfig(AiService aiService) {
        this.aiService = aiService;
    }

    public record CancelRegistrationRequest(
            @JsonPropertyDescription("报名主键 ID（数字），不是票种编码") Long registrationId,
            @JsonPropertyDescription("报名人真实姓名，须与报名用户一致") String name) {
    }

    public record RegistrationDetailRequest(
            @JsonPropertyDescription("报名主键 ID") Long registrationId) {
    }

    public record RegistrationsByNameRequest(
            @JsonPropertyDescription("用户真实姓名，用于列出该姓名下的报名") String realName) {
    }

    public record CreateRegistrationRequest(
            @JsonPropertyDescription("用户 ID，须与当前登录用户一致；若用户未登录则先请用户登录") Long userId,
            @JsonPropertyDescription("活动主键 ID，来自 searchActivities 等查询结果") Long activityId,
            @JsonPropertyDescription("票档名称，如普通票；不确定可传 null 或空串") String tierName,
            @JsonPropertyDescription("报名人真实姓名") String name) {
    }

    public record ActivityByTicketCodeRequest(
            @JsonPropertyDescription("票种编码，如 TCK-ALGO-STD") String ticketCode) {
    }

    public record SearchActivitiesRequest(
            @JsonPropertyDescription("活动类型，中文，如：讲座") String activityType,
            @JsonPropertyDescription("活动地点，中文，如：图书馆") String venue,
            @JsonPropertyDescription("活动日期 yyyy-MM-dd") String date) {
    }

    public record ActivitiesByClubRequest(
            @JsonPropertyDescription("主办社团名称，如：计算机协会") String clubName) {
    }

    public record ListUpcomingActivitiesRequest(
            @JsonPropertyDescription("返回条数；用户说「最近有什么活动」可给 5～10；不传则默认 5，最大 30") Integer limit) {
    }

    @Bean
    @Description("列出即将开放报名或即将开始的活动。用于「最近有什么活动」「还能报什么」等未指定类型的问法。")
    public Function<ListUpcomingActivitiesRequest, String> listUpcomingActivities() {
        return request -> {
            try {
                Integer lim = request != null ? request.limit() : null;
                List<Activity> activities = aiService.listUpcomingActivities(lim);
                if (activities.isEmpty()) {
                    return AiToolJson.ok("暂无即将开始或开放报名的活动", activities);
                }
                return AiToolJson.ok("共 " + activities.size() + " 个活动", activities);
            } catch (Exception e) {
                return AiToolJson.fail(e.getMessage() != null ? e.getMessage() : "查询失败");
            }
        };
    }

    @Bean
    @Description("取消报名。需报名 ID 与姓名校验通过；活动开始前 24 小时内可能不可取消（以业务规则为准）。")
    public Function<CancelRegistrationRequest, String> cancelRegistration() {
        return request -> {
            try {
                aiService.cancelRegistration(request.registrationId(), request.name());
                return AiToolJson.ok("报名已取消", request.registrationId());
            } catch (Exception e) {
                return AiToolJson.fail(e.getMessage() != null ? e.getMessage() : "取消失败");
            }
        };
    }

    @Bean
    @Description("按报名 ID 查询报名与活动详情，用于回答用户或人工咨询前核对。")
    public Function<RegistrationDetailRequest, String> getRegistrationDetail() {
        return request -> {
            try {
                RegistrationDetail d = aiService.getRegistrationDetail(request.registrationId());
                return AiToolJson.ok("查询成功", d);
            } catch (Exception e) {
                return AiToolJson.fail(e.getMessage() != null ? e.getMessage() : "报名查询失败");
            }
        };
    }

    @Bean
    @Description("按用户真实姓名查询该用户名下未取消的报名列表。")
    public Function<RegistrationsByNameRequest, String> getRegistrationsByName() {
        return request -> {
            try {
                List<RegistrationDetail> list = aiService.getRegistrationsByName(request.realName());
                return AiToolJson.ok("共 " + list.size() + " 条报名", list);
            } catch (Exception e) {
                return AiToolJson.fail(e.getMessage() != null ? e.getMessage() : "未找到报名记录");
            }
        };
    }

    @Bean
    @Description("创建活动报名。userId 须为当前登录用户；activityId 来自活动查询；可先确认票档再报名。")
    public Function<CreateRegistrationRequest, String> createRegistration() {
        return request -> {
            try {
                Registration registration = aiService.createRegistration(
                        request.userId(), request.activityId(), request.tierName(), request.name());
                return AiToolJson.ok("报名成功", registration);
            } catch (Exception e) {
                return AiToolJson.fail(e.getMessage() != null ? e.getMessage() : "报名失败");
            }
        };
    }

    @Bean
    @Description("按票种编码查询活动。")
    public Function<ActivityByTicketCodeRequest, String> getActivityByTicketCode() {
        return request -> {
            try {
                Activity activity = aiService.getActivityByTicketCode(request.ticketCode());
                return AiToolJson.ok("查询成功", activity);
            } catch (Exception e) {
                return AiToolJson.fail(e.getMessage() != null ? e.getMessage() : "未找到活动");
            }
        };
    }

    @Bean
    @Description("按活动类型、地点与日期搜索活动列表；日期必须为 yyyy-MM-dd。")
    public Function<SearchActivitiesRequest, String> searchActivities() {
        return request -> {
            try {
                List<Activity> activities = aiService.searchActivities(
                        request.activityType(), request.venue(), request.date());
                return AiToolJson.ok("共 " + activities.size() + " 个活动", activities);
            } catch (Exception e) {
                return AiToolJson.fail(e.getMessage() != null ? e.getMessage() : "搜索无结果");
            }
        };
    }

    @Bean
    @Description("按主办社团名称筛选活动列表。")
    public Function<ActivitiesByClubRequest, String> getActivitiesByClub() {
        return request -> {
            try {
                List<Activity> activities = aiService.getActivitiesByClub(request.clubName());
                return AiToolJson.ok("共 " + activities.size() + " 个活动", activities);
            } catch (Exception e) {
                return AiToolJson.fail(e.getMessage() != null ? e.getMessage() : "未找到活动");
            }
        };
    }

    public record DashboardOverviewRequest(
            @JsonPropertyDescription("无需参数，直接调用即可") String unused) {
    }

    public record UserProfileRequest(
            @JsonPropertyDescription("用户 ID，从系统上下文中的当前登录用户 ID 获取") Long userId) {
    }

    public record ActivityStatsRequest(
            @JsonPropertyDescription("活动标题关键词，可选；不传则返回近期所有活动的报名统计") String activityTitle) {
    }

    @Bean
    @Description("获取仪表盘概览数据：今日活动数、今日报名数、活跃用户数、近期趋势。用于「最近报名情况怎么样」「今天有什么数据」等统计类问题。")
    public Function<DashboardOverviewRequest, String> getDashboardOverview() {
        return request -> {
            try {
                DashboardData data = aiService.getDashboardOverview();
                return AiToolJson.ok("仪表盘数据", data);
            } catch (Exception e) {
                return AiToolJson.fail(e.getMessage() != null ? e.getMessage() : "获取仪表盘数据失败");
            }
        };
    }

    @Bean
    @Description("获取当前登录用户的个人信息、社团活跃度等级及对应权益。用于「我的等级」「我有什么权益」「我的个人信息」等。")
    public Function<UserProfileRequest, String> getUserProfile() {
        return request -> {
            try {
                User user = aiService.getUserProfile(request.userId());
                return AiToolJson.ok("用户信息", user);
            } catch (Exception e) {
                return AiToolJson.fail(e.getMessage() != null ? e.getMessage() : "获取用户信息失败");
            }
        };
    }

    @Bean
    @Description("查询活动的报名统计情况（各活动报名人数）。可指定活动标题关键词筛选，不传则返回近期活动统计。用于「XX活动有多少人报名」「哪个活动最热门」等。")
    public Function<ActivityStatsRequest, String> getActivityStats() {
        return request -> {
            try {
                List<Map<String, Object>> stats = aiService.getActivityStats(request.activityTitle());
                if (stats.isEmpty()) {
                    return AiToolJson.ok("暂无活动统计信息", List.of());
                }
                return AiToolJson.ok("共 " + stats.size() + " 个活动统计", stats);
            } catch (Exception e) {
                return AiToolJson.fail(e.getMessage() != null ? e.getMessage() : "查询活动统计失败");
            }
        };
    }
}
