package com.lqx.health_check_system.dto;

import lombok.Data;
import java.util.List;

@Data
public class CapacityConfigVO {

    private String capacityDate;
    private Integer totalCapacity;
    private Integer morningCapacity;
    private Integer afternoonCapacity;
    private List<DoctorConfigItem> doctorCapacities;

    @Data
    public static class DoctorConfigItem {
        private Long doctorId;
        private String doctorName;
        private Integer morning;
        private Integer afternoon;
    }
}