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
@TableName("ticket_tier")
public class TicketTier {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long activityId;

    private String ticketCode;

    private String tierName;

    private BigDecimal price;

    private BigDecimal originalPrice;

    private Integer capacity;

    private Integer isDefault;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
