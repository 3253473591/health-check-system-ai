package com.lqx.health_check_system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lqx.health_check_system.common.Result;
import com.lqx.health_check_system.entity.CheckItem;
import com.lqx.health_check_system.entity.CheckPackage;
import com.lqx.health_check_system.service.CheckPackageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/packages")
@RequiredArgsConstructor
public class CheckPackageController {

    private final CheckPackageService checkPackageService;

    @GetMapping
    public Result<Page<CheckPackage>> listPackages(
            @RequestParam(defaultValue = "1")  int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false)    String packageName,
            @RequestParam(required = false)    Integer isActive) {
        return Result.success(checkPackageService.listPackages(page, size, packageName, isActive));
    }

    @PostMapping
    public Result<Void> addPackage(@RequestBody CheckPackage pkg) {
        checkPackageService.addPackage(pkg);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> updatePackage(@PathVariable Long id, @RequestBody CheckPackage pkg) {
        pkg.setPackageId(id);
        checkPackageService.updatePackage(pkg);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    public Result<Void> toggleStatus(@PathVariable Long id, @RequestParam Integer isActive) {
        checkPackageService.toggleStatus(id, isActive);
        return Result.success();
    }

    @GetMapping("/{id}/items")
    public Result<List<CheckItem>> listPackageItems(@PathVariable Long id) {
        return Result.success(checkPackageService.listPackageItems(id));
    }

    @PostMapping("/{id}/items")
    public Result<Void> savePackageItems(@PathVariable Long id,
                                         @RequestBody Map<String, List<Long>> body) {
        checkPackageService.savePackageItems(id, body.get("itemIds"));
        return Result.success();
    }
}