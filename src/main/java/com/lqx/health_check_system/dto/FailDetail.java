package com.lqx.health_check_system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FailDetail {
    private int row;
    private String reason;
}