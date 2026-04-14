package com.backend.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductOrderDetail {
    private Long orderId;
    private String sku;
    private String specOption;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private Integer status;
    private LocalDateTime createdAt;

    private Long userId;
    private String username;
    private String realName;
    private String phone;

    private Long productId;
    private String spu;
    private String productName;
    private String category;
    private String brand;
}
