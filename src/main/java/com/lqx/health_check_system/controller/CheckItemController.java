package com.lqx.health_check_system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lqx.health_check_system.common.Result;
import com.lqx.health_check_system.entity.CheckItem;
import com.lqx.health_check_system.service.CheckItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/items")
@RequiredArgsConstructor
public class CheckItemController {

    private final CheckItemService checkItemService;

    @GetMapping
    public Result<Page<CheckItem>> listItems(
            @RequestParam(defaultValue = "1")  int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false)    String itemName,
            @RequestParam(required = false)    String itemCode,
            @RequestParam(required = false)    String category,
            @RequestParam(required = false)    Integer isActive) {
        return Result.success(checkItemService.listItems(page, size, itemName, itemCode, category, isActive));
    }

    @PostMapping
    public Result<Void> addItem(@RequestBody CheckItem item) {
        checkItemService.addItem(item);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> updateItem(@PathVariable Long id, @RequestBody CheckItem item) {
        item.setItemId(id);
        checkItemService.updateItem(item);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    public Result<Void> toggleStatus(@PathVariable Long id, @RequestParam Integer isActive) {
        checkItemService.toggleStatus(id, isActive);
        return Result.success();
    }
}