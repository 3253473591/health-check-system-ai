package com.lqx.health_check_system.dto;

import lombok.Data;
import java.util.List;

@Data
public class CapacityTodayVO {

    private String date;

    private SlotVO total;
    private TimeSlotVO morning;
    private TimeSlotVO afternoon;
    private List<DoctorSlotVO> doctorList;

    @Data
    public static class SlotVO {
        private int capacity;
        private int used;
        private int remaining;
    }

    @Data
    public static class TimeSlotVO {
        private String timeRange;
        private int capacity;
        private int used;
        private int remaining;
    }

    @Data
    public static class DoctorSlotVO {
        private Long doctorId;
        private String doctorName;
        private SlotVO morning;
        private SlotVO afternoon;
    }
}