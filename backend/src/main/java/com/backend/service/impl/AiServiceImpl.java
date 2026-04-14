package com.backend.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.backend.entity.Product;
import com.backend.entity.ProductOrder;
import com.backend.service.AiService;
import com.backend.service.ProductOrderService;
import com.backend.service.ProductService;
import com.backend.vo.ProductOrderDetail;

@Service
public class AiServiceImpl implements AiService {

    @Autowired
    private ProductOrderService productOrderService;

    @Autowired
    private ProductService productService;

    @Override
    public void cancelProductOrder(Long orderId, String name) {
        ProductOrderDetail detail = productOrderService.getOrderDetailById(orderId);
        if (detail == null) {
            throw new RuntimeException("订单不存在");
        }
        if (detail.getRealName().equals(name)) {
            boolean result = productOrderService.cancelOrder(orderId);
            if (!result) {
                throw new RuntimeException("取消订单失败");
            }
        } else {
            throw new RuntimeException("姓名不匹配，无法取消订单");
        }
    }

    @Override
    public ProductOrder createProductOrder(Long userId, Long productId, String specOption, String name) {
        if (userId == null || productId == null) {
            throw new RuntimeException("用户ID和商品ID不能为空");
        }
        ProductOrder order = productOrderService.createOrder(userId, productId, specOption);
        if (order == null) {
            throw new RuntimeException("创建订单失败");
        }
        return order;
    }

    @Override
    public ProductOrderDetail getProductOrderDetail(Long orderId) {
        if (orderId == null) {
            throw new RuntimeException("订单ID不能为空");
        }
        ProductOrderDetail detail = productOrderService.getOrderDetailById(orderId);
        if (detail == null) {
            throw new RuntimeException("订单不存在");
        }
        return detail;
    }

    @Override
    public List<ProductOrderDetail> getProductOrdersByName(String realName) {
        if (realName == null || realName.trim().isEmpty()) {
            throw new RuntimeException("用户姓名不能为空");
        }
        List<ProductOrderDetail> orders = productOrderService.getProductOrderDetailsByName(realName);
        if (orders == null || orders.isEmpty()) {
            throw new RuntimeException("未找到该用户的订单");
        }
        return orders;
    }

    @Override
    public Product getProductBySku(String sku) {
        if (sku == null || sku.trim().isEmpty()) {
            throw new RuntimeException("SKU 不能为空");
        }
        Product product = productService.getBySku(sku);
        if (product == null) {
            throw new RuntimeException("未找到该商品");
        }
        return product;
    }

    @Override
    public List<Product> searchProducts(String category, String shipFrom, String dateStr) {
        if (category == null || category.trim().isEmpty()) {
            throw new RuntimeException("类目不能为空");
        }
        String keyword = shipFrom == null ? "" : shipFrom.trim();
        LocalDateTime startTime = LocalDateTime.now().minusYears(30);
        LocalDateTime endTime = LocalDateTime.now().plusYears(30);

        List<Product> products = productService.searchProducts(
                category.trim(), keyword, startTime, endTime);

        if (products == null || products.isEmpty()) {
            throw new RuntimeException("未找到符合条件的商品");
        }
        return products;
    }

    @Override
    public List<Product> getProductsByBrand(String brand) {
        if (brand == null || brand.trim().isEmpty()) {
            throw new RuntimeException("品牌名称不能为空");
        }
        List<Product> products = productService.listByBrand(brand.trim());
        if (products == null || products.isEmpty()) {
            throw new RuntimeException("未找到该品牌的商品");
        }
        return products;
    }

    @Override
    public List<Product> listRecentlyAvailableProducts(Integer limit) {
        int n = limit == null ? 5 : limit;
        List<Product> products = productService.listRecentlyAvailable(n);
        return products != null ? products : List.of();
    }
}
