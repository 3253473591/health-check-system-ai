package com.lqx.health_check_system.dto;

import lombok.Data;

@Data
public class ImportRequestDTO {
    private String  previewToken;
    private Long    doctorId;
    private Boolean spilloverToOtherDoctors;
    private Boolean spilloverToFuture;
}