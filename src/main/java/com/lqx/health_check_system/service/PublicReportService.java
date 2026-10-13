package com.lqx.health_check_system.service;

import java.util.List;
import java.util.Map;

public interface PublicReportService {

    List<Map<String, Object>> queryReports(String realName, String idNumber);

    Map<String, Object> getReportDetail(Long reportId);
}