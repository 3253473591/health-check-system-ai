package com.lqx.health_check_system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lqx.health_check_system.entity.PackageItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface PackageItemMapper extends BaseMapper<PackageItem> {

    List<Long> selectItemIdsByPackageId(@Param("packageId") Long packageId);
}