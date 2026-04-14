package com.backend.controller;

import com.backend.common.Result;
import com.backend.common.enumeration.ResultCode;
import com.backend.entity.ProductOrder;
import com.backend.service.ProductOrderService;
import com.backend.vo.ProductOrderDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 商品订单
 */
@RestController
@RequestMapping("/api/order")
public class ProductOrderController {

    @Autowired
    private ProductOrderService productOrderService;

    @GetMapping("/{id}")
    public Result getOrderById(@PathVariable Long id) {
        ProductOrder order = productOrderService.getById(id);
        if (order == null) {
            return Result.error(ResultCode.NOT_FOUND, "订单不存在");
        }
        return Result.success(order);
    }

    @GetMapping("/{id}/detail")
    public Result getOrderDetailById(@PathVariable Long id) {
        ProductOrderDetail orderDetail = productOrderService.getOrderDetailById(id);
        if (orderDetail == null) {
            return Result.error(ResultCode.NOT_FOUND, "订单不存在");
        }
        return Result.success(orderDetail);
    }

    @GetMapping("/user/{userId}")
    public Result listOrdersByUserId(@PathVariable Long userId) {
        List<ProductOrder> orders = productOrderService.listByUserId(userId);
        return Result.success(orders);
    }

    @GetMapping("/user/{userId}/detail")
    public Result listOrdersWithDetail(@PathVariable Long userId) {
        List<ProductOrder> orders = productOrderService.listOrdersWithDetail(userId);
        return Result.success(orders);
    }

    @GetMapping("/product/{productId}")
    public Result listOrdersByProductId(@PathVariable Long productId) {
        List<ProductOrder> orders = productOrderService.listByProductId(productId);
        return Result.success(orders);
    }

    @PostMapping
    public Result createOrder(@RequestBody Map<String, Object> orderMap) {
        if (orderMap.get("userId") == null || orderMap.get("productId") == null) {
            return Result.error(ResultCode.PARAM_ERROR, "用户ID与商品ID不能为空");
        }
        Long userId = Long.valueOf(orderMap.get("userId").toString());
        Long productId = Long.valueOf(orderMap.get("productId").toString());
        Object specOpt = orderMap.get("specOption");
        String specOption = specOpt != null ? specOpt.toString() : null;

        ProductOrder order = productOrderService.createOrder(userId, productId, specOption);
        if (order != null) {
            return Result.success("订单创建成功", order);
        }
        return Result.error("订单创建失败");
    }

    @PutMapping("/{id}/cancel")
    public Result cancelOrder(@PathVariable Long id) {
        ProductOrder order = productOrderService.getById(id);
        if (order == null) {
            return Result.error(ResultCode.NOT_FOUND, "订单不存在");
        }

        boolean success = productOrderService.cancelOrder(id);
        if (success) {
            return Result.success("订单取消成功", null);
        }
        return Result.error("订单取消失败");
    }

    @PutMapping("/{id}/complete")
    public Result completeOrder(@PathVariable Long id) {
        ProductOrder order = productOrderService.getById(id);
        if (order == null) {
            return Result.error(ResultCode.NOT_FOUND, "订单不存在");
        }
        boolean success = productOrderService.completeOrder(id);
        if (success) {
            return Result.success("订单已完成", null);
        }
        return Result.error("更新订单状态失败");
    }

    @GetMapping("/admin/list")
    public Result listAllOrders() {
        List<ProductOrder> orders = productOrderService.list();
        return Result.success(orders);
    }
}
