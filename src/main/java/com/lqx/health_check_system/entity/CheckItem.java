package com.lqx.health_check_system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("check_item")
public class CheckItem {

    @TableId(type = IdType.AUTO)
    private Long itemId;

    private String itemCode;

    private String itemName;

    private String unit;

    private String refRange;

    private String category;

    private Integer isActive;
}