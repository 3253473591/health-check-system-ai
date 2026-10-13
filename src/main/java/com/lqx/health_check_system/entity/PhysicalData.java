package com.lqx.health_check_system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("physical_data")
public class PhysicalData {

    @TableId(type = IdType.AUTO)
    private Long dataId;

    private Long examId;

    private Long itemId;

    private String itemValue;

    private String unit;

    private LocalDateTime recordTime;
}