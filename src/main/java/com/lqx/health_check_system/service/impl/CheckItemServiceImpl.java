package com.lqx.health_check_system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lqx.health_check_system.entity.CheckItem;
import com.lqx.health_check_system.mapper.CheckItemMapper;
import com.lqx.health_check_system.service.CheckItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CheckItemServiceImpl implements CheckItemService {

    private final CheckItemMapper checkItemMapper;

    @Override
    public Page<CheckItem> listItems(int page, int size,
                                     String itemName, String itemCode,
                                     String category, Integer isActive) {
        Page<CheckItem> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<CheckItem> wrapper = new LambdaQueryWrapper<CheckItem>()
                .like(StringUtils.hasText(itemName), CheckItem::getItemName, itemName)
                .like(StringUtils.hasText(itemCode), CheckItem::getItemCode, itemCode)
                .eq(StringUtils.hasText(category),   CheckItem::getCategory, category)
                .eq(isActive != null,                CheckItem::getIsActive, isActive)
                .orderByAsc(CheckItem::getItemId);
        return checkItemMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public void addItem(CheckItem item) {
        Long count = checkItemMapper.selectCount(
                new LambdaQueryWrapper<CheckItem>().eq(CheckItem::getItemCode, item.getItemCode()));
        if (count > 0) throw new RuntimeException("项目编码已存在");
        item.setIsActive(1);
        checkItemMapper.insert(item);
    }

    @Override
    public void updateItem(CheckItem item) {
        CheckItem existing = checkItemMapper.selectById(item.getItemId());
        if (existing == null) throw new RuntimeException("项目不存在");
        checkItemMapper.updateById(item);
    }

    @Override
    public void toggleStatus(Long itemId, Integer isActive) {
        CheckItem item = new CheckItem();
        item.setItemId(itemId);
        item.setIsActive(isActive);
        checkItemMapper.updateById(item);
    }
}