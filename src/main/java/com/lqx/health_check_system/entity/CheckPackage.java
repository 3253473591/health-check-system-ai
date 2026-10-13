package com.lqx.health_check_system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@TableName("check_package")
public class CheckPackage {

    @TableId(type = IdType.AUTO)
    private Long packageId;

    private String packageName;

    private BigDecimal price;

    private String description;

    private Integer isActive;
}