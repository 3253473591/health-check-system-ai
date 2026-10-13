package com.lqx.health_check_system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lqx.health_check_system.entity.*;
import com.lqx.health_check_system.mapper.*;
import com.lqx.health_check_system.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ExaminationMapper  examinationMapper;
    private final CheckPackageMapper checkPackageMapper;
    private final PackageItemMapper  packageItemMapper;
    private final CheckItemMapper    checkItemMapper;
    private final PhysicalDataMapper physicalDataMapper;
    private final FinalReportMapper  finalReportMapper;
    private final SysUserMapper      sysUserMapper;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Map<String, Object> getReportPageData(Long examId) {
        Examination exam = examinationMapper.selectById(examId);
        if (exam == null) throw new RuntimeException("体检记录不存在");

        String status = exam.getStatus();
        if ("PENDING".equals(status) || "IN_PROGRESS".equals(status)) {
            throw new RuntimeException("体检记录尚未锁定，不可进入报告编辑页");
        }

        Map<String, Object> data = new HashMap<>();

        data.put("examId",   exam.getExamId());
        data.put("realName", exam.getRealName());
        data.put("idNumber", exam.getIdNumber());
        data.put("phone",    exam.getPhone());
        data.put("status",   status);
        data.put("gender",   parseGender(exam.getIdNumber()));
        data.put("age",      parseAge(exam.getIdNumber()));

        CheckPackage pkg = checkPackageMapper.selectById(exam.getPackageId());
        data.put("packageName", pkg != null ? pkg.getPackageName() : "");

        List<String> itemNames = new ArrayList<>();
        List<Long> pkgItemIds = packageItemMapper.selectItemIdsByPackageId(exam.getPackageId());
        if (pkgItemIds != null && !pkgItemIds.isEmpty()) {
            List<CheckItem> pkgItems = checkItemMapper.selectBatchIds(pkgItemIds);
            pkgItems.forEach(i -> itemNames.add(i.getItemName()));
        }
        data.put("packageItemNames", itemNames);

        List<PhysicalData> pdList = physicalDataMapper.selectList(
                new LambdaQueryWrapper<PhysicalData>()
                        .eq(PhysicalData::getExamId, examId)
                        .orderByAsc(PhysicalData::getItemId));

        Map<Long, CheckItem> itemMap = new HashMap<>();
        if (!pdList.isEmpty()) {
            List<Long> pdItemIds = new ArrayList<>();
            pdList.forEach(pd -> pdItemIds.add(pd.getItemId()));
            List<CheckItem> ciList = checkItemMapper.selectBatchIds(pdItemIds);
            ciList.forEach(ci -> itemMap.put(ci.getItemId(), ci));
        }

        List<Map<String, Object>> tableRows = new ArrayList<>();
        for (PhysicalData pd : pdList) {
            CheckItem ci = itemMap.get(pd.getItemId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("itemName",  ci != null ? ci.getItemName() : "未知项目");
            row.put("itemValue", pd.getItemValue());
            row.put("unit",      pd.getUnit() != null ? pd.getUnit()
                    : (ci != null ? ci.getUnit() : ""));
            row.put("refRange",  ci != null ? ci.getRefRange() : "");
            row.put("abnormal",  calcAbnormal(pd.getItemValue(),
                    ci != null ? ci.getRefRange() : null));
            tableRows.add(row);
        }
        data.put("dataTable", tableRows);

        FinalReport report = findReport(examId);
        data.put("conclusion",  report != null ? report.getConclusion() : "");
        data.put("advice",      report != null ? report.getAdvice()     : "");
        data.put("publishTime", report != null && report.getPublishTime() != null
                ? report.getPublishTime().format(FMT) : null);
        data.put("doctorSign",  report != null && report.getDoctorSign() != null
                ? report.getDoctorSign() : "");

        return data;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveReport(Long examId, Long doctorId,
                              String conclusion, String advice) {
        Examination exam = examinationMapper.selectById(examId);
        if (exam == null) throw new RuntimeException("体检记录不存在");
        if (!"LOCKED".equals(exam.getStatus())) {
            throw new RuntimeException("只有 LOCKED 状态的记录才能保存报告");
        }

        FinalReport report = findReport(examId);
        if (report == null) {
            report = new FinalReport();
            report.setExamId(examId);
            report.setDoctorId(doctorId);
            report.setConclusion(conclusion != null ? conclusion : "");
            report.setAdvice(advice != null ? advice : "");
            report.setDoctorSign(getDoctorSign(doctorId));
            finalReportMapper.insert(report);
        } else {
            if (report.getPublishTime() != null) {
                throw new RuntimeException("报告已发布，不可再修改");
            }
            report.setConclusion(conclusion != null ? conclusion : "");
            report.setAdvice(advice != null ? advice : "");
            finalReportMapper.updateById(report);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> publishReport(Long examId, Long doctorId,
                                             String conclusion, String advice) {
        Examination exam = examinationMapper.selectById(examId);
        if (exam == null) throw new RuntimeException("体检记录不存在");
        if (!"LOCKED".equals(exam.getStatus())) {
            throw new RuntimeException("只有 LOCKED 状态的记录才能发布报告");
        }

        FinalReport report = findReport(examId);
        LocalDateTime now  = LocalDateTime.now();
        String sign        = getDoctorSign(doctorId);

        if (report == null) {
            report = new FinalReport();
            report.setExamId(examId);
            report.setDoctorId(doctorId);
            report.setConclusion(conclusion != null ? conclusion : "");
            report.setAdvice(advice != null ? advice : "");
            report.setDoctorSign(sign);
            report.setPublishTime(now);
            finalReportMapper.insert(report);
        } else {
            report.setDoctorId(doctorId);
            report.setConclusion(conclusion != null ? conclusion : report.getConclusion());
            report.setAdvice(advice != null ? advice : report.getAdvice());
            report.setDoctorSign(sign);
            report.setPublishTime(now);
            finalReportMapper.updateById(report);
        }

        Examination upd = new Examination();
        upd.setExamId(examId);
        upd.setStatus("PUBLISHED");
        examinationMapper.updateById(upd);

        Map<String, Object> res = new HashMap<>();
        res.put("examId",      examId);
        res.put("status",      "PUBLISHED");
        res.put("publishTime", now.format(FMT));
        res.put("doctorSign",  sign);
        return res;
    }

    private FinalReport findReport(Long examId) {
        return finalReportMapper.selectOne(
                new LambdaQueryWrapper<FinalReport>()
                        .eq(FinalReport::getExamId, examId));
    }

    private String getDoctorSign(Long doctorId) {
        SysUser u = sysUserMapper.selectById(doctorId);
        return u != null ? u.getRealName() : "";
    }

    private String parseGender(String idNumber) {
        if (idNumber == null || idNumber.length() < 17) return "";
        int code = Character.getNumericValue(idNumber.charAt(16));
        return code % 2 == 1 ? "男" : "女";
    }

    private String parseAge(String idNumber) {
        if (idNumber == null || idNumber.length() < 14) return "";
        try {
            int birthYear  = Integer.parseInt(idNumber.substring(6, 10));
            int birthMonth = Integer.parseInt(idNumber.substring(10, 12));
            int birthDay   = Integer.parseInt(idNumber.substring(12, 14));
            LocalDateTime now = LocalDateTime.now();
            int age = now.getYear() - birthYear;
            if (now.getMonthValue() < birthMonth
                    || (now.getMonthValue() == birthMonth
                    && now.getDayOfMonth() < birthDay)) {
                age--;
            }
            return String.valueOf(age);
        } catch (Exception e) {
            return "";
        }
    }

    private String calcAbnormal(String value, String refRange) {
        if (value == null || refRange == null || refRange.isEmpty()) return "—";
        try {
            String range  = refRange.split("/")[0].trim();
            String[] parts = range.split("-");
            if (parts.length != 2) return "—";
            double min = Double.parseDouble(parts[0].trim());
            double max = Double.parseDouble(parts[1].trim());
            double v   = Double.parseDouble(value.trim());
            if (v < min) return "低";
            if (v > max) return "高";
            return "无异常";
        } catch (Exception e) {
            return "—";
        }
    }
}