package com.lqx.health_check_system.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.lqx.health_check_system.common.AesConverter;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName(value = "examination", autoResultMap = true)
public class Examination {

    @TableId(type = IdType.AUTO)
    private Long examId;

    private String realName;

    @TableField(typeHandler = AesConverter.class)
    private String idNumber;

    @TableField(typeHandler = AesConverter.class)
    private String phone;

    private Long packageId;

    private Long registerDoctorId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime registerTime;

    private String status;

    private LocalDateTime lockedTime;

    private String source;
}