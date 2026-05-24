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

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("registration")
public class Registration {

    @TableId(value = "id", type = IdType.AUTO)
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

    @TableField(exist = false)
    private User user;

    @TableField(exist = false)
    private Activity activity;
}
