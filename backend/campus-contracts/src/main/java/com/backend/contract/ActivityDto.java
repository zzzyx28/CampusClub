package com.backend.contract;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class ActivityDto {

    private Long id;

    private String activityCode;

    private String title;

    private String clubName;

    private String activityType;

    private String description;

    private String venue;

    private LocalDateTime registrationStart;

    private LocalDateTime eventStart;

    private LocalDateTime eventEnd;

    private String posterUrl;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private List<TicketTierDto> tierList;
}
