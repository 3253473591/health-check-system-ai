package com.lqx.health_check_system.service;

import java.util.Map;

public interface ReportService {

    Map<String, Object> getReportPageData(Long examId);

    boolean saveReport(Long examId, Long doctorId, String conclusion, String advice);

    Map<String, Object> publishReport(Long examId, Long doctorId,
                                      String conclusion, String advice);
}