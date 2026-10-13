package com.lqx.health_check_system.service;

import com.lqx.health_check_system.dto.CapacityConfigRequest;
import com.lqx.health_check_system.dto.CapacityConfigVO;
import com.lqx.health_check_system.dto.CapacityTodayVO;

import java.time.LocalDate;

public interface CapacityService {

    CapacityTodayVO getTodayCapacity();

    CapacityTodayVO getCapacityByDate(LocalDate date);

    CapacityConfigVO getTodayConfig();

    CapacityConfigVO getConfigByDate(LocalDate date);

    void saveConfig(CapacityConfigRequest req);
}