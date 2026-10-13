package com.lqx.health_check_system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lqx.health_check_system.entity.CheckItem;

public interface CheckItemService {

    Page<CheckItem> listItems(int page, int size,
                              String itemName, String itemCode,
                              String category, Integer isActive);

    void addItem(CheckItem item);

    void updateItem(CheckItem item);

    void toggleStatus(Long itemId, Integer isActive);
}