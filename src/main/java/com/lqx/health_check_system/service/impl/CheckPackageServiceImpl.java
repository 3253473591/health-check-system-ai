package com.lqx.health_check_system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lqx.health_check_system.entity.CheckItem;
import com.lqx.health_check_system.entity.CheckPackage;
import com.lqx.health_check_system.entity.PackageItem;
import com.lqx.health_check_system.mapper.CheckItemMapper;
import com.lqx.health_check_system.mapper.CheckPackageMapper;
import com.lqx.health_check_system.mapper.PackageItemMapper;
import com.lqx.health_check_system.service.CheckPackageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CheckPackageServiceImpl implements CheckPackageService {

    private final CheckPackageMapper checkPackageMapper;
    private final CheckItemMapper    checkItemMapper;
    private final PackageItemMapper  packageItemMapper;

    @Override
    public Page<CheckPackage> listPackages(int page, int size,
                                           String packageName, Integer isActive) {
        Page<CheckPackage> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<CheckPackage> wrapper = new LambdaQueryWrapper<CheckPackage>()
                .like(StringUtils.hasText(packageName), CheckPackage::getPackageName, packageName)
                .eq(isActive != null,                   CheckPackage::getIsActive,    isActive)
                .orderByAsc(CheckPackage::getPackageId);
        return checkPackageMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public void addPackage(CheckPackage pkg) {
        pkg.setIsActive(1);
        checkPackageMapper.insert(pkg);
    }

    @Override
    public void updatePackage(CheckPackage pkg) {
        CheckPackage existing = checkPackageMapper.selectById(pkg.getPackageId());
        if (existing == null) throw new RuntimeException("套餐不存在");
        checkPackageMapper.updateById(pkg);
    }

    @Override
    public void toggleStatus(Long packageId, Integer isActive) {
        CheckPackage pkg = new CheckPackage();
        pkg.setPackageId(packageId);
        pkg.setIsActive(isActive);
        checkPackageMapper.updateById(pkg);
    }

    @Override
    public List<CheckItem> listPackageItems(Long packageId) {
        List<Long> itemIds = packageItemMapper.selectItemIdsByPackageId(packageId);
        if (itemIds == null || itemIds.isEmpty()) return Collections.emptyList();
        return checkItemMapper.selectBatchIds(itemIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void savePackageItems(Long packageId, List<Long> itemIds) {
        packageItemMapper.delete(new LambdaQueryWrapper<PackageItem>()
                .eq(PackageItem::getPackageId, packageId));
        if (itemIds != null && !itemIds.isEmpty()) {
            for (Long itemId : itemIds) {
                PackageItem pi = new PackageItem();
                pi.setPackageId(packageId);
                pi.setItemId(itemId);
                packageItemMapper.insert(pi);
            }
        }
    }
}