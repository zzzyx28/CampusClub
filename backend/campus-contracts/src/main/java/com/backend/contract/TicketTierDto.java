package com.backend.contract;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class TicketTierDto {

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
