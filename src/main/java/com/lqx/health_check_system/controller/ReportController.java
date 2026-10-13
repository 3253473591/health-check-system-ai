package com.lqx.health_check_system.controller;

import com.lqx.health_check_system.common.Result;
import com.lqx.health_check_system.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/doctor/examinations")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/{examId}/report")
    public Result<Map<String, Object>> getReportData(@PathVariable Long examId) {
        return Result.success(reportService.getReportPageData(examId));
    }

    @PostMapping("/{examId}/report/save")
    public Result<Boolean> saveReport(
            @PathVariable Long examId,
            @RequestBody Map<String, String> body,
            HttpServletRequest req) {
        Long doctorId = parseDoctorId(req);
        return Result.success(reportService.saveReport(
                examId, doctorId,
                body.get("conclusion"),
                body.get("advice")));
    }

    @PutMapping("/{examId}/report/publish")
    public Result<Map<String, Object>> publishReport(
            @PathVariable Long examId,
            @RequestBody Map<String, String> body,
            HttpServletRequest req) {
        Long doctorId = parseDoctorId(req);
        return Result.success(reportService.publishReport(
                examId, doctorId,
                body.get("conclusion"),
                body.get("advice")));
    }

    private Long parseDoctorId(HttpServletRequest req) {
        String token = req.getHeader("Authorization");
        if (token == null) throw new RuntimeException("未登录");
        return Long.parseLong(token.split("-")[2]);
    }
}