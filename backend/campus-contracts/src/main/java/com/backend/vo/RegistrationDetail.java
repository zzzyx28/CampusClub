package com.backend.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationDetail {
    private Long registrationId;
    private String ticketCode;
    private String tierName;
    private Integer ticketCount;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private Integer status;
    private LocalDateTime createdAt;

    private Long userId;
    private String username;
    private String realName;
    private String phone;

    private Long activityId;
    private String activityCode;
    private String activityTitle;
    private String activityType;
    private String clubName;
    private String venue;
    private LocalDateTime registrationStart;
    private LocalDateTime eventStart;
    private LocalDateTime eventEnd;
}
