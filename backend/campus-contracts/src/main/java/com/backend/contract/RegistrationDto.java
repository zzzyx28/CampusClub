package com.backend.contract;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class RegistrationDto {

    private Long id;

    private Long userId;

    private Long activityId;

    private Long ticketTierId;

    private Integer ticketCount;

    private BigDecimal unitPrice;

    private BigDecimal totalPrice;

    /** 1-已报名，2-已取消，3-已参加 */
    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private UserDto user;

    private ActivityDto activity;
}
