package com.lqx.health_check_system.dto;

import lombok.Data;
import java.util.List;

@Data
public class ImportResultDTO {
    private Integer total;
    private Integer successCount;
    private Integer failCount;
    private List<FailDetail> failDetails;
    private Integer morningCount;
    private Integer afternoonCount;
}