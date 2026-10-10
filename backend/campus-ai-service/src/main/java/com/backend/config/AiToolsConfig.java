package com.backend.config;

import com.backend.contract.ActivityDto;
import com.backend.contract.RegistrationDto;
import com.backend.service.AiService;
import com.backend.util.AiToolJson;
import com.backend.vo.DashboardData;
import com.backend.vo.RegistrationDetail;
import com.backend.vo.UserProfileVo;
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
            @JsonPropertyDescription("报名主键 ID（数字），来自 getMyRegistrations 结果中的 registrationId") Long registrationId) {
    }

    public record RegistrationDetailRequest(
            @JsonPropertyDescription("报名主键 ID") Long registrationId) {
    }

    public record MyRegistrationsRequest(
            @JsonPropertyDescription("无需参数，自动查询当前登录用户") String unused) {
    }

    public record CreateRegistrationRequest(
            @JsonPropertyDescription("活动主键 ID，来自活动查询结果的 id 字段；与 activityCode 二选一") Long activityId,
            @JsonPropertyDescription("活动编码，如 ACT-ALGO-001；与 activityId 二选一") String activityCode,
            @JsonPropertyDescription("票档名称，如普通票；不确定可传 null") String tierName) {
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

    public record DashboardOverviewRequest(
            @JsonPropertyDescription("无需参数，仅管理员可用") String unused) {
    }

    public record CurrentUserProfileRequest(
            @JsonPropertyDescription("无需参数，自动返回当前登录用户") String unused) {
    }

    public record ActivityStatsRequest(
            @JsonPropertyDescription("活动标题关键词，可选；不传则返回近期所有活动的报名统计") String activityTitle) {
    }

    @Bean
    @Description("列出即将开放报名或即将开始的活动。用于「最近有什么活动」「还能报什么」等未指定类型的问法。")
    public Function<ListUpcomingActivitiesRequest, String> listUpcomingActivities() {
        return request -> {
            try {
                Integer lim = request != null ? request.limit() : null;
                List<ActivityDto> activities = aiService.listUpcomingActivities(lim);
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
    @Description("取消当前登录用户本人的报名。需要报名 ID；系统会自动校验归属，无需提供姓名。")
    public Function<CancelRegistrationRequest, String> cancelRegistration() {
        return request -> {
            try {
                aiService.cancelRegistration(request.registrationId());
                return AiToolJson.ok("报名已取消", request.registrationId());
            } catch (Exception e) {
                return AiToolJson.fail(e.getMessage() != null ? e.getMessage() : "取消失败");
            }
        };
    }

    @Bean
    @Description("按报名 ID 查询报名与活动详情，仅限当前用户本人或管理员。")
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
    @Description("查询当前登录用户名下未取消的报名列表。用于「我的报名」「我报了哪些」。")
    public Function<MyRegistrationsRequest, String> getMyRegistrations() {
        return request -> {
            try {
                List<RegistrationDetail> list = aiService.getMyRegistrations();
                if (list.isEmpty()) {
                    return AiToolJson.ok("当前暂无有效报名", list);
                }
                return AiToolJson.ok("共 " + list.size() + " 条报名", list);
            } catch (Exception e) {
                return AiToolJson.fail(e.getMessage() != null ? e.getMessage() : "查询报名失败");
            }
        };
    }

    @Bean
    @Description("为当前登录用户创建活动报名。activityId 或 activityCode 至少提供一个；票档不确定可传 null。")
    public Function<CreateRegistrationRequest, String> createRegistration() {
        return request -> {
            try {
                RegistrationDto registration = aiService.createRegistration(
                        request.activityId(), request.activityCode(), request.tierName());
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
                ActivityDto activity = aiService.getActivityByTicketCode(request.ticketCode());
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
                List<ActivityDto> activities = aiService.searchActivities(
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
                List<ActivityDto> activities = aiService.getActivitiesByClub(request.clubName());
                return AiToolJson.ok("共 " + activities.size() + " 个活动", activities);
            } catch (Exception e) {
                return AiToolJson.fail(e.getMessage() != null ? e.getMessage() : "未找到活动");
            }
        };
    }

    @Bean
    @Description("获取仪表盘概览数据，仅管理员可用。")
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
    @Description("获取当前登录用户的个人信息、社团活跃度等级及权益。无需传 userId。")
    public Function<CurrentUserProfileRequest, String> getCurrentUserProfile() {
        return request -> {
            try {
                UserProfileVo user = aiService.getCurrentUserProfile();
                return AiToolJson.ok("用户信息", user);
            } catch (Exception e) {
                return AiToolJson.fail(e.getMessage() != null ? e.getMessage() : "获取用户信息失败");
            }
        };
    }

    @Bean
    @Description("查询活动的报名统计情况（各活动报名人数）。可指定活动标题关键词筛选，不传则返回近期活动统计。")
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
