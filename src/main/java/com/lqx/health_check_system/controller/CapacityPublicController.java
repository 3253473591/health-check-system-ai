package com.lqx.health_check_system.controller;

import com.lqx.health_check_system.common.Result;
import com.lqx.health_check_system.dto.CapacityTodayVO;
import com.lqx.health_check_system.service.CapacityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/public/capacity")
@RequiredArgsConstructor
public class CapacityPublicController {

    private final CapacityService capacityService;

    @GetMapping("/today")
    public Result<CapacityTodayVO> today() {
        return Result.success(capacityService.getTodayCapacity());
    }

    @GetMapping("/{date}")
    public Result<CapacityTodayVO> getByDate(@PathVariable String date) {
        LocalDate localDate = LocalDate.parse(date);
        return Result.success(capacityService.getCapacityByDate(localDate));
    }
}