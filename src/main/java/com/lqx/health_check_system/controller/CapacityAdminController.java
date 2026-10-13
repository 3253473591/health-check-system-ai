package com.lqx.health_check_system.controller;

import com.lqx.health_check_system.common.Result;
import com.lqx.health_check_system.dto.CapacityConfigRequest;
import com.lqx.health_check_system.dto.CapacityConfigVO;
import com.lqx.health_check_system.dto.CapacityTodayVO;
import com.lqx.health_check_system.service.CapacityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/capacity")
@RequiredArgsConstructor
public class CapacityAdminController {

    private final CapacityService capacityService;

    @GetMapping("/today")
    public Result<CapacityConfigVO> getTodayConfig() {
        return Result.success(capacityService.getTodayConfig());
    }

    @GetMapping("/config/today")
    public Result<CapacityConfigVO> getTodayConfigLegacy() {
        return Result.success(capacityService.getTodayConfig());
    }

    @GetMapping("/{date}")
    public Result<CapacityTodayVO> getCapacityByDate(@PathVariable String date) {
        LocalDate localDate = LocalDate.parse(date);
        return Result.success(capacityService.getCapacityByDate(localDate));
    }

    @GetMapping("/config/{date}")
    public Result<CapacityConfigVO> getConfigByDate(@PathVariable String date) {
        LocalDate localDate = LocalDate.parse(date);
        return Result.success(capacityService.getConfigByDate(localDate));
    }

    @PostMapping("/config")
    public Result<Void> saveConfig(@RequestBody CapacityConfigRequest req) {
        capacityService.saveConfig(req);
        return Result.success();
    }
}