package com.lqx.health_check_system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lqx.health_check_system.dto.*;
import com.lqx.health_check_system.entity.*;
import com.lqx.health_check_system.mapper.*;
import com.lqx.health_check_system.service.CapacityService;
import com.lqx.health_check_system.service.HrService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HrServiceImpl implements HrService {

    private final CheckPackageMapper checkPackageMapper;
    private final ExaminationMapper examinationMapper;
    private final SysUserMapper sysUserMapper;
    private final DailyCapacityConfigMapper dailyMapper;
    private final DoctorCapacityConfigMapper doctorCapacityMapper;
    private final CapacityService capacityService;

    private static final Pattern ID_PATTERN = Pattern.compile(
            "^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx]$");

    private final Map<String, List<Examination>> previewCache = new ConcurrentHashMap<>();

    @Override
    public void downloadTemplate(HttpServletResponse response) {
        List<CheckPackage> pkgList = checkPackageMapper.selectList(
                new LambdaQueryWrapper<CheckPackage>()
                        .eq(CheckPackage::getIsActive, 1)
                        .orderByAsc(CheckPackage::getPackageId));

        String[] pkgNames = pkgList.stream()
                .map(CheckPackage::getPackageName)
                .toArray(String[]::new);

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet sheet = wb.createSheet("体检人员导入模板");
            sheet.setColumnWidth(0, 5000);
            sheet.setColumnWidth(1, 7000);
            sheet.setColumnWidth(2, 5000);
            sheet.setColumnWidth(3, 7000);

            CellStyle headerStyle = wb.createCellStyle();
            XSSFFont headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            CellStyle textStyle = wb.createCellStyle();
            DataFormat fmt = wb.createDataFormat();
            textStyle.setDataFormat(fmt.getFormat("@"));

            String[] headers = {"姓名", "身份证号", "手机号", "套餐名称（下拉选择）"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            Row demo = sheet.createRow(1);
            demo.createCell(0).setCellValue("张三");
            Cell demoIdCell = demo.createCell(1);
            demoIdCell.setCellStyle(textStyle);
            demoIdCell.setCellValue("110101199001011234");
            demo.createCell(2).setCellValue("13800138000");
            demo.createCell(3).setCellValue(pkgNames.length > 0 ? pkgNames[0] : "请选择套餐");

            sheet.setDefaultColumnStyle(1, textStyle);

            if (pkgNames.length > 0) {
                XSSFDataValidationHelper dvHelper = new XSSFDataValidationHelper(sheet);
                XSSFDataValidationConstraint dvConstraint =
                        (XSSFDataValidationConstraint) dvHelper.createExplicitListConstraint(pkgNames);
                CellRangeAddressList addressList = new CellRangeAddressList(2, 1001, 3, 3);
                XSSFDataValidation dv = (XSSFDataValidation) dvHelper.createValidation(dvConstraint, addressList);
                dv.setShowErrorBox(true);
                sheet.addValidationData(dv);
            }

            sheet.createFreezePane(0, 1);
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition",
                    "attachment; filename*=UTF-8''" +
                            URLEncoder.encode("体检人员导入模板.xlsx", "UTF-8").replace("+", "%20"));
            wb.write(response.getOutputStream());
            response.getOutputStream().flush();
        } catch (IOException e) {
            throw new RuntimeException("模板生成失败: " + e.getMessage());
        }
    }

    @Override
    public ImportPreviewDTO preview(MultipartFile file, Long doctorId) {
        if (file == null || file.isEmpty()) throw new RuntimeException("请上传文件");
        if (doctorId == null)               throw new RuntimeException("请选择目标医生");

        Map<String, Long> pkgMap = checkPackageMapper
                .selectList(new LambdaQueryWrapper<CheckPackage>().eq(CheckPackage::getIsActive, 1))
                .stream().collect(Collectors.toMap(
                        CheckPackage::getPackageName, CheckPackage::getPackageId, (a, b) -> a));

        List<Examination> valid = new ArrayList<>();
        List<FailDetail>  fails = new ArrayList<>();

        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            int lastRow = sheet.getLastRowNum();
            for (int i = 2; i <= lastRow; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String realName = getCellStr(row, 0);
                String idNumber = getCellStr(row, 1);
                if (realName.isEmpty() && idNumber.isEmpty()) continue;

                int    displayRow = i + 1;
                String phone      = getCellStr(row, 2);
                String pkgName    = getCellStr(row, 3);

                if (realName.isEmpty()) { fails.add(new FailDetail(displayRow, "姓名不能为空"));               continue; }
                if (!ID_PATTERN.matcher(idNumber).matches()) { fails.add(new FailDetail(displayRow, "身份证号格式不正确：" + idNumber)); continue; }
                if (pkgName.isEmpty())  { fails.add(new FailDetail(displayRow, "套餐名称不能为空"));           continue; }
                Long pkgId = pkgMap.get(pkgName);
                if (pkgId == null)      { fails.add(new FailDetail(displayRow, "套餐名称不存在：" + pkgName)); continue; }

                Examination exam = new Examination();
                exam.setRealName(realName);
                exam.setIdNumber(idNumber);
                exam.setPhone(phone.isEmpty() ? null : phone);
                exam.setPackageId(pkgId);
                exam.setStatus("PENDING");
                exam.setSource("GROUP");
                valid.add(exam);
            }
        } catch (Exception e) {
            throw new RuntimeException("文件解析失败: " + e.getMessage());
        }

        CapacityTodayVO today = capacityService.getTodayCapacity();
        int morningRem   = today.getMorning()   != null ? today.getMorning().getRemaining()   : 0;
        int afternoonRem = today.getAfternoon() != null ? today.getAfternoon().getRemaining() : 0;
        int allRemaining = morningRem + afternoonRem;

        CapacityTodayVO.DoctorSlotVO targetDoctor = findDoctorSlot(today, doctorId);
        int targetMorningRem   = targetDoctor != null && targetDoctor.getMorning()   != null ? targetDoctor.getMorning().getRemaining()   : 0;
        int targetAfternoonRem = targetDoctor != null && targetDoctor.getAfternoon() != null ? targetDoctor.getAfternoon().getRemaining() : 0;
        int targetTotalRem     = targetMorningRem + targetAfternoonRem;

        int total    = valid.size();
        int spillover = Math.max(0, total - targetTotalRem);
        int noEnough  = Math.max(0, total - allRemaining);

        int thirtyDayTotal = calcThirtyDayCapacity(LocalDate.now(), total);
        boolean cannotFitIn30Days = total > thirtyDayTotal;

        String token = UUID.randomUUID().toString();
        previewCache.put(token, valid);

        ImportPreviewDTO dto = new ImportPreviewDTO();
        dto.setTotal(total);
        dto.setTargetDoctorRemaining(targetTotalRem);
        dto.setTargetDoctorMorningRemaining(targetMorningRem);
        dto.setTargetDoctorAfternoonRemaining(targetAfternoonRem);
        dto.setAllDoctorsRemaining(allRemaining);
        dto.setAllDoctorsMorningRemaining(morningRem);
        dto.setAllDoctorsAfternoonRemaining(afternoonRem);
        dto.setHasSpillover(spillover > 0);
        dto.setSpilloverCount(spillover);
        dto.setNoEnoughToday(noEnough > 0);
        dto.setFutureCount(noEnough);
        dto.setFailCount(fails.size());
        dto.setFailDetails(fails);
        dto.setPreviewToken(token);

        dto.setThirtyDayTotalCapacity(thirtyDayTotal);
        dto.setCannotFitIn30Days(cannotFitIn30Days);

        return dto;
    }

    private int calcThirtyDayCapacity(LocalDate start, int need) {
        int total = 0;
        LocalDate limit = start.plusDays(29); // 含 start 共30天
        for (LocalDate d = start; !d.isAfter(limit); d = d.plusDays(1)) {
            CapacityTodayVO cap = capacityService.getCapacityByDate(d);
            if (cap != null && cap.getTotal() != null) {
                total += cap.getTotal().getRemaining();
            }
            if (total >= need) return total;
        }
        return total;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportResultDTO importByToken(String previewToken, ImportRequestDTO req) {
        List<Examination> valid = previewCache.remove(previewToken);
        if (valid == null) throw new RuntimeException("预览已过期，请重新上传文件");

        Long    primaryDoctorId = req.getDoctorId();
        boolean spillToOthers   = Boolean.TRUE.equals(req.getSpilloverToOtherDoctors());
        boolean spillToFuture   = Boolean.TRUE.equals(req.getSpilloverToFuture());

        CapacityTodayVO today     = capacityService.getTodayCapacity();
        int             todayRem  = today.getTotal() != null ? today.getTotal().getRemaining() : 0;

        if (todayRem == 0 && !spillToFuture) {
            throw new RuntimeException("今日所有时段名额已满，请选择分配到未来日期");
        }

        if (spillToFuture) {
            int thirtyDayCap = calcThirtyDayCapacity(LocalDate.now(), valid.size());
            if (valid.size() > thirtyDayCap) {
                throw new RuntimeException(
                        "30天内名额仍不足，无法完成导入。" +
                                "30天总剩余名额：" + thirtyDayCap + " 人，" +
                                "本次需导入：" + valid.size() + " 人，" +
                                "缺口：" + (valid.size() - thirtyDayCap) + " 人。" +
                                "请联系管理员调整名额配置后重试。");
            }
        }

        int total        = valid.size();
        int morningRem   = today.getMorning()   != null ? today.getMorning().getRemaining()   : 0;
        int afternoonRem = today.getAfternoon() != null ? today.getAfternoon().getRemaining() : 0;
        int todayTotal   = morningRem + afternoonRem;

        int mBatch, aBatch;
        if (todayTotal == 0) {
            mBatch = total / 2;
            aBatch = total - mBatch;
        } else {
            mBatch = total * morningRem / todayTotal;
            aBatch = total - mBatch;
        }

        List<Examination> morningQueue   = new ArrayList<>(valid.subList(0, mBatch));
        List<Examination> afternoonQueue = new ArrayList<>(valid.subList(mBatch, total));
        List<Assignment>  assignments    = new ArrayList<>();
        LocalDate         today0         = LocalDate.now();

        if (!morningQueue.isEmpty()) {
            DoctorSlot target = buildDoctorSlot(primaryDoctorId, today);
            if (target != null && target.morningRemaining > 0)
                drain(morningQueue, target, "MORNING", today0, assignments);
            if (!morningQueue.isEmpty() && target != null && target.afternoonRemaining > 0)
                drain(morningQueue, target, "AFTERNOON", today0, assignments);

            if (!morningQueue.isEmpty() && spillToOthers) {
                for (DoctorSlot s : buildOtherDoctorSlots(primaryDoctorId, today)) {
                    if (morningQueue.isEmpty()) break;
                    if (s.morningRemaining > 0)   drain(morningQueue, s, "MORNING",   today0, assignments);
                }
            }
            if (!morningQueue.isEmpty() && spillToOthers) {
                for (DoctorSlot s : buildOtherDoctorSlots(primaryDoctorId, today)) {
                    if (morningQueue.isEmpty()) break;
                    if (s.afternoonRemaining > 0) drain(morningQueue, s, "AFTERNOON", today0, assignments);
                }
            }
            if (!morningQueue.isEmpty() && spillToFuture)
                drainToFuture(morningQueue, today0.plusDays(1), primaryDoctorId, spillToOthers, assignments);
        }

        if (!afternoonQueue.isEmpty()) {
            DoctorSlot target = buildDoctorSlot(primaryDoctorId, today);
            if (target != null && target.afternoonRemaining > 0)
                drain(afternoonQueue, target, "AFTERNOON", today0, assignments);

            if (!afternoonQueue.isEmpty() && spillToOthers) {
                for (DoctorSlot s : buildOtherDoctorSlots(primaryDoctorId, today)) {
                    if (afternoonQueue.isEmpty()) break;
                    if (s.afternoonRemaining > 0) drain(afternoonQueue, s, "AFTERNOON", today0, assignments);
                }
            }

            if (!afternoonQueue.isEmpty()) {
                LocalDate       tomorrow   = today0.plusDays(1);
                CapacityTodayVO tomorrowCap = buildDayCapacityVO(tomorrow);

                DoctorSlot tTarget = buildDoctorSlot(primaryDoctorId, tomorrowCap);
                if (tTarget != null && tTarget.morningRemaining > 0)
                    drain(afternoonQueue, tTarget, "MORNING",   tomorrow, assignments);
                if (!afternoonQueue.isEmpty() && tTarget != null && tTarget.afternoonRemaining > 0)
                    drain(afternoonQueue, tTarget, "AFTERNOON", tomorrow, assignments);

                if (!afternoonQueue.isEmpty() && spillToOthers) {
                    for (DoctorSlot s : buildOtherDoctorSlots(primaryDoctorId, tomorrowCap)) {
                        if (afternoonQueue.isEmpty()) break;
                        if (s.morningRemaining > 0)   drain(afternoonQueue, s, "MORNING",   tomorrow, assignments);
                    }
                }
                if (!afternoonQueue.isEmpty() && spillToOthers) {
                    for (DoctorSlot s : buildOtherDoctorSlots(primaryDoctorId, tomorrowCap)) {
                        if (afternoonQueue.isEmpty()) break;
                        if (s.afternoonRemaining > 0) drain(afternoonQueue, s, "AFTERNOON", tomorrow, assignments);
                    }
                }
            }

            if (!afternoonQueue.isEmpty() && spillToFuture)
                drainToFuture(afternoonQueue, today0.plusDays(2), primaryDoctorId, spillToOthers, assignments);
        }

        int leftover = morningQueue.size() + afternoonQueue.size();
        if (leftover > 0) {
            throw new RuntimeException(
                    "30天内名额仍不足，无法完成导入（还剩 " + leftover + " 人无法分配）。" +
                            "本次操作已全部回滚，数据库无任何变更。");
        }

        for (Assignment a : assignments) {
            a.applyToExamination();
            examinationMapper.insert(a.exam);
        }

        ImportResultDTO result = new ImportResultDTO();
        result.setTotal(valid.size());
        result.setSuccessCount(assignments.size());
        result.setFailCount(0);
        result.setFailDetails(Collections.emptyList());
        result.setMorningCount((int)   assignments.stream().filter(a -> "MORNING".equals(a.period)).count());
        result.setAfternoonCount((int) assignments.stream().filter(a -> "AFTERNOON".equals(a.period)).count());
        return result;
    }

    private void drainToFuture(List<Examination> queue, LocalDate startDate,
                               Long primaryDoctorId, boolean spillToOthers,
                               List<Assignment> assignments) {
        LocalDate cursor = startDate;
        LocalDate limit  = LocalDate.now().plusDays(29);
        while (!queue.isEmpty() && !cursor.isAfter(limit)) {
            CapacityTodayVO dayCap = buildDayCapacityVO(cursor);
            DoctorSlot      target = buildDoctorSlot(primaryDoctorId, dayCap);
            if (target != null) {
                if (target.morningRemaining > 0)
                    drain(queue, target, "MORNING",   cursor, assignments);
                if (!queue.isEmpty() && target.afternoonRemaining > 0)
                    drain(queue, target, "AFTERNOON", cursor, assignments);
            }
            if (!queue.isEmpty() && spillToOthers) {
                for (DoctorSlot s : buildOtherDoctorSlots(primaryDoctorId, dayCap)) {
                    if (queue.isEmpty()) break;
                    if (s.morningRemaining > 0)
                        drain(queue, s, "MORNING",   cursor, assignments);
                    if (!queue.isEmpty() && s.afternoonRemaining > 0)
                        drain(queue, s, "AFTERNOON", cursor, assignments);
                }
            }
            cursor = cursor.plusDays(1);
        }
    }

    private void drain(List<Examination> queue, DoctorSlot slot, String period,
                       LocalDate date, List<Assignment> assignments) {
        int cap  = "MORNING".equals(period) ? slot.morningRemaining : slot.afternoonRemaining;
        int take = Math.min(cap, queue.size());
        for (int i = 0; i < take; i++)
            assignments.add(new Assignment(queue.remove(0), slot.doctorId, date, period));
        if ("MORNING".equals(period)) slot.morningRemaining   -= take;
        else                          slot.afternoonRemaining -= take;
    }

    private CapacityTodayVO buildDayCapacityVO(LocalDate date) {
        return capacityService.getCapacityByDate(date);
    }

    private DoctorSlot buildDoctorSlot(Long doctorId, CapacityTodayVO cap) {
        if (cap == null || cap.getDoctorList() == null) return null;
        for (CapacityTodayVO.DoctorSlotVO d : cap.getDoctorList()) {
            if (d.getDoctorId().equals(doctorId)) {
                return new DoctorSlot(d.getDoctorId(),
                        safeRemaining(d.getMorning()), safeRemaining(d.getAfternoon()));
            }
        }
        return null;
    }

    private List<DoctorSlot> buildOtherDoctorSlots(Long excludeId, CapacityTodayVO cap) {
        if (cap == null || cap.getDoctorList() == null) return Collections.emptyList();
        List<DoctorSlot> list = new ArrayList<>();
        for (CapacityTodayVO.DoctorSlotVO d : cap.getDoctorList()) {
            if (d.getDoctorId().equals(excludeId)) continue;
            int m = safeRemaining(d.getMorning()), a = safeRemaining(d.getAfternoon());
            if (m > 0 || a > 0) list.add(new DoctorSlot(d.getDoctorId(), m, a));
        }
        list.sort((x, y) -> Integer.compare(
                y.morningRemaining + y.afternoonRemaining,
                x.morningRemaining + x.afternoonRemaining));
        return list;
    }

    private int safeRemaining(CapacityTodayVO.SlotVO slot) {
        return slot == null ? 0 : Math.max(0, slot.getRemaining());
    }

    private String getCellStr(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:  { String v = cell.getStringCellValue(); return v == null ? "" : v.trim(); }
            case NUMERIC: return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            case FORMULA: {
                try { return cell.getStringCellValue().trim(); }
                catch (Exception e) { return String.valueOf((long) cell.getNumericCellValue()); }
            }
            default: return "";
        }
    }

    private CapacityTodayVO.DoctorSlotVO findDoctorSlot(CapacityTodayVO today, Long doctorId) {
        if (today.getDoctorList() == null) return null;
        return today.getDoctorList().stream()
                .filter(d -> d.getDoctorId().equals(doctorId))
                .findFirst().orElse(null);
    }

    private static class DoctorSlot {
        Long doctorId;
        int  morningRemaining;
        int  afternoonRemaining;

        DoctorSlot(Long doctorId, int morningRemaining, int afternoonRemaining) {
            this.doctorId           = doctorId;
            this.morningRemaining   = morningRemaining;
            this.afternoonRemaining = afternoonRemaining;
        }
    }

    private static class Assignment {
        Examination exam;
        Long        doctorId;
        LocalDate   date;
        String      period;

        Assignment(Examination exam, Long doctorId, LocalDate date, String period) {
            this.exam     = exam;
            this.doctorId = doctorId;
            this.date     = date;
            this.period   = period;
        }

        void applyToExamination() {
            exam.setRegisterDoctorId(doctorId);
            exam.setRegisterTime("MORNING".equals(period)
                    ? date.atTime(9,  0)
                    : date.atTime(14, 0));
        }
    }
}