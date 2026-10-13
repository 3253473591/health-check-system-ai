package com.lqx.health_check_system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lqx.health_check_system.entity.CheckPackage;
import com.lqx.health_check_system.entity.Examination;
import com.lqx.health_check_system.entity.PhysicalData;
import com.lqx.health_check_system.entity.SysUser;
import com.lqx.health_check_system.mapper.CheckPackageMapper;
import com.lqx.health_check_system.mapper.ExaminationMapper;
import com.lqx.health_check_system.mapper.PhysicalDataMapper;
import com.lqx.health_check_system.mapper.SysUserMapper;
import com.lqx.health_check_system.service.ExaminationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ExaminationServiceImpl implements ExaminationService {

    private final ExaminationMapper  examinationMapper;
    private final CheckPackageMapper checkPackageMapper;
    private final SysUserMapper      sysUserMapper;
    private final PhysicalDataMapper physicalDataMapper;

    @Override
    public Map<String, Object> register(Examination exam, Long doctorId) {
        exam.setRegisterDoctorId(doctorId);
        exam.setStatus("PENDING");
        exam.setSource("INDIVIDUAL");
        examinationMapper.insert(exam);
        Map<String, Object> res = new HashMap<>();
        res.put("examId", exam.getExamId());
        res.put("status", exam.getStatus());
        return res;
    }

    @Override
    public Page<Map<String, Object>> listExams(int page, int size,
                                               String status, String realName,
                                               String idNumber, Long doctorId) {
        LambdaQueryWrapper<Examination> wrapper = new LambdaQueryWrapper<Examination>()
                .eq(Examination::getRegisterDoctorId, doctorId)
                .eq(StringUtils.hasText(status),   Examination::getStatus,   status)
                .like(StringUtils.hasText(realName),  Examination::getRealName, realName)
                .like(StringUtils.hasText(idNumber),  Examination::getIdNumber, idNumber)
                .orderByAsc(Examination::getExamId);

        Page<Examination> examPage = examinationMapper.selectPage(new Page<>(page, size), wrapper);
        Page<Map<String, Object>> result = new Page<>(
                examPage.getCurrent(), examPage.getSize(), examPage.getTotal());

        List<Map<String, Object>> records = new ArrayList<>();
        for (Examination e : examPage.getRecords()) {
            Map<String, Object> row = toMap(e);
            CheckPackage pkg = checkPackageMapper.selectById(e.getPackageId());
            row.put("packageName", pkg != null ? pkg.getPackageName() : "");
            records.add(row);
        }
        result.setRecords(records);
        return result;
    }

    @Override
    public Map<String, Object> getDetail(Long examId) {
        Examination exam = examinationMapper.selectById(examId);
        if (exam == null) throw new RuntimeException("体检记录不存在");
        Map<String, Object> detail = toMap(exam);

        CheckPackage pkg = checkPackageMapper.selectById(exam.getPackageId());
        detail.put("packageName",  pkg != null ? pkg.getPackageName() : "");
        detail.put("packagePrice", pkg != null ? pkg.getPrice()       : null);
        detail.put("packageId",    exam.getPackageId());

        SysUser doctor = sysUserMapper.selectById(exam.getRegisterDoctorId());
        detail.put("doctorName", doctor != null ? doctor.getRealName() : "");

        List<PhysicalData> dataList = physicalDataMapper.selectList(
                new LambdaQueryWrapper<PhysicalData>()
                        .eq(PhysicalData::getExamId, examId)
                        .orderByAsc(PhysicalData::getItemId));
        detail.put("physicalDataList", dataList);
        return detail;
    }

    private Map<String, Object> toMap(Examination e) {
        Map<String, Object> m = new HashMap<>();
        m.put("examId",           e.getExamId());
        m.put("realName",         e.getRealName());
        m.put("idNumber",         e.getIdNumber());
        m.put("phone",            e.getPhone());
        m.put("packageId",        e.getPackageId());
        m.put("registerDoctorId", e.getRegisterDoctorId());
        m.put("registerTime",     e.getRegisterTime() != null
                ? e.getRegisterTime().toString() : "");
        m.put("status",           e.getStatus());
        m.put("lockedTime",       e.getLockedTime() != null
                ? e.getLockedTime().toString() : null);
        m.put("source",           e.getSource());
        return m;
    }
}