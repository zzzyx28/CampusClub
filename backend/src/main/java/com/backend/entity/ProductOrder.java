package com.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品订单实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("product_order")
public class ProductOrder {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long productId;

    private Long productSkuId;

    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal totalPrice;

    /**
     * 订单状态：1-进行中，2-已取消，3-已完成
     */
    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private User user;

    @TableField(exist = false)
    private Product product;
}
