package com.lqx.health_check_system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lqx.health_check_system.common.Result;
import com.lqx.health_check_system.entity.CheckPackage;
import com.lqx.health_check_system.mapper.CheckPackageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/common")
@RequiredArgsConstructor
public class CommonController {

    private final CheckPackageMapper checkPackageMapper;

    @GetMapping("/packages")
    public Result<List<CheckPackage>> activePackages() {
        List<CheckPackage> list = checkPackageMapper.selectList(
                new LambdaQueryWrapper<CheckPackage>()
                        .eq(CheckPackage::getIsActive, 1)
                        .orderByAsc(CheckPackage::getPackageId));
        return Result.success(list);
    }
}