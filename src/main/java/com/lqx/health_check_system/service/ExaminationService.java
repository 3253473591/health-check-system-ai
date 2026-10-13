package com.lqx.health_check_system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lqx.health_check_system.entity.Examination;
import java.util.Map;

public interface ExaminationService {

    Map<String, Object> register(Examination exam, Long doctorId);

    Page<Map<String, Object>> listExams(int page, int size,
                                        String status, String realName,
                                        String idNumber, Long doctorId);

    Map<String, Object> getDetail(Long examId);
}