package com.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("product_sku")
public class ProductSku {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long productId;

    private String sku;

    private String specOption;

    private BigDecimal salePrice;

    private BigDecimal marketPrice;

    private Integer stock;

    private Integer isDefault;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
