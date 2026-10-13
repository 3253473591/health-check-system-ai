package com.lqx.health_check_system.service;

import com.lqx.health_check_system.entity.CheckItem;
import com.lqx.health_check_system.entity.PhysicalData;

import java.util.List;
import java.util.Map;

public interface PhysicalDataService {
    List<CheckItem> getPackageItems(Long packageId);

    void saveData(Long examId, Long doctorId, List<PhysicalData> dataList);

    Map<String, Object> lockExam(Long examId);
}