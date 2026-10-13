package com.lqx.health_check_system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lqx.health_check_system.entity.CheckItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CheckItemMapper extends BaseMapper<CheckItem> {
}