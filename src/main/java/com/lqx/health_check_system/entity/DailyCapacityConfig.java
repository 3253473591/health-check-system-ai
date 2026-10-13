package com.lqx.health_check_system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("daily_capacity_config")
public class DailyCapacityConfig {

    @TableId(type = IdType.AUTO)
    private Long configId;

    private LocalDate capacityDate;

    private Integer totalCapacity;

    private Integer morningCapacity;

    private Integer afternoonCapacity;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}