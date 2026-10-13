package com.lqx.health_check_system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lqx.health_check_system.entity.CheckItem;
import com.lqx.health_check_system.entity.CheckPackage;
import java.util.List;

public interface CheckPackageService {

    Page<CheckPackage> listPackages(int page, int size,
                                    String packageName, Integer isActive);

    void addPackage(CheckPackage pkg);

    void updatePackage(CheckPackage pkg);

    void toggleStatus(Long packageId, Integer isActive);

    List<CheckItem> listPackageItems(Long packageId);

    void savePackageItems(Long packageId, List<Long> itemIds);
}