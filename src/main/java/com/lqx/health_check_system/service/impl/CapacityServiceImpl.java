package com.lqx.health_check_system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lqx.health_check_system.dto.*;
import com.lqx.health_check_system.entity.*;
import com.lqx.health_check_system.mapper.*;
import com.lqx.health_check_system.service.CapacityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CapacityServiceImpl implements CapacityService {

    private static final int DEFAULT_TOTAL = 100;
    private static final LocalTime MORNING_START = LocalTime.of(9, 0, 0);
    private static final LocalTime MORNING_END = LocalTime.of(11, 59, 59);
    private static final LocalTime AFTERNOON_START = LocalTime.of(14, 0, 0);
    private static final LocalTime AFTERNOON_END = LocalTime.of(17, 59, 59);

    private final DailyCapacityConfigMapper dailyMapper;
    private final DoctorCapacityConfigMapper doctorMapper;
    private final SysUserMapper userMapper;
    private final ExaminationMapper examinationMapper;

    @Override
    public CapacityTodayVO getTodayCapacity() {
        return getCapacityByDate(LocalDate.now());
    }

    @Override
    public CapacityTodayVO getCapacityByDate(LocalDate date) {
        CapacityConfigVO config = buildConfig(date);

        List<Examination> exams = examinationMapper.selectList(
                new LambdaQueryWrapper<Examination>()
                        .ge(Examination::getRegisterTime, date.atStartOfDay())
                        .lt(Examination::getRegisterTime, date.plusDays(1).atStartOfDay()));

        int totalUsed = exams.size();
        int morningUsed = countByTimeRange(exams, MORNING_START, MORNING_END);
        int afternoonUsed = countByTimeRange(exams, AFTERNOON_START, AFTERNOON_END);

        Map<Long, Long> morningByDoc = countByDocAndTime(exams, MORNING_START, MORNING_END);
        Map<Long, Long> afternoonByDoc = countByDocAndTime(exams, AFTERNOON_START, AFTERNOON_END);

        CapacityTodayVO vo = new CapacityTodayVO();
        vo.setDate(date.format(DateTimeFormatter.ISO_LOCAL_DATE));

        CapacityTodayVO.SlotVO total = new CapacityTodayVO.SlotVO();
        total.setCapacity(config.getTotalCapacity());
        total.setUsed(totalUsed);
        total.setRemaining(Math.max(0, config.getTotalCapacity() - totalUsed));
        vo.setTotal(total);

        CapacityTodayVO.TimeSlotVO morning = new CapacityTodayVO.TimeSlotVO();
        morning.setTimeRange("09:00-12:00");
        morning.setCapacity(config.getMorningCapacity());
        morning.setUsed(morningUsed);
        morning.setRemaining(Math.max(0, config.getMorningCapacity() - morningUsed));
        vo.setMorning(morning);

        CapacityTodayVO.TimeSlotVO afternoon = new CapacityTodayVO.TimeSlotVO();
        afternoon.setTimeRange("14:00-18:00");
        afternoon.setCapacity(config.getAfternoonCapacity());
        afternoon.setUsed(afternoonUsed);
        afternoon.setRemaining(Math.max(0, config.getAfternoonCapacity() - afternoonUsed));
        vo.setAfternoon(afternoon);

        List<CapacityTodayVO.DoctorSlotVO> doctorList = new ArrayList<>();
        for (CapacityConfigVO.DoctorConfigItem dc : config.getDoctorCapacities()) {
            CapacityTodayVO.DoctorSlotVO d = new CapacityTodayVO.DoctorSlotVO();
            d.setDoctorId(dc.getDoctorId());
            d.setDoctorName(dc.getDoctorName());

            int mUsed = morningByDoc.getOrDefault(dc.getDoctorId(), 0L).intValue();
            int aUsed = afternoonByDoc.getOrDefault(dc.getDoctorId(), 0L).intValue();

            CapacityTodayVO.SlotVO ms = new CapacityTodayVO.SlotVO();
            ms.setCapacity(dc.getMorning());
            ms.setUsed(mUsed);
            ms.setRemaining(Math.max(0, dc.getMorning() - mUsed));
            d.setMorning(ms);

            CapacityTodayVO.SlotVO as = new CapacityTodayVO.SlotVO();
            as.setCapacity(dc.getAfternoon());
            as.setUsed(aUsed);
            as.setRemaining(Math.max(0, dc.getAfternoon() - aUsed));
            d.setAfternoon(as);

            doctorList.add(d);
        }
        vo.setDoctorList(doctorList);
        return vo;
    }

    @Override
    public CapacityConfigVO getTodayConfig() {
        return getConfigByDate(LocalDate.now());
    }

    @Override
    public CapacityConfigVO getConfigByDate(LocalDate date) {
        return buildConfig(date);
    }

    @Override
    @Transactional
    public void saveConfig(CapacityConfigRequest req) {
        LocalDate date = LocalDate.parse(req.getCapacityDate());
        int total = req.getTotalCapacity();
        int morning = total % 2 == 0 ? total / 2 : (total - 1) / 2;
        int afternoon = total - morning;

        int sumMorning = 0;
        int sumAfternoon = 0;
        if (req.getDoctorCapacities() != null) {
            for (CapacityConfigRequest.DoctorCapacity dc : req.getDoctorCapacities()) {
                sumMorning += (dc.getMorning() != null) ? dc.getMorning() : 0;
                sumAfternoon += (dc.getAfternoon() != null) ? dc.getAfternoon() : 0;
            }
        }

        if (sumMorning != morning) {
            throw new RuntimeException("上午医生名额之和(" + sumMorning + ")须等于上午总额(" + morning + ")");
        }
        if (sumAfternoon != afternoon) {
            throw new RuntimeException("下午医生名额之和(" + sumAfternoon + ")须等于下午总额(" + afternoon + ")");
        }

        DailyCapacityConfig daily = dailyMapper.selectOne(
                new LambdaQueryWrapper<DailyCapacityConfig>()
                        .eq(DailyCapacityConfig::getCapacityDate, date));

        if (daily == null) {
            daily = new DailyCapacityConfig();
            daily.setCapacityDate(date);
        }
        daily.setTotalCapacity(total);
        daily.setMorningCapacity(morning);
        daily.setAfternoonCapacity(afternoon);

        if (daily.getConfigId() == null) {
            dailyMapper.insert(daily);
        } else {
            dailyMapper.updateById(daily);
        }

        doctorMapper.delete(
                new LambdaQueryWrapper<DoctorCapacityConfig>()
                        .eq(DoctorCapacityConfig::getCapacityDate, date));

        if (req.getDoctorCapacities() != null) {
            for (CapacityConfigRequest.DoctorCapacity dc : req.getDoctorCapacities()) {
                DoctorCapacityConfig dcc = new DoctorCapacityConfig();
                dcc.setCapacityDate(date);
                dcc.setDoctorId(dc.getDoctorId());
                dcc.setMorningCapacity(dc.getMorning());
                dcc.setAfternoonCapacity(dc.getAfternoon());
                doctorMapper.insert(dcc);
            }
        }
    }

    private CapacityConfigVO buildConfig(LocalDate date) {
        List<SysUser> doctors = userMapper.selectList(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getRole, "DOCTOR")
                        .eq(SysUser::getIsActive, 1)
                        .orderByAsc(SysUser::getUserId));

        DailyCapacityConfig daily = dailyMapper.selectOne(
                new LambdaQueryWrapper<DailyCapacityConfig>()
                        .eq(DailyCapacityConfig::getCapacityDate, date));

        int total = (daily != null) ? daily.getTotalCapacity() : DEFAULT_TOTAL;
        int morning = (daily != null) ? daily.getMorningCapacity() : (total % 2 == 0 ? total / 2 : (total - 1) / 2);
        int afternoon = (daily != null) ? daily.getAfternoonCapacity() : (total - morning);

        List<DoctorCapacityConfig> dcList = doctorMapper.selectList(
                new LambdaQueryWrapper<DoctorCapacityConfig>()
                        .eq(DoctorCapacityConfig::getCapacityDate, date));

        Map<Long, DoctorCapacityConfig> dcMap = dcList.stream()
                .collect(Collectors.toMap(DoctorCapacityConfig::getDoctorId, d -> d));

        int n = doctors.size();
        List<CapacityConfigVO.DoctorConfigItem> items = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            SysUser doc = doctors.get(i);
            CapacityConfigVO.DoctorConfigItem item = new CapacityConfigVO.DoctorConfigItem();
            item.setDoctorId(doc.getUserId());
            item.setDoctorName(doc.getRealName());

            if (dcMap.containsKey(doc.getUserId())) {
                DoctorCapacityConfig cfg = dcMap.get(doc.getUserId());
                item.setMorning(cfg.getMorningCapacity());
                item.setAfternoon(cfg.getAfternoonCapacity());
            } else {
                int mBase = n > 0 ? morning / n : 0;
                int mRem = n > 0 ? morning % n : 0;
                int aBase = n > 0 ? afternoon / n : 0;
                int aRem = n > 0 ? afternoon % n : 0;
                item.setMorning(mBase + (i < mRem ? 1 : 0));
                item.setAfternoon(aBase + (i < aRem ? 1 : 0));
            }
            items.add(item);
        }

        CapacityConfigVO vo = new CapacityConfigVO();
        vo.setCapacityDate(date.format(DateTimeFormatter.ISO_LOCAL_DATE));
        vo.setTotalCapacity(total);
        vo.setMorningCapacity(morning);
        vo.setAfternoonCapacity(afternoon);
        vo.setDoctorCapacities(items);
        return vo;
    }

    private int countByTimeRange(List<Examination> exams, LocalTime start, LocalTime end) {
        int count = 0;
        for (Examination e : exams) {
            if (e.getRegisterTime() == null) continue;
            LocalTime t = e.getRegisterTime().toLocalTime();
            if (!t.isBefore(start) && !t.isAfter(end)) {
                count++;
            }
        }
        return count;
    }

    private Map<Long, Long> countByDocAndTime(List<Examination> exams, LocalTime start, LocalTime end) {
        Map<Long, Long> map = new HashMap<>();
        for (Examination e : exams) {
            if (e.getRegisterDoctorId() == null || e.getRegisterTime() == null) continue;
            LocalTime t = e.getRegisterTime().toLocalTime();
            if (!t.isBefore(start) && !t.isAfter(end)) {
                Long docId = e.getRegisterDoctorId();
                map.put(docId, map.getOrDefault(docId, 0L) + 1);
            }
        }
        return map;
    }
}