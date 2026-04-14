package com.backend.config;

import com.backend.entity.Product;
import com.backend.entity.ProductOrder;
import com.backend.service.AiService;
import com.backend.util.AiToolJson;
import com.backend.vo.ProductOrderDetail;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.List;
import java.util.function.Function;

/** 增删工具时请同步 {@link SmartShopAgentPrompts#REGISTERED_TOOLS_CATALOG} 中的说明。 */
@Configuration
public class AiToolsConfig {

    private final AiService aiService;

    public AiToolsConfig(AiService aiService) {
        this.aiService = aiService;
    }

    public record CancelProductOrderRequest(
            @JsonPropertyDescription("订单主键 ID（数字），不是 SKU") Long orderId,
            @JsonPropertyDescription("下单人真实姓名，须与订单用户一致") String name) {
    }

    public record ProductOrderDetailRequest(
            @JsonPropertyDescription("订单主键 ID") Long orderId) {
    }

    public record BuyerProductOrdersRequest(
            @JsonPropertyDescription("用户真实姓名，用于列出该姓名下的订单") String realName) {
    }

    public record CreateProductOrderRequest(
            @JsonPropertyDescription("下单用户 ID，须与当前登录用户一致；若用户未登录则先请用户登录") Long userId,
            @JsonPropertyDescription("商品主键 ID，来自 searchProducts 等查询结果") Long productId,
            @JsonPropertyDescription("选购规格，如颜色尺码；不确定可传 null 或空串") String specOption,
            @JsonPropertyDescription("收货人/下单人真实姓名") String name) {
    }

    public record ProductBySkuRequest(
            @JsonPropertyDescription("商品 SKU，如 EC-MU1234") String sku) {
    }

    public record SearchProductsRequest(
            @JsonPropertyDescription("类目，中文，如：手机通讯") String category,
            @JsonPropertyDescription("发货地/仓库，中文，如：上海仓") String shipFrom,
            @JsonPropertyDescription("开售日期 yyyy-MM-dd（匹配当日可上架商品）") String date) {
    }

    public record ProductsByBrandRequest(
            @JsonPropertyDescription("品牌名称，如：星辰数码") String brand) {
    }

    public record ListRecentlyAvailableProductsRequest(
            @JsonPropertyDescription("返回条数；用户说「最近上新」可给 5～10；不传则默认 5，最大 30") Integer limit) {
    }

    @Bean
    @Description("列出当前时间之后开售、按开售时间升序的商品。用于「最近上新」「接下来有哪些现货」等未指定类目的问法；数据来自数据库实时查询。")
    public Function<ListRecentlyAvailableProductsRequest, String> listRecentlyAvailableProducts() {
        return request -> {
            try {
                Integer lim = request != null ? request.limit() : null;
                List<Product> products = aiService.listRecentlyAvailableProducts(lim);
                if (products.isEmpty()) {
                    return AiToolJson.ok("暂无开售时间不早于当前时刻的商品", products);
                }
                return AiToolJson.ok("共 " + products.size() + " 件", products);
            } catch (Exception e) {
                return AiToolJson.fail(e.getMessage() != null ? e.getMessage() : "查询失败");
            }
        };
    }

    @Bean
    @Description("取消订单。需订单 ID 与姓名校验通过；预计发货前 48 小时内可能不可取消（以业务规则为准）。")
    public Function<CancelProductOrderRequest, String> cancelProductOrder() {
        return request -> {
            try {
                aiService.cancelProductOrder(request.orderId, request.name);
                return AiToolJson.ok("订单已取消", request.orderId);
            } catch (Exception e) {
                return AiToolJson.fail(e.getMessage() != null ? e.getMessage() : "取消失败");
            }
        };
    }

    @Bean
    @Description("按订单 ID 查询订单与商品详情，用于回答用户或售后前核对。")
    public Function<ProductOrderDetailRequest, String> getProductOrderDetail() {
        return request -> {
            try {
                ProductOrderDetail d = aiService.getProductOrderDetail(request.orderId);
                return AiToolJson.ok("查询成功", d);
            } catch (Exception e) {
                return AiToolJson.fail(e.getMessage() != null ? e.getMessage() : "订单查询失败");
            }
        };
    }

    @Bean
    @Description("按用户真实姓名查询该用户名下未取消的订单列表。")
    public Function<BuyerProductOrdersRequest, String> getBuyerProductOrders() {
        return request -> {
            try {
                List<ProductOrderDetail> list = aiService.getProductOrdersByName(request.realName);
                return AiToolJson.ok("共 " + list.size() + " 条订单", list);
            } catch (Exception e) {
                return AiToolJson.fail(e.getMessage() != null ? e.getMessage() : "未找到订单");
            }
        };
    }

    @Bean
    @Description("创建商品订单。userId 须为当前登录用户；productId 来自商品查询；可先确认规格再下单。")
    public Function<CreateProductOrderRequest, String> createProductOrder() {
        return request -> {
            try {
                ProductOrder order = aiService.createProductOrder(
                        request.userId, request.productId, request.specOption, request.name);
                return AiToolJson.ok("下单成功", order);
            } catch (Exception e) {
                return AiToolJson.fail(e.getMessage() != null ? e.getMessage() : "创建订单失败");
            }
        };
    }

    @Bean
    @Description("按 SKU 查询单个商品。")
    public Function<ProductBySkuRequest, String> getProductBySku() {
        return request -> {
            try {
                Product p = aiService.getProductBySku(request.sku);
                return AiToolJson.ok("查询成功", p);
            } catch (Exception e) {
                return AiToolJson.fail(e.getMessage() != null ? e.getMessage() : "未找到商品");
            }
        };
    }

    @Bean
    @Description("按类目、发货地与日期搜索商品列表；日期必须为 yyyy-MM-dd。")
    public Function<SearchProductsRequest, String> searchProducts() {
        return request -> {
            try {
                List<Product> products = aiService.searchProducts(
                        request.category, request.shipFrom, request.date);
                return AiToolJson.ok("共 " + products.size() + " 个商品", products);
            } catch (Exception e) {
                return AiToolJson.fail(e.getMessage() != null ? e.getMessage() : "搜索无结果");
            }
        };
    }

    @Bean
    @Description("按品牌名称筛选商品列表。")
    public Function<ProductsByBrandRequest, String> getProductsByBrand() {
        return request -> {
            try {
                List<Product> products = aiService.getProductsByBrand(request.brand);
                return AiToolJson.ok("共 " + products.size() + " 个商品", products);
            } catch (Exception e) {
                return AiToolJson.fail(e.getMessage() != null ? e.getMessage() : "未找到商品");
            }
        };
    }
}
