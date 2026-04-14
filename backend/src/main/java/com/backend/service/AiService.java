package com.backend.service;

import java.util.List;

import com.backend.entity.Product;
import com.backend.entity.ProductOrder;
import com.backend.vo.ProductOrderDetail;

public interface AiService {

    void cancelProductOrder(Long orderId, String name);

    ProductOrder createProductOrder(Long userId, Long productId, String specOption, String name);

    ProductOrderDetail getProductOrderDetail(Long orderId);

    List<ProductOrderDetail> getProductOrdersByName(String realName);

    Product getProductBySku(String sku);

    List<Product> searchProducts(String category, String shipFrom, String dateStr);

    List<Product> getProductsByBrand(String brand);

    List<Product> listRecentlyAvailableProducts(Integer limit);
}
