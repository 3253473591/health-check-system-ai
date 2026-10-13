package com.lqx.health_check_system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lqx.health_check_system.common.Result;
import com.lqx.health_check_system.entity.Examination;
import com.lqx.health_check_system.service.ExaminationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/doctor/examinations")
@RequiredArgsConstructor
public class ExaminationController {

    private final ExaminationService examinationService;

    @PostMapping
    public Result<Map<String, Object>> register(
            @RequestBody Examination exam,
            HttpServletRequest req) {
        Long doctorId = getDoctorId(req);
        return Result.success(examinationService.register(exam, doctorId));
    }

    @GetMapping
    public Result<Page<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1")  int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false)    String status,
            @RequestParam(required = false)    String realName,
            @RequestParam(required = false)    String idNumber,
            HttpServletRequest req) {
        Long doctorId = getDoctorId(req);
        return Result.success(
                examinationService.listExams(page, size, status, realName, idNumber, doctorId));
    }

    @GetMapping("/{examId}")
    public Result<Map<String, Object>> detail(@PathVariable Long examId) {
        return Result.success(examinationService.getDetail(examId));
    }

    private Long getDoctorId(HttpServletRequest req) {
        String token = req.getHeader("Authorization");
        if (token == null) throw new RuntimeException("未登录");
        String[] parts = token.split("-");
        return Long.parseLong(parts[2]);
    }
}