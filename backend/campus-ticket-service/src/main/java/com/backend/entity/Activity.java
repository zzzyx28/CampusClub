package com.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("activity")
public class Activity {

    @TableId(value = "id", type = IdType.AUTO)
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

    @TableField(exist = false)
    private List<TicketTier> tierList;
}
