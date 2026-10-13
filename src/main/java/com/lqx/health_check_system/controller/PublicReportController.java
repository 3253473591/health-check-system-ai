package com.lqx.health_check_system.controller;

import com.lqx.health_check_system.common.Result;
import com.lqx.health_check_system.service.PublicReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/public/report")
@RequiredArgsConstructor
public class PublicReportController {

    private final PublicReportService publicReportService;

    @PostMapping("/query")
    public Result<List<Map<String, Object>>> query(
            @RequestBody Map<String, String> body) {
        String realName = body.get("realName");
        String idNumber = body.get("idNumber");
        return Result.success(publicReportService.queryReports(realName, idNumber));
    }

    @GetMapping("/{reportId}")
    public Result<Map<String, Object>> detail(@PathVariable Long reportId) {
        return Result.success(publicReportService.getReportDetail(reportId));
    }
}