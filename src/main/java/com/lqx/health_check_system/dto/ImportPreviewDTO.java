package com.lqx.health_check_system.dto;

import lombok.Data;
import java.util.List;

@Data
public class ImportPreviewDTO {
    private Integer total;
    private Integer targetDoctorRemaining;
    private Integer targetDoctorMorningRemaining;
    private Integer targetDoctorAfternoonRemaining;
    private Integer allDoctorsRemaining;
    private Integer allDoctorsMorningRemaining;
    private Integer allDoctorsAfternoonRemaining;
    private Boolean hasSpillover;
    private Integer spilloverCount;
    private Boolean noEnoughToday;
    private Integer futureCount;
    private Integer failCount;
    private List<FailDetail> failDetails;
    private String  previewToken;
    private Integer thirtyDayTotalCapacity;
    private Boolean cannotFitIn30Days;
}