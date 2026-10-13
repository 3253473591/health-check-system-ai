package com.lqx.health_check_system.dto;

import lombok.Data;
import java.util.List;

@Data
public class CapacityConfigRequest {

    private String capacityDate;
    private Integer totalCapacity;

    private List<DoctorCapacity> doctorCapacities;

    @Data
    public static class DoctorCapacity {
        private Long doctorId;
        private Integer morning;
        private Integer afternoon;
    }
}