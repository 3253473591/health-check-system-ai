package com.lqx.health_check_system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lqx.health_check_system.entity.*;
import com.lqx.health_check_system.mapper.*;
import com.lqx.health_check_system.service.PublicReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PublicReportServiceImpl implements PublicReportService {

    private final ExaminationMapper  examinationMapper;
    private final FinalReportMapper  finalReportMapper;
    private final CheckPackageMapper checkPackageMapper;
    private final PackageItemMapper  packageItemMapper;
    private final CheckItemMapper    checkItemMapper;
    private final PhysicalDataMapper physicalDataMapper;
    private final SysUserMapper      sysUserMapper;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public List<Map<String, Object>> queryReports(String realName, String idNumber) {
        if (!StringUtils.hasText(realName) || !StringUtils.hasText(idNumber)) {
            throw new RuntimeException("姓名和身份证号不能为空");
        }

        List<Examination> exams = examinationMapper.selectList(
                new LambdaQueryWrapper<Examination>()
                        .eq(Examination::getRealName, realName.trim())
                        .eq(Examination::getIdNumber, idNumber.trim())
                        .eq(Examination::getStatus, "PUBLISHED")
                        .orderByDesc(Examination::getRegisterTime));

        if (exams == null || exams.isEmpty()) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Examination exam : exams) {
            FinalReport report = finalReportMapper.selectOne(
                    new LambdaQueryWrapper<FinalReport>()
                            .eq(FinalReport::getExamId, exam.getExamId()));
            if (report == null) continue;

            CheckPackage pkg = checkPackageMapper.selectById(exam.getPackageId());

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("reportId",    report.getReportId());
            row.put("examId",      exam.getExamId());
            row.put("realName",    exam.getRealName());
            row.put("packageName", pkg != null ? pkg.getPackageName() : "");
            row.put("publishTime", report.getPublishTime() != null
                    ? report.getPublishTime().format(FMT) : "");
            row.put("status",      "PUBLISHED");
            result.add(row);
        }
        return result;
    }

    @Override
    public Map<String, Object> getReportDetail(Long reportId) {
        FinalReport report = finalReportMapper.selectById(reportId);
        if (report == null) {
            throw new RuntimeException("报告不存在");
        }
        if (report.getPublishTime() == null) {
            throw new RuntimeException("该报告尚未发布");
        }

        Examination exam = examinationMapper.selectById(report.getExamId());
        if (exam == null) {
            throw new RuntimeException("体检记录不存在");
        }
        if (!"PUBLISHED".equals(exam.getStatus())) {
            throw new RuntimeException("该报告未发布，无法查看");
        }

        Map<String, Object> data = new LinkedHashMap<>();

        data.put("reportId",  report.getReportId());
        data.put("examId",    exam.getExamId());
        data.put("realName",  exam.getRealName());
        data.put("idNumber",  exam.getIdNumber());
        data.put("phone",     exam.getPhone());
        data.put("gender",    parseGender(exam.getIdNumber()));
        data.put("age",       parseAge(exam.getIdNumber()));

        CheckPackage pkg = checkPackageMapper.selectById(exam.getPackageId());
        data.put("packageName", pkg != null ? pkg.getPackageName() : "");

        List<String> itemNames = new ArrayList<>();
        List<Long> pkgItemIds = packageItemMapper.selectItemIdsByPackageId(exam.getPackageId());
        if (pkgItemIds != null && !pkgItemIds.isEmpty()) {
            checkItemMapper.selectBatchIds(pkgItemIds)
                    .forEach(ci -> itemNames.add(ci.getItemName()));
        }
        data.put("packageItemNames", itemNames);

        List<PhysicalData> pdList = physicalDataMapper.selectList(
                new LambdaQueryWrapper<PhysicalData>()
                        .eq(PhysicalData::getExamId, exam.getExamId())
                        .orderByAsc(PhysicalData::getItemId));

        Map<Long, CheckItem> itemMap = new HashMap<>();
        if (!pdList.isEmpty()) {
            List<Long> pdItemIds = new ArrayList<>();
            pdList.forEach(pd -> pdItemIds.add(pd.getItemId()));
            checkItemMapper.selectBatchIds(pdItemIds)
                    .forEach(ci -> itemMap.put(ci.getItemId(), ci));
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

        data.put("conclusion",  report.getConclusion());
        data.put("advice",      report.getAdvice());
        data.put("doctorSign",  report.getDoctorSign());
        data.put("publishTime", report.getPublishTime().format(FMT));

        return data;
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
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
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
            String range   = refRange.split("/")[0].trim();
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