package com.lqx.health_check_system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("final_report")
public class FinalReport {

    @TableId(type = IdType.AUTO)
    private Long reportId;

    private Long examId;

    private Long doctorId;

    private String conclusion;

    private String advice;

    private String doctorSign;

    private LocalDateTime publishTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}