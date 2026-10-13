package com.lqx.health_check_system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lqx.health_check_system.entity.PhysicalData;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PhysicalDataMapper extends BaseMapper<PhysicalData> {

    @Insert("INSERT INTO physical_data (exam_id, item_id, item_value, unit, record_time) " +
            "VALUES (#{examId}, #{itemId}, #{itemValue}, #{unit}, #{recordTime}) " +
            "ON DUPLICATE KEY UPDATE item_value = #{itemValue}, unit = #{unit}, record_time = #{recordTime}")
    int upsert(PhysicalData data);
}