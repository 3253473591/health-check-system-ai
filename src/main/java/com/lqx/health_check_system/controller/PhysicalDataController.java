package com.lqx.health_check_system.controller;

import com.lqx.health_check_system.common.Result;
import com.lqx.health_check_system.entity.CheckItem;
import com.lqx.health_check_system.entity.PhysicalData;
import com.lqx.health_check_system.service.PhysicalDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class PhysicalDataController {

    private final PhysicalDataService physicalDataService;

    @GetMapping("/api/doctor/packages/{packageId}/items")
    public Result<List<CheckItem>> getPackageItems(@PathVariable Long packageId) {
        return Result.success(physicalDataService.getPackageItems(packageId));
    }

    @PostMapping("/api/doctor/examinations/{examId}/data")
    public Result<Boolean> saveData(
            @PathVariable Long examId,
            @RequestBody List<PhysicalData> dataList,
            HttpServletRequest req) {
        Long doctorId = parseDoctorId(req);
        physicalDataService.saveData(examId, doctorId, dataList);
        return Result.success(true);
    }

    @PutMapping("/api/doctor/examinations/{examId}/lock")
    public Result<Map<String, Object>> lockExam(@PathVariable Long examId) {
        return Result.success(physicalDataService.lockExam(examId));
    }

    private Long parseDoctorId(HttpServletRequest req) {
        String token = req.getHeader("Authorization");
        if (token == null) throw new RuntimeException("未登录");
        return Long.parseLong(token.split("-")[2]);
    }
}