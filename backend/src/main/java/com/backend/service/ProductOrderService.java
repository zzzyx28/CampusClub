package com.backend.service;

import com.backend.entity.ProductOrder;
import com.backend.vo.ProductOrderDetail;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface ProductOrderService extends IService<ProductOrder> {

    List<ProductOrder> listByUserId(Long userId);

    List<ProductOrder> listByProductId(Long productId);

    ProductOrder createOrder(Long userId, Long productId, String specOption);

    boolean cancelOrder(Long orderId);

    boolean completeOrder(Long orderId);

    List<ProductOrder> listOrdersWithDetail(Long userId);

    ProductOrderDetail getOrderDetailById(Long id);

    List<ProductOrderDetail> getProductOrderDetailsByName(String realName);
}
