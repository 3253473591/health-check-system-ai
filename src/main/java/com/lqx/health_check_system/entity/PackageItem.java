package com.lqx.health_check_system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("package_item")
public class PackageItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long packageId;

    private Long itemId;
}