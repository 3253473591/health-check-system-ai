package com.lqx.health_check_system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lqx.health_check_system.entity.CheckItem;
import com.lqx.health_check_system.entity.Examination;
import com.lqx.health_check_system.entity.PhysicalData;
import com.lqx.health_check_system.mapper.CheckItemMapper;
import com.lqx.health_check_system.mapper.ExaminationMapper;
import com.lqx.health_check_system.mapper.PackageItemMapper;
import com.lqx.health_check_system.mapper.PhysicalDataMapper;
import com.lqx.health_check_system.service.PhysicalDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PhysicalDataServiceImpl implements PhysicalDataService {

    private final ExaminationMapper examinationMapper;
    private final PackageItemMapper packageItemMapper;
    private final CheckItemMapper checkItemMapper;
    private final PhysicalDataMapper physicalDataMapper;

    @Override
    public List<CheckItem> getPackageItems(Long packageId) {
        List<Long> itemIds = packageItemMapper.selectItemIdsByPackageId(packageId);
        if (itemIds == null || itemIds.isEmpty()) {
            return Collections.emptyList();
        }
        return checkItemMapper.selectBatchIds(itemIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveData(Long examId, Long doctorId, List<PhysicalData> dataList) {
        if (examId == null) {
            throw new RuntimeException("体检单号不能为空");
        }
        if (doctorId == null) {
            throw new RuntimeException("医生信息无效，请重新登录");
        }
        if (dataList == null || dataList.isEmpty()) {
            throw new RuntimeException("没有需要保存的数据");
        }

        Examination exam = examinationMapper.selectById(examId);
        if (exam == null) {
            throw new RuntimeException("体检记录不存在");
        }

        String status = exam.getStatus();
        if ("LOCKED".equals(status) || "PUBLISHED".equals(status)) {
            throw new RuntimeException("该体检记录已锁定，不可再修改");
        }

        if ("IN_PROGRESS".equals(status)
                && exam.getRegisterDoctorId() != null
                && !exam.getRegisterDoctorId().equals(doctorId)) {
            throw new RuntimeException("该记录已由其他医生认领，不可重复录入");
        }

        LocalDateTime now = LocalDateTime.now();

        for (PhysicalData pd : dataList) {
            if (pd == null) {
                continue;
            }
            if (pd.getItemId() == null) {
                throw new RuntimeException("存在未指定项目ID的数据");
            }
            if (pd.getItemValue() == null || pd.getItemValue().trim().isEmpty()) {
                throw new RuntimeException("检查结果不能为空");
            }

            pd.setExamId(examId);
            pd.setItemValue(pd.getItemValue().trim());

            if (pd.getUnit() != null) {
                String unit = pd.getUnit().trim();
                pd.setUnit(unit.isEmpty() ? null : unit);
            }

            pd.setRecordTime(now);
            physicalDataMapper.upsert(pd);
        }

        if ("PENDING".equals(status)) {
            Examination upd = new Examination();
            upd.setExamId(examId);
            upd.setStatus("IN_PROGRESS");
            upd.setRegisterDoctorId(doctorId);
            examinationMapper.updateById(upd);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> lockExam(Long examId) {
        Examination exam = examinationMapper.selectById(examId);
        if (exam == null) {
            throw new RuntimeException("体检记录不存在");
        }
        String status = exam.getStatus();
        if ("LOCKED".equals(status) || "PUBLISHED".equals(status)) {
            throw new RuntimeException("该体检记录已锁定或已发布，无需重复操作");
        }

        Long packageId = exam.getPackageId();
        List<CheckItem> packageItems = this.getPackageItems(packageId);
        int expectedCount = packageItems.size();

        long savedCount = physicalDataMapper.selectCount(
                new LambdaQueryWrapper<PhysicalData>()
                        .eq(PhysicalData::getExamId, examId)
        );

        if (savedCount == 0) {
            throw new RuntimeException("数据未录入，禁止锁定");
        }

        if (expectedCount > 0 && savedCount < expectedCount) {
            throw new RuntimeException("数据录入未完成，禁止锁定（已录入 " + savedCount + " 项，共需录入 " + expectedCount + " 项）");
        }

        Examination upd = new Examination();
        upd.setExamId(examId);
        upd.setStatus("LOCKED");
        upd.setLockedTime(LocalDateTime.now());
        examinationMapper.updateById(upd);

        Map<String, Object> res = new HashMap<>();
        res.put("examId", examId);
        res.put("status", "LOCKED");
        return res;
    }
}