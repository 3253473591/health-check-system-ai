package com.lqx.health_check_system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("doctor_capacity_config")
public class DoctorCapacityConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    private LocalDate capacityDate;

    private Long doctorId;

    private Integer morningCapacity;

    private Integer afternoonCapacity;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}